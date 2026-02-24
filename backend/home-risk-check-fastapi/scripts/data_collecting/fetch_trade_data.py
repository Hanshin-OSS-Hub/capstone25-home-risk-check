"""
매매 실거래가 데이터 수집 스크립트

국토교통부 API를 통해 아파트, 연립다세대, 오피스텔의
매매 실거래가 데이터를 수집하여 DB에 저장합니다.

수집 이력 관리: api_price_log 테이블 (sigungu_code, deal_ymd, data_type)
- 이미 수집된 (지역, 월, 유형) 조합은 SKIP → 재실행 시 중복 수집 없음
- 마이그레이션: scripts/migrate_price_log.sql 실행 후 사용

사용법:
    # 프로젝트 루트에서 실행
    python -m scripts.fetch_data.fetch_trade_data

    # 또는 직접 실행
    python scripts/fetch_data/fetch_trade_data.py
"""
import os
import sys
import time
from pathlib import Path
from datetime import datetime
from functools import lru_cache

import requests
import pandas as pd
import xml.etree.ElementTree as ET
from sqlalchemy import text
from dotenv import load_dotenv

# =============================================================================
# 경로 설정 (프로젝트 루트 기준)
# =============================================================================
# 이 파일의 위치: fraud-detector/scripts/fetch_data/fetch_trade_data.py
# 프로젝트 루트: fraud-detector/
SCRIPT_DIR   = Path(__file__).resolve().parent  # scripts/fetch_data/
SCRIPTS_DIR  = SCRIPT_DIR.parent                # scripts/
PROJECT_ROOT = SCRIPTS_DIR.parent               # fraud-detector/

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

# =============================================================================
# app 모듈 import (경로 설정 후)
# =============================================================================
from app.core import get_engine

# =============================================================================
# 환경 변수 로드
# =============================================================================
load_dotenv(PROJECT_ROOT / '.env')
API_SERVICE_KEY = os.getenv("API_SERVICE_KEY")

# =============================================================================
# 상수 정의
# =============================================================================
API_URLS_TRADE = {
    "아파트":    "https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade",
    "연립다세대": "https://apis.data.go.kr/1613000/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade",
    "오피스텔":  "https://apis.data.go.kr/1613000/RTMSDataSvcOffiTrade/getRTMSDataSvcOffiTrade",
}

# data_type 값: api_price_log.data_type 컬럼에 저장되는 식별자
BUILDING_TYPE_TO_LOG_KEY = {
    "아파트":    "trade_apt",
    "연립다세대": "trade_rh",
    "오피스텔":  "trade_offi",
}

TRADE_TABLE_NAME  = "raw_trade"
REGION_TABLE_NAME = "meta_sgg_codes"
PRICE_LOG_TABLE   = "api_price_log"

LEGAL_CODES_CSV_PATH = PROJECT_ROOT / 'data' / '국토교통부_법정동코드_20250805.csv'

OLDEST_DATE_YMD          = "202301"
API_CALL_LIMIT_PER_RUN   = 9900
SLEEP_TIME_BETWEEN_CALLS = 0.5


# =============================================================================
# 헬퍼 함수: 법정동 코드 로드
# =============================================================================
@lru_cache(maxsize=1)
def get_bjdong_code_map() -> dict:
    """
    법정동 코드 CSV를 로드하여 (시군구코드, 동이름) -> 법정동코드 매핑 생성

    Returns:
        dict: {(sgg_code, dong_name): bjdong_code}
    """
    print("--- [헬퍼] 법정동 코드 마스터(CSV) 로드 중... (1회 실행) ---")

    if not LEGAL_CODES_CSV_PATH.exists():
        print(f"[치명적 오류] 법정동 코드 파일이 없습니다: {LEGAL_CODES_CSV_PATH}")
        return {}

    try:
        df = pd.read_csv(LEGAL_CODES_CSV_PATH, sep=',', encoding='cp949', dtype=str)
    except Exception as e:
        print(f"[치명적 오류] 법정동 코드 CSV 로드 실패: {e}")
        return {}

    df = df.rename(columns={'법정동코드': 'code', '법정동명': 'name', '폐지여부': 'status'})
    df_active     = df[df['status'] == '존재'].copy()
    df_dong_level = df_active[~df_active['code'].str.endswith('00000')].copy()

    df_dong_level['sgg_code']       = df_dong_level['code'].str.slice(0, 5)
    df_dong_level['bjdong_code']    = df_dong_level['code'].str.slice(5, 10)
    df_dong_level['dong_name_only'] = df_dong_level['name'].str.split().str[-1]

    code_map = {
        (row.sgg_code, row.dong_name_only): row.bjdong_code
        for row in df_dong_level.itertuples()
    }

    print(f"--- [헬퍼] 법정동 코드 맵({len(code_map)}개) 생성 완료 ---")
    return code_map


