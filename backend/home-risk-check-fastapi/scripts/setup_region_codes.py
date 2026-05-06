import pandas as pd
import sys
import os
from sqlalchemy.types import String
from sqlalchemy import text
from datetime import datetime

# 중앙 설정 파일(engine) 임포트
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
# .env 파일 탐색 기준을 프로젝트 루트로 고정 (스크립트 실행 위치 무관)
os.chdir(project_root)
sys.path.append(project_root)
from app.core.database import engine, get_connection

# 특정 시군구 코드 수집을 위한 배열 (비어있으면 전국 전체)
TARGET_SGG_CODES = []

# --- 설정 ---
# 1. 원본 법정동 코드 CSV 파일 경로
CSV_PATH = os.path.join(project_root, 'data', '국토교통부_법정동코드_20250805.csv')

TABLE_SGG = "meta_sgg_codes"      # 1. 시군구 단위 테이블 (매매, 전월세용)
TABLE_BJDONG = "meta_bjdong_codes" # 2. 법정동 단위 테이블 (건축물대장용)

# 수집 시작 날짜를 현재 달로 설정
# 'fetch_data' 스크립트가 이 값을 보고 해당 월부터 수집을 시작합니다.
NOW_MONTH = datetime.now()
DEFAULT_START_DATE = NOW_MONTH.strftime('%Y%m')  # 예: '202502'


def setup_region_database():
    print(f"--- 1. 원본 법정동 코드 CSV 파일 로드 ---")
    print(f"파일 경로: {CSV_PATH}")

    try:
        df = pd.read_csv(CSV_PATH, sep=',', encoding='cp949', dtype=str)
    except Exception as e:
        print(f"CSV 로드 실패: {e}")
        return

    print("--- 2. 데이터 정제 및 가공 ---")

    # 원본 컬럼명 변경
    df = df.rename(columns={
        '법정동코드': 'code',
        '법정동명': 'name',
        '폐지여부': 'status'
    })

    # 현재 사용 중인 코드('존재')만 필터링
    df_active = df[df['status'] == '존재'].copy()

    # 시군구/법정동 코드 분리 생성
    df_active['sgg_code']    = df_active['code'].str.slice(0, 5)
    df_active['bjdong_code'] = df_active['code'].str.slice(5, 10)
    df_active['bjdong_name'] = df_active['name']

    # --- 3. 지역 필터링 ---
    print(f"--- 3. 목표 지역 필터링: {TARGET_SGG_CODES if TARGET_SGG_CODES else '전국(필터 없음)'} ---")
    # TARGET_SGG_CODES가 있을 때만 필터링, 없으면 전국 전체 사용
    if TARGET_SGG_CODES:
        df_active = df_active[df_active['sgg_code'].isin(TARGET_SGG_CODES)].copy()

    # --- 3-A. [시군구] 테이블 데이터 생성 (매매, 전월세용) ---
    # 시/도 레벨 코드(끝이 '000'인 것) 제외
    is_sigungu_code = ~df_active['sgg_code'].str.endswith('000')
    df_sgg_final = df_active[is_sigungu_code][['sgg_code']].drop_duplicates().copy()
    df_sgg_final['trade_last_fetched_date'] = DEFAULT_START_DATE
    df_sgg_final['rent_last_fetched_date']  = DEFAULT_START_DATE
    print(f"총 {len(df_sgg_final)}개의 *시군구* 코드(e.g., 11110)를 추출했습니다.")

    # --- 3-B. [법정동] 테이블 데이터 생성 (건축물대장용) ---
    # 읍면동 레벨만 (시/도, 시/군/구 레벨 제외)
    is_dong_level = ~df_active['code'].str.endswith('00000')
    df_bjdong_final = df_active[is_dong_level][['sgg_code', 'bjdong_code', 'bjdong_name']].drop_duplicates().copy()
    print(f"총 {len(df_bjdong_final)}개의 *법정동* 코드(e.g., 11680-10300)를 추출했습니다.")

    # --- 4. DB에 두 테이블 저장 ---
    try:
        print(f"--- 4-A. DB 테이블 '{TABLE_SGG}' (시군구) 저장 중 ---")
        df_sgg_final.to_sql(
            TABLE_SGG,
            con=engine,
            if_exists='replace',
            index=False,
            dtype={
                'sgg_code':               String(10),
                'trade_last_fetched_date': String(10),
                'rent_last_fetched_date':  String(10)
            }
        )
        print(f"성공: '{TABLE_SGG}' 테이블 생성이 완료되었습니다.")

        print(f"--- 4-B. DB 테이블 '{TABLE_BJDONG}' (법정동) 저장 중 ---")
        df_bjdong_final.to_sql(
            TABLE_BJDONG,
            con=engine,
            if_exists='replace',
            index=False,
            dtype={
                'sgg_code':               String(10),
                'bjdong_code':            String(10),
                'bjdong_name':            String(100)
            }
        )

        # to_sql(replace)은 PK 없이 생성하므로 복합 PK 수동 추가
        with engine.begin() as conn:
            conn.execute(text(f"""
                ALTER TABLE {TABLE_BJDONG}
                    MODIFY sgg_code VARCHAR(10) NOT NULL,
                    MODIFY bjdong_code VARCHAR(10) NOT NULL,
                    ADD PRIMARY KEY (sgg_code, bjdong_code)
            """))
        print(f"성공: '{TABLE_BJDONG}' 테이블 생성 + 복합 PK (sgg_code, bjdong_code) 설정 완료")

    except Exception as e:
        print(f"[오류] DB 저장 실패: {e}")


