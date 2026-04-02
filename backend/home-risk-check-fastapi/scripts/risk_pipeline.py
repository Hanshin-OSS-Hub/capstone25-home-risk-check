import pandas as pd
import numpy as np
import os
import sys
import joblib
from datetime import datetime
from sqlalchemy import text
from app.services.feature_service import calculate_risk_features, convert_to_model_input
from app.services.risk_calculator import _rule_based_score, _score_to_level, RULE_WEIGHT, ML_WEIGHT

from app.core.constants import BuildingCol, Table
# ---------------------------------------------------------
# 1. 프로젝트 설정 로드
# ---------------------------------------------------------
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.abspath(os.path.join(current_dir, '..'))
sys.path.append(project_root)

from app.core import get_engine
engine = get_engine()


# ---------------------------------------------------------
# 2. 헬퍼 함수
# ---------------------------------------------------------
def _create_join_key_robust(row, col_map):
    try:
        sgg = str(row[col_map['sgg']]).strip()
        bjd = str(row[col_map['bjd']]).strip()
        bon_val = row[col_map['bon']]
        bu_val = row[col_map['bu']]

        if pd.isna(bon_val) or pd.isna(bu_val): return None
        bon_str = str(int(float(bon_val))).zfill(4)
        bu_str = str(int(float(bu_val))).zfill(4)

        return f"{sgg}-{bjd}-{bon_str}-{bu_str}"
    except Exception:
        return None


def _generate_key_from_pnu(unique_no):
    try:
        if pd.isna(unique_no): return None
        s = str(unique_no).replace("-", "").strip()
        if len(s) < 19: return None
        return f"{s[0:5]}-{s[5:10]}-{s[11:15]}-{s[15:19]}"
    except Exception:
        return None