# =============================================================================
# api_price_log 관련 함수
# =============================================================================
def get_fetched_log_keys(sgg_code: str, deal_ymd: str) -> set:
    """
    api_price_log에서 특정 (지역, 월)에 대해 이미 수집된 data_type 집합 반환

    Returns:
        set: {'trade_apt', 'trade_rh', 'trade_offi'} 중 수집 완료된 항목
    """
    engine = get_engine()
    try:
        with engine.connect() as conn:
            result = conn.execute(
                text("""
                    SELECT data_type FROM api_price_log
                    WHERE sigungu_code = :sgg_code
                      AND deal_ymd     = :deal_ymd
                      AND data_type    IN ('trade_apt', 'trade_rh', 'trade_offi')
                """),
                {"sgg_code": sgg_code, "deal_ymd": deal_ymd},
            )
            return {row[0] for row in result}
    except Exception as e:
        print(f"  [경고] api_price_log 조회 실패 ({sgg_code}-{deal_ymd}): {e}")
        return set()


def mark_as_fetched(sgg_code: str, deal_ymd: str, data_type: str) -> None:
    """
    api_price_log에 수집 완료 기록 INSERT (이미 존재하면 무시)

    Args:
        sgg_code:  시군구 코드
        deal_ymd:  거래년월 (YYYYMM)
        data_type: 'trade_apt' | 'trade_rh' | 'trade_offi'
    """
    engine = get_engine()
    try:
        with engine.connect() as conn:
            conn.execute(
                text("""
                    INSERT IGNORE INTO api_price_log
                        (sigungu_code, deal_ymd, data_type, collected_at)
                    VALUES
                        (:sgg_code, :deal_ymd, :data_type, NOW())
                """),
                {"sgg_code": sgg_code, "deal_ymd": deal_ymd, "data_type": data_type},
            )
            conn.commit()
    except Exception as e:
        print(f"  [경고] api_price_log 기록 실패 ({sgg_code}-{deal_ymd}-{data_type}): {e}")


# =============================================================================
# XML 파싱 함수
# =============================================================================
def parse_trade_xml_to_df(xml_text: str, code_map: dict, building_type: str) -> pd.DataFrame:
    """
    매매 실거래가 API의 XML 응답을 DataFrame으로 파싱

    Args:
        xml_text:      API 응답 XML 문자열
        code_map:      법정동 코드 매핑 딕셔너리
        building_type: 건물 유형 (아파트, 연립다세대, 오피스텔)

    Returns:
        pd.DataFrame: 파싱된 매매 데이터 (0건이면 빈 DataFrame)
    """
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return pd.DataFrame()

    if root.findtext('.//resultCode', '99') not in ('00', '000'):
        return pd.DataFrame()

    items = root.findall('.//item')
    if not items:
        return pd.DataFrame()

    # 건물 유형별 건물명 태그
    name_tag = {'아파트': 'aptNm', '오피스텔': 'offiNm', '연립다세대': 'mhouseNm'}.get(building_type, 'aptNm')

    # 데이터 1000건 초과 경고
    total_count = int(root.findtext('.//totalCount', '0') or 0)
    if total_count >= 1000:
        print(f"  [경고] {building_type} totalCount={total_count} — numOfRows(1000) 초과 가능, 누락 데이터 있을 수 있음")

    records = []
    for item in items:
        jibun_str = item.findtext('jibun', '').strip()
        if not jibun_str:
            continue

        parts   = jibun_str.split('-')
        bonbeon = parts[0].lstrip('0').strip().zfill(4) or '0000'
        bubeon  = parts[1].lstrip('0').strip().zfill(4) if len(parts) > 1 else '0000'

        deal_date = (
            f"{item.findtext('dealYear', '')}"
            f"{item.findtext('dealMonth', '').zfill(2)}"
            f"{item.findtext('dealDay', '').zfill(2)}"
        )

        sgg_code  = item.findtext('sggCd', '').strip()
        dong_name = item.findtext('umdNm', '').strip()

        bjdong_code = code_map.get((sgg_code, dong_name))
        if bjdong_code is None:
            continue

        records.append({
            'district':          sgg_code,
            'legal_dong':        bjdong_code,
            'main_jibun':        bonbeon,
            'sub_jibun':         bubeon,
            'trade_price':       item.findtext('dealAmount', '0').replace(',', '').strip(),
            'contract_date':     deal_date,
            'exclusive_area':    item.findtext('excluUseAr', ''),
            'floor':             item.findtext('floor', ''),
            'building_name':     item.findtext(name_tag, ''),
            'construction_year': item.findtext('buildYear', ''),
            'building_type':     building_type,
        })

    return pd.DataFrame(records)