def sync_regions_from_meta():
    """
    meta_sgg_codes + meta_bjdong_codes -> regions 테이블 동기화
    - region_code : sgg_code (시군구 코드)
    - region_name : bjdong_name에서 파싱한 "서울 종로구" 형태
    - lat / lng   : NULL 유지 (이후 update_coords.py로 채움)
    - 이미 존재하는 region_code는 region_name만 UPDATE (좌표 보존)
    """
    print("--- [regions 동기화] meta_sgg_codes -> regions 테이블 ---")

    sql_select = """
        SELECT DISTINCT s.sgg_code, b.bjdong_name
        FROM meta_sgg_codes s
        JOIN meta_bjdong_codes b ON s.sgg_code = b.sgg_code
        ORDER BY s.sgg_code
    """

    try:
        with engine.connect() as conn:
            df = pd.read_sql(sql_select, conn)

        if df.empty:
            print("  [오류] meta_sgg_codes 또는 meta_bjdong_codes 데이터가 없습니다.")
            return

        def parse_region_name(full_name):
            tokens = str(full_name).strip().split()
            if len(tokens) < 2:
                return tokens[0] if tokens else full_name
            sido, gugun = tokens[0], tokens[1]
            if "특별" in sido or "광역" in sido:
                sido = sido[:2]
            elif "경상" in sido or "전라" in sido or "충청" in sido:
                sido = sido[0] + sido[2]  # 경상남도 -> 경남
            elif sido in ("경기도", "강원도", "제주도"):
                sido = sido[:2]
            return f"{sido} {gugun}"

        # sgg_code 기준 첫 번째 bjdong_name만 사용 (중복 제거)
        df_unique = df.drop_duplicates(subset="sgg_code").copy()
        df_unique["region_name"] = df_unique["bjdong_name"].apply(parse_region_name)

        # INSERT ... ON DUPLICATE KEY UPDATE (좌표는 건드리지 않음)
        upsert_sql = text("""
            INSERT INTO regions (region_code, region_name)
            VALUES (:code, :name)
            ON DUPLICATE KEY UPDATE region_name = VALUES(region_name)
        """)

        with engine.begin() as conn:
            for _, row in df_unique.iterrows():
                conn.execute(upsert_sql, {
                    "code": row["sgg_code"],
                    "name": row["region_name"]
                })

        print(f"  성공: {len(df_unique)}개 지역을 regions 테이블에 동기화했습니다.")

    except Exception as e:
        print(f"  [오류] regions 동기화 실패: {e}")


def get_sgg_codes():
    """meta_bjdong_codes에서 전체 시군구 코드 반환 (테이블이 존재할 때만 사용)"""
    conn = get_connection()
    sql = "SELECT DISTINCT sgg_code FROM meta_bjdong_codes"
    df_rows = pd.read_sql(sql, conn)
    result_list = df_rows['sgg_code'].tolist()

    if not result_list:
        print("  [Wait] 작업할 대상이 없습니다. 대기 중...")
        return []

    return result_list


if __name__ == "__main__":
    # Step 1: 전국 시군구/법정동 메타 테이블 생성
    setup_region_database()
    # Step 2: regions 테이블에 지역명 동기화 (좌표는 update_coords.py로 별도 수집)
    sync_regions_from_meta()