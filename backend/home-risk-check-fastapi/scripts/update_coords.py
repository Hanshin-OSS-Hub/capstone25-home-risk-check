"""
regions 테이블의 lat/lng가 NULL인 지역을 Kakao API로 조회해 직접 업데이트합니다.

실행 순서:
  1. python scripts/setup_region_codes.py   (meta 테이블 + regions 지역명 세팅)
  2. python scripts/update_coords.py        (regions lat/lng 채우기)
"""
import os
import sys
import time
import requests
from sqlalchemy import text
from dotenv import load_dotenv

# 프로젝트 루트 기준으로 .env 및 모듈 탐색
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
os.chdir(project_root)
sys.path.append(project_root)

# pydantic Settings가 무시하는 키(KAKAO_API_KEY 등)도 직접 로드
load_dotenv(os.path.join(project_root, '.env'))

from app.core.database import engine

KAKAO_API_KEY = os.getenv('KAKAO_API_KEY')


# ---------------------------------------------------------
# Kakao Local API - 주소명으로 좌표 조회
# ---------------------------------------------------------
def get_coordinates_from_kakao(region_name: str):
    """
    지역명(예: "서울 종로구")으로 Kakao API에서 위도/경도를 반환합니다.
    반환: (lat, lng) 또는 (None, None)
    """
    headers = {"Authorization": f"KakaoAK {KAKAO_API_KEY}"}
    params = {"query": region_name}

    try:
        # 1차: 주소 검색
        response = requests.get(
            "https://dapi.kakao.com/v2/local/search/address.json",
            headers=headers, params=params, timeout=5
        )
        response.raise_for_status()
        result = response.json()

        if result.get('documents'):
            return float(result['documents'][0]['y']), float(result['documents'][0]['x'])

        # 2차: 키워드 검색으로 재시도
        response2 = requests.get(
            "https://dapi.kakao.com/v2/local/search/keyword.json",
            headers=headers, params=params, timeout=5
        )
        response2.raise_for_status()
        result2 = response2.json()

        if result2.get('documents'):
            return float(result2['documents'][0]['y']), float(result2['documents'][0]['x'])

        print(f"   [API] 검색 결과 없음: {region_name}")
        return None, None

    except Exception as e:
        print(f"   [API] 호출 오류 ({region_name}): {e}")
        return None, None


# ---------------------------------------------------------
# 메인 로직
# ---------------------------------------------------------
def update_region_coordinates():
    if not KAKAO_API_KEY:
        print("❌ KAKAO_API_KEY 환경변수가 설정되지 않았습니다.")
        print(f"   확인 경로: {os.path.join(project_root, '.env')}")
        return

    print("🚀 regions 테이블 좌표 수집 시작...")

    select_sql = text("""
        SELECT region_code, region_name
        FROM regions
        WHERE lat IS NULL OR lng IS NULL
        ORDER BY region_code
    """)

    update_sql = text("""
        UPDATE regions
        SET lat = :lat, lng = :lng
        WHERE region_code = :code
    """)

    with engine.connect() as conn:
        rows = conn.execute(select_sql).mappings().fetchall()

    total = len(rows)
    if total == 0:
        print("✅ 좌표가 없는 지역이 없습니다. 이미 모두 채워져 있습니다.")
        return

    print(f"   ㄴ 좌표 수집 대상: {total}개 지역\n")

    success_count = 0
    fail_list = []

    for i, row in enumerate(rows, 1):
        code = row['region_code']
        name = row['region_name']

        lat, lng = get_coordinates_from_kakao(name)

        if lat and lng:
            with engine.begin() as conn:
                conn.execute(update_sql, {"lat": lat, "lng": lng, "code": code})
            success_count += 1
            print(f"   [{i}/{total}] ✅ {name} ({code}) → {lat}, {lng}")
        else:
            fail_list.append((code, name))
            print(f"   [{i}/{total}] ❌ {name} ({code}) → 좌표 없음")

        time.sleep(0.1)  # Kakao API rate limit 방지

    print(f"\n✅ 완료: {success_count}/{total}개 지역 좌표 저장")

    if fail_list:
        print(f"\n⚠️  좌표 수집 실패 목록 ({len(fail_list)}개):")
        for code, name in fail_list:
            print(f"   - {name} ({code})")


if __name__ == "__main__":
    update_region_coordinates()