# =============================================================================
# API 호출 및 저장 (건물 유형별 독립 처리 + api_price_log 연동)
# =============================================================================
def fetch_trade_data_and_save(lawd_cd: str, deal_ymd: str, code_map: dict) -> bool:
    """
    매매 데이터 수집 및 DB 저장

    - api_price_log를 확인해 이미 수집된 건물 유형은 SKIP
    - 각 유형 수집 성공 시 즉시 api_price_log에 기록 (부분 실패 추적 가능)

    Args:
        lawd_cd:   시군구 코드
        deal_ymd:  거래년월 (YYYYMM)
        code_map:  법정동 코드 매핑

    Returns:
        bool: 전체 성공 여부 (하나라도 실패 시 False)
    """
    engine = get_engine()

    # 이미 수집된 유형 확인 (부분 재시작 지원)
    already_fetched = get_fetched_log_keys(lawd_cd, deal_ymd)

    all_success = True

    for building_type, api_url in API_URLS_TRADE.items():
        log_key = BUILDING_TYPE_TO_LOG_KEY[building_type]

        # ── 이미 수집 완료된 유형은 SKIP ──
        if log_key in already_fetched:
            print(f"  -> {building_type}: 이미 수집됨, SKIP.")
            continue

        params = {
            'serviceKey': API_SERVICE_KEY,
            'LAWD_CD':    lawd_cd,
            'DEAL_YMD':   deal_ymd,
            'numOfRows':  '1000',
        }

        try:
            response = requests.get(api_url, params=params, timeout=30)
            response.raise_for_status()

            df = parse_trade_xml_to_df(response.text, code_map, building_type)

            if df.empty:
                print(f"  -> {building_type}: 0건.")
            else:
                with engine.connect() as conn:
                    df.to_sql(
                        TRADE_TABLE_NAME,
                        con=conn,
                        if_exists='append',
                        index=False,
                        method='multi',
                    )
                    conn.commit()
                print(f"  -> {building_type}: {len(df)}건 저장 완료.")

            # 수집 성공 (0건 포함) → 로그 기록
            mark_as_fetched(lawd_cd, deal_ymd, log_key)

        except requests.RequestException as e:
            print(f"  -> {building_type} API 요청 실패: {e}")
            all_success = False
        except Exception as e:
            print(f"  -> {building_type} 처리 실패: {e}")
            all_success = False

    return all_success


# =============================================================================
# DB 조회 함수 (api_price_log 기반)
# =============================================================================
def get_all_regions() -> list:
    """
    meta_sgg_codes에서 전체 지역 코드 목록 반환

    Returns:
        list: [sgg_code, ...]
    """
    engine = get_engine()
    try:
        with engine.connect() as conn:
            query = f"SELECT sgg_code FROM {REGION_TABLE_NAME} ORDER BY sgg_code ASC"
            df = pd.read_sql(query, con=conn)
            return df['sgg_code'].tolist()
    except Exception as e:
        print(f"[오류] 지역 목록 조회 실패: {e}")
        return []


