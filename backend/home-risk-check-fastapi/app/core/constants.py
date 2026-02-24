# constants.py
# DB 테이블명 및 칼럼명 중앙 관리
# 칼럼명이 바뀌면 여기만 수정하면 됩니다.


# ────────────────────────────────────────────
# 테이블명
# ────────────────────────────────────────────
class Table:
    BUILDING_INFO       = "building_info"
    BUILDING_TITLE_INFO = "building_title_info"
    PUBLIC_PRICE        = "public_price_history"
    RAW_RENT            = "raw_rent"
    RAW_TRADE           = "raw_trade"
    REGIONS             = "regions"


# ────────────────────────────────────────────
# building_info 칼럼
# ────────────────────────────────────────────
class BuildingCol:
    ID                  = "id"
    UNIQUE_NUMBER       = "unique_number"
    BUILDING_ID_CODE    = "building_id_code"

    ROAD_ADDRESS        = "road_address"
    LOT_ADDRESS         = "lot_address"
    DETAIL_ADDRESS      = "detail_address"

    EXCLUSIVE_AREA      = "exclusive_area"
    MAIN_USE            = "main_use"
    STRUCTURE_TYPE      = "structure_type"

    OWNER_NAME          = "owner_name"
    OWNERSHIP_CHANGED   = "ownership_changed_date"
    OWNERSHIP_CAUSE     = "ownership_cause"
    IS_VIOLATING        = "is_violating"        # ← 변경: is_violating_building → is_violating

    CREATED_AT          = "created_at"
    UPDATED_AT          = "updated_at"


# ────────────────────────────────────────────
# building_title_info 칼럼
# ────────────────────────────────────────────
class BuildingTitleCol:
    ID              = "id"
    UNIQUE_NUMBER   = "unique_number"
    SIGUNGU_CODE    = "sigungu_code"
    BJDONG_CODE     = "bjdong_code"
    BUNJI           = "bunji"

    ROAD_ADDRESS    = "road_address"
    DETAIL_ADDRESS  = "detail_address"
    DONG_NAME       = "dong_name"

    MAIN_USE        = "main_use"
    STRUCTURE_TYPE  = "structure_type"
    TOTAL_FLOOR_AREA = "total_floor_area"

    HOUSEHOLD_CNT   = "household_cnt"
    GRND_FLR_CNT    = "grnd_flr_cnt"
    UND_FLR_CNT     = "und_flr_cnt"

    USE_APR_DAY     = "use_apr_day"
    IS_VIOLATING    = "is_violating"


# ────────────────────────────────────────────
# public_price_history 칼럼
# ────────────────────────────────────────────
class PublicPriceCol:
    ID              = "id"
    BUILDING_INFO_ID = "building_info_id"
    BASE_DATE       = "base_date"
    PRICE           = "price"
    CREATED_AT      = "created_at"


# ────────────────────────────────────────────
# raw_rent / raw_trade 공통 칼럼
# ────────────────────────────────────────────
class RawTransactionCol:
    ID                  = "id"
    DISTRICT            = "district"
    LEGAL_DONG          = "legal_dong"
    MAIN_JIBUN          = "main_jibun"
    SUB_JIBUN           = "sub_jibun"
    CONTRACT_DATE       = "contract_date"
    EXCLUSIVE_AREA      = "exclusive_area"
    FLOOR               = "floor"
    BUILDING_NAME       = "building_name"
    CONSTRUCTION_YEAR   = "construction_year"
    BUILDING_TYPE       = "building_type"
    CREATED_AT          = "created_at"


# ────────────────────────────────────────────
# raw_rent 전용 칼럼
# ────────────────────────────────────────────
class RawRentCol(RawTransactionCol):
    DEPOSIT         = "deposit"
    MONTHLY_RENT    = "monthly_rent"
    CONTRACT_TYPE   = "contract_type"


# ────────────────────────────────────────────
# raw_trade 전용 칼럼
# ────────────────────────────────────────────
class RawTradeCol(RawTransactionCol):
    TRADE_PRICE     = "trade_price"