# ---------------------------------------------------------
# 3. 메인 파이프라인
# ---------------------------------------------------------
def run_risk_analysis_pipeline():
    print(f"[{datetime.now()}] --- 1. 데이터 로드 시작 ---")

    # 1. 건축물대장
    sql_build = f"""
        SELECT b.id as building_info_id, b.unique_number, b.main_use, 
               b.owner_name, b.exclusive_area AS AREA, 
               b.{BuildingCol.IS_VIOLATING}, b.detail_address, b.lot_address,
               t.use_apr_day, b.ownership_changed_date
        FROM building_info b
        LEFT JOIN building_title_info t 
            ON SUBSTR(b.unique_number, 1, 19) = t.unique_number
        WHERE b.unique_number IS NOT NULL
    """

    # 2. 전세 실거래가
    sql_rent = f"""
        SELECT district, legal_dong, main_jibun, sub_jibun, 
               deposit as rent_price, contract_date as contract_date, 
               exclusive_area as rent_area
        FROM {Table.RAW_RENT}
        WHERE monthly_rent = 0 AND contract_date >= '20230101'
    """

    # 3. 매매 실거래가
    sql_trade = f"""
        SELECT district, legal_dong, main_jibun, sub_jibun, 
               trade_price as trade_price, contract_date as trade_date
        FROM {Table.RAW_TRADE}
        WHERE contract_date >= '20230101'
    """

    # 4. 공시가격
    sql_pub = f"""
        SELECT building_info_id, price as public_price, base_date as price_date
        FROM {Table.PUBLIC_PRICE} ORDER BY base_date ASC
    """

    with engine.connect() as conn:
        df_build = pd.read_sql(sql_build, conn)
        df_rent = pd.read_sql(sql_rent, conn)
        df_trade = pd.read_sql(sql_trade, conn)
        df_pub = pd.read_sql(sql_pub, conn)

    print(f"-> 건축물대장 로드: {len(df_build)}건")

    # --- 전처리 ---
    for df in [df_rent, df_trade, df_build]:
        cols = ['rent_price', 'trade_price', 'rent_area', 'AREA']
        for c in cols:
            if c in df.columns: df[c] = pd.to_numeric(df[c], errors='coerce')

    df_pub['public_price'] = pd.to_numeric(df_pub['public_price'], errors='coerce') / 10000

    df_rent['contract_date'] = pd.to_datetime(df_rent['contract_date'], errors='coerce')
    df_trade['trade_date'] = pd.to_datetime(df_trade['trade_date'], errors='coerce')
    df_pub['price_date'] = pd.to_datetime(df_pub['price_date'], errors='coerce')
    df_build['use_apr_day'] = pd.to_datetime(df_build['use_apr_day'], errors='coerce')
    df_build['ownership_changed_date'] = pd.to_datetime(df_build['ownership_changed_date'], errors='coerce')

    col_map = {'sgg': 'district', 'bjd': 'legal_dong', 'bon': 'main_jibun', 'bu': 'sub_jibun'}
    df_rent['key'] = df_rent.apply(lambda row: _create_join_key_robust(row, col_map), axis=1)
    df_trade['key'] = df_trade.apply(lambda row: _create_join_key_robust(row, col_map), axis=1)
    df_build['key'] = df_build['unique_number'].apply(_generate_key_from_pnu)

    df_rent = df_rent.dropna(subset=['key', 'rent_price'])
    df_build = df_build.dropna(subset=['key'])
    df_rent = df_rent.sort_values('contract_date', ascending=False).drop_duplicates(subset=['key'], keep='first')

    # --- 병합 ---
    df_rent = df_rent.sort_values('contract_date')
    df_trade = df_trade.sort_values('trade_date')

    df_merged = pd.merge_asof(
        df_rent, df_trade[['key', 'trade_price', 'trade_date']],
        left_on='contract_date', right_on='trade_date',
        by='key', direction='backward', tolerance=pd.Timedelta(days=365 * 2)
    )

    df_merged = pd.merge(df_merged, df_build, on='key', how='left')
    df_merged = df_merged.dropna(subset=['building_info_id'])

    df_merged['area_diff'] = abs(df_merged['rent_area'] - df_merged['AREA'])
    initial_cnt = len(df_merged)
    df_merged = df_merged[df_merged['area_diff'] < 3.3].copy()
    print(f"-> 면적 불일치 제거: {initial_cnt - len(df_merged)}건")

    df_merged['building_info_id'] = df_merged['building_info_id'].astype(int)
    df_pub['building_info_id'] = df_pub['building_info_id'].astype(int)
    df_pub = df_pub.sort_values('price_date')

    df_merged = pd.merge_asof(
        df_merged.sort_values('contract_date'), df_pub,
        left_on='contract_date', right_on='price_date',
        by='building_info_id', direction='backward'
    )

    if df_merged.empty:
        print("!!! 분석 대상 없음 !!!")
        return

    df_target = df_merged.copy()

    # (1) 시세 추정
    def estimate_market_price(row):
        if pd.notna(row['trade_price']) and row['trade_price'] > 0:
            return row['trade_price']
        if pd.notna(row['public_price']) and row['public_price'] > 0:
            m_use = str(row['main_use'])
            if any(x in m_use for x in ['다세대', '오피스텔', '연립', '근린']):
                return row['public_price'] * 1.8
            return row['public_price'] * 1.5
        return np.nan

    df_target['est_market_price'] = df_target.apply(estimate_market_price, axis=1)
    df_target = df_target.dropna(subset=['est_market_price'])
    df_target = df_target[df_target['est_market_price'] > 0]

    # (2) 전세가율 계산 (순수 전세 / 시세)
    df_target['jeonse_ratio'] = df_target['rent_price'] / df_target['est_market_price']
    df_target = df_target[df_target['jeonse_ratio'] < 3.0].copy()

    # (3) HUG 위험도
    filled_public = df_target['public_price'].fillna(df_target['est_market_price'] * 0.7)
    df_target['hug_safe_limit'] = (filled_public * 1.26).astype(int)
    df_target['hug_risk_ratio'] = df_target.apply(
        lambda x: x['rent_price'] / x['hug_safe_limit'] if x['hug_safe_limit'] > 0 else 0, axis=1
    )

    # (4) 정성적 리스크 점수화 (Simulation Score) — 참고용 유지
    def type_weight(use):
        s = str(use)
        if '근린' in s: return 0.4
        if any(c in s for c in ['다세대', '오피스텔', '연립']): return 0.1
        return 0.0

    def trust_weight(owner):
        return 0.3 if owner and ('신탁' in str(owner)) else 0.0

    high_risk_regions = ['미추홀', '강서', '관악', '수원', '대전', '대구', '부평']

    def region_weight(row):
        addr = str(row.get('detail_address', '')) + str(row.get('lot_address', ''))
        return 0.2 if any(r in addr for r in high_risk_regions) else 0.0

    def calc_short_term_weight(row):
        try:
            if pd.isna(row['contract_date']) or pd.isna(row['ownership_changed_date']): return 0.0
            days = (row['contract_date'] - row['ownership_changed_date']).days
            if -90 < days < 90: return 0.3
            if 0 <= days < 730: return 0.1
            return 0.0
        except Exception:
            return 0.0

    w_type = df_target['main_use'].apply(type_weight)
    w_trust = df_target['owner_name'].apply(trust_weight)
    w_region = df_target.apply(region_weight, axis=1)
    w_short = df_target.apply(calc_short_term_weight, axis=1)

    df_target['risk_simulation_score'] = (w_type + w_trust + w_region + w_short).clip(0, 1.0)

    # (5) 깡통전세 위험도
    real_debt_amount = 0
    df_target['total_risk_ratio'] = (df_target['rent_price'] + real_debt_amount) / df_target['est_market_price']

    # DB 저장을 위해 loan_amount 컬럼은 0으로 채워둠
    df_target['est_loan_amount'] = 0

    # =================================================================
    # [변경] 하이브리드 등급 판정 (룰 베이스 60% + ML 40%)
    # =================================================================

    # --- Step 1: 룰 베이스 점수 계산 (각 행에 대해) ---
    def get_rule_score(row):
        """risk_calculator._rule_based_score()와 동일한 로직을 행 단위로 적용"""
        feats = {
            'jeonse_ratio': row['jeonse_ratio'],
            'total_risk_ratio': row['total_risk_ratio'],
            'hug_risk_ratio': row['hug_risk_ratio'],
            'is_illegal': 1 if str(row[BuildingCol.IS_VIOLATING]).strip() == 'Y' else 0,
            'is_trust_owner': 1 if row['owner_name'] and '신탁' in str(row['owner_name']) else 0,
            'short_term_weight': w_short[row.name] if row.name in w_short.index else 0,
        }
        return _rule_based_score(feats)

    df_target['rule_score'] = df_target.apply(get_rule_score, axis=1)

    # --- Step 2: ML 모델 예측 (Optional) ---
    model_path = os.path.join(project_root, 'models', 'fraud_rf_model.pkl')
    ml_available = False

    if os.path.exists(model_path):
        print("--- [AI] 학습된 모델로 예측 수행 ---")
        try:
            rf_model = joblib.load(model_path)

            def get_ai_prob(row):
                short_term_w = 0.0
                if pd.notna(row['ownership_changed_date']) and pd.notna(row['contract_date']):
                    days = (row['contract_date'] - row['ownership_changed_date']).days
                    if days < 90:
                        short_term_w = 0.3
                    elif days < 730:
                        short_term_w = 0.1

                is_trust = 1 if row['owner_name'] and '신탁' in str(row['owner_name']) else 0
                is_viol = 1 if str(row[BuildingCol.IS_VIOLATING]).strip() == 'Y' else 0

                feats = calculate_risk_features(
                    deposit_amount=row['rent_price'],
                    market_price=row['est_market_price'],
                    real_debt=0,
                    main_use=row['main_use'],
                    usage_approval_date=row['use_apr_day'],
                    is_illegal=is_viol,
                    is_trust_owner=is_trust,
                    short_term_weight=short_term_w
                )

                df_input = convert_to_model_input(feats)
                return rf_model.predict_proba(df_input)[0][1]

            df_target['ai_risk_prob'] = df_target.apply(get_ai_prob, axis=1)
            ml_available = True
            print(f"-> AI 예측 완료 (평균 위험도: {df_target['ai_risk_prob'].mean():.4f})")

        except Exception as e:
            print(f"!!! [Critical] AI 모델 예측 실패: {e}")
            df_target['ai_risk_prob'] = 0.0
    else:
        df_target['ai_risk_prob'] = 0.0

    # --- Step 3: 하이브리드 결합 ---
    if ml_available:
        df_target['hybrid_score'] = (
            df_target['rule_score'] * RULE_WEIGHT +
            df_target['ai_risk_prob'] * ML_WEIGHT
        )
        print(f"-> 하이브리드 판정 적용 (룰 {RULE_WEIGHT*100:.0f}% + ML {ML_WEIGHT*100:.0f}%)")
    else:
        df_target['hybrid_score'] = df_target['rule_score']
        print("-> ML 모델 미사용 — 룰 베이스 100%로 판정")

    # --- Step 4: 등급 판정 ---
    df_target['risk_level'] = df_target['hybrid_score'].apply(_score_to_level)

    # risk_score는 0~100 스케일로 변환 (DB 저장 및 표시용)
    df_target['risk_score'] = (df_target['hybrid_score'] * 100).astype(int).clip(0, 100)

    # --- DB 저장 ---
    df_save = df_target[[
        'key', 'rent_price', 'est_market_price',
        'jeonse_ratio', 'hug_safe_limit', 'hug_risk_ratio',
        'total_risk_ratio', 'est_loan_amount', 'risk_level', 'risk_score', 'ai_risk_prob'
    ]].copy()

    df_save.rename(columns={
        'key': 'address_key',
        'rent_price': 'used_rent_price',
        'est_market_price': 'used_market_price',
    }, inplace=True)

    df_save['analyzed_at'] = datetime.now()
    df_save = df_save.sort_values('total_risk_ratio', ascending=False).drop_duplicates(subset=['address_key'])

    print(f"--- 최종 저장: {len(df_save)}건 ---")
    try:
        with engine.connect() as conn:
            keys = df_save['address_key'].tolist()
            conn.execute(text(
                "DELETE FROM risk_analysis_result WHERE address_key IN :keys"
            ), {"keys": tuple(keys)})
            conn.commit()
        df_save.to_sql('risk_analysis_result', engine, if_exists='append', index=False)
        print("-> 저장 완료")
    except Exception as e:
        print(f"저장 실패: {e}")


if __name__ == "__main__":
    run_risk_analysis_pipeline()