def get_all_pending(regions: list) -> dict:
    """
    전체 지역의 미수집 달 목록을 단 1회 DB 쿼리로 계산

    api_price_log에서 3개 유형(trade_apt, trade_rh, trade_offi)이
    모두 존재하는 (지역, 월)을 완료로 판단하고,
    OLDEST_DATE_YMD ~ 현재 월 범위 중 나머지를 미수집으로 반환.

    Args:
        regions: sgg_code 목록

    Returns:
        dict: {sgg_code: ['202502', '202501', ...]} (내림차순, 미수집 달만 포함)
    """
    engine = get_engine()
    all_log_keys = set(BUILDING_TYPE_TO_LOG_KEY.values())  # {'trade_apt', 'trade_rh', 'trade_offi'}

    # 수집 완료된 (지역, 월) 집합을 한 번에 로드
    try:
        with engine.connect() as conn:
            result = conn.execute(text("""
                SELECT sigungu_code, deal_ymd, GROUP_CONCAT(data_type) AS types
                FROM api_price_log
                WHERE data_type IN ('trade_apt', 'trade_rh', 'trade_offi')
                GROUP BY sigungu_code, deal_ymd
            """))
            fully_done = {
                (row[0], row[1])
                for row in result
                if set(row[2].split(',')) >= all_log_keys
            }
    except Exception as e:
        print(f"[경고] 수집 이력 일괄 조회 실패: {e}")
        fully_done = set()

    # 전체 월 범위 계산
    oldest_dt  = pd.to_datetime(OLDEST_DATE_YMD, format='%Y%m')
    current_dt = pd.to_datetime(datetime.now().strftime('%Y%m'), format='%Y%m')
    all_months = [m.strftime('%Y%m') for m in pd.date_range(oldest_dt, current_dt, freq='MS')]

    # 지역별 미수집 달 목록 구성
    pending = {}
    for sgg in regions:
        months = sorted(
            [m for m in all_months if (sgg, m) not in fully_done],
            reverse=True
        )
        if months:
            pending[sgg] = months

    done_count = len(regions) - len(pending)
    print(f"    수집 완료 지역: {done_count}개 / 미수집 지역: {len(pending)}개")
    return pending


# =============================================================================
# 메인 실행 루프
# =============================================================================
def main_fetch_loop():
    """
    api_price_log 기반 라운드 로빈 매매 데이터 수집

    - 라운드 시작 시 전체 미수집 목록을 1회 DB 쿼리로 로드
    - 각 지역의 미수집 월을 최신 순으로 1개씩 수집 (라운드 로빈)
    - 이미 수집된 (지역, 월, 유형)은 SKIP → 재실행 안전
    - API 호출 한도 도달 시 중단 (다음 실행에서 이어서 수집)
    """
    call_count = 0

    code_map = get_bjdong_code_map()
    if not code_map:
        print("[치명적 오류] 법정동 코드 맵을 생성할 수 없어 종료합니다.")
        return

    print(f"--- [매매] api_price_log 기반 데이터 수집 시작 ---")
    print(f"    프로젝트 루트: {PROJECT_ROOT}")
    print(f"    수집 범위: {OLDEST_DATE_YMD} ~ 현재")
    print(f"    API 호출 한도: {API_CALL_LIMIT_PER_RUN}")

    regions = get_all_regions()
    if not regions:
        print("[오류] 수집 대상 지역이 없습니다. meta_sgg_codes 테이블을 확인하세요.")
        return

    print(f"    전체 지역: {len(regions)}개")

    try:
        while call_count < API_CALL_LIMIT_PER_RUN:
            print(f"\n--- [매매] 새 라운드 시작 (현재 호출: {call_count}/{API_CALL_LIMIT_PER_RUN}) ---")

            # 라운드 시작 시 미수집 목록 1회 로드 (지역 수만큼 쿼리 절감)
            pending_map = get_all_pending(regions)

            if not pending_map:
                print("\n[완료] 모든 지역의 데이터 수집이 완료되었습니다.")
                break

            work_done_in_this_round = False

            for sgg_code in regions:
                if call_count >= API_CALL_LIMIT_PER_RUN:
                    break

                pending_months = pending_map.get(sgg_code)
                if not pending_months:
                    continue  # 이 지역은 모든 달 수집 완료

                # 이번 라운드에서 가장 최신 미수집 달 1개만 처리
                date_ym = pending_months[0]
                work_done_in_this_round = True

                print(
                    f"[{datetime.now().strftime('%H:%M:%S')}] "
                    f"수집: {sgg_code}-{date_ym} (호출 {call_count + 1})"
                )
                call_count += 1

                success = fetch_trade_data_and_save(sgg_code, date_ym, code_map)
                if not success:
                    print(f"  -> [경고] {sgg_code}-{date_ym} 일부 유형 처리 실패.")

                time.sleep(SLEEP_TIME_BETWEEN_CALLS)

            if call_count >= API_CALL_LIMIT_PER_RUN:
                print(f"\n[한도 도달] API 호출 한도({API_CALL_LIMIT_PER_RUN})에 도달했습니다.")
                break

            if not work_done_in_this_round:
                print("\n[완료] 모든 지역의 데이터 수집이 완료되었습니다.")
                break

    except KeyboardInterrupt:
        print("\n[중단] 사용자에 의해 중지되었습니다.")

    print(f"\n--- [매매] 수집 세션 종료 (총 {call_count}회 호출) ---")


# =============================================================================
# 엔트리포인트
# =============================================================================
if __name__ == "__main__":
    main_fetch_loop()