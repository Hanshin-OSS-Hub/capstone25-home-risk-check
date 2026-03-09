import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import os
import sys
import joblib

# Scikit-Learn
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score, roc_auc_score,
    classification_report, confusion_matrix
)
from app.services.feature_service import TRAIN_FEATURES

# ---------------------------------------------------------
# 1. 프로젝트 경로 및 폰트 설정
# ---------------------------------------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, '..'))

if PROJECT_ROOT not in sys.path:
    sys.path.append(PROJECT_ROOT)

from scripts.data_processor import load_and_engineer_features

# 한글 폰트 설정
import platform

if platform.system() == 'Darwin':
    font_family = 'AppleGothic'
elif platform.system() == 'Windows':
    font_family = 'Malgun Gothic'
else:
    font_family = 'NanumGothic'
plt.rc('font', family=font_family)
plt.rc('axes', unicode_minus=False)


def train_and_save_model():
    print("\n" + "=" * 60)
    print("[Start] 전세사기 위험도 예측 모델 학습 시작")
    print("=" * 60)

    # ---------------------------------------------------------
    # 2. 데이터 로드
    # ---------------------------------------------------------
    print("\n>> 1. 데이터 로드 및 전처리 중...")
    df = load_and_engineer_features()

    if df.empty:
        print("❌ 학습할 데이터가 없습니다.")
        return

    # ---------------------------------------------------------
    # 3. 추가 전처리 (One-Hot Encoding)
    # ---------------------------------------------------------
    if 'simple_type' in df.columns:
        df = pd.get_dummies(df, columns=['simple_type'], prefix='type')

    # ---------------------------------------------------------
    # 4. 정답지(Label) 생성: 'is_fraud'
    # ---------------------------------------------------------

    # 방어: 필수 원천 컬럼 존재 여부 확인
    required_cols = ['RENT_PRICE', 'ESTIMATED_MARKET_PRICE']
    missing_cols = [c for c in required_cols if c not in df.columns]
    if missing_cols:
        print(f"❌ 라벨 생성에 필요한 컬럼 없음: {missing_cols}")
        return

    # 원천값으로 직접 비율 계산 (파생 피처 컬럼 사용 안 함)
    raw_jeonse_ratio = df['RENT_PRICE'] / df['ESTIMATED_MARKET_PRICE'].replace(0, np.nan)
    raw_gap          = df['ESTIMATED_MARKET_PRICE'] - df['RENT_PRICE']

    # 조건 1: 깡통전세 (전세가율 80% 이상) - 원천값 기반
    cond_high_ratio = raw_jeonse_ratio >= 0.8

    # 조건 2: 무자본 갭투자 의심 (매매가 - 전세가 1,000만원 미만) - 원천값 기반
    cond_gap = raw_gap < 1000

    # 조건 3: 위반건축물 (사실(fact) 기반 단독 조건 - 피처와 겹쳐도 정보 자체가 다름)
    cond_illegal = df['is_illegal'] == 1

    # 조건 4: 복합 위험 - 전세가율 70% 이상 + 신탁 소유
    cond_trust_complex = (raw_jeonse_ratio >= 0.7) & (df['is_trust_owner'] == 1)

    # 조건 5: 복합 위험 - 전세가율 70% 이상 + 단기 소유
    cond_short_complex = (raw_jeonse_ratio >= 0.7) & (df['short_term_weight'] > 0)

    df['is_fraud'] = (
        cond_high_ratio |
        cond_gap        |
        cond_illegal    |
        cond_trust_complex |
        cond_short_complex
    ).astype(int)

    total_cnt = len(df)
    fraud_cnt = df['is_fraud'].sum()
    safe_cnt  = total_cnt - fraud_cnt

    print(f"\n[데이터 분포 확인]")
    print(f"   전체 데이터       : {total_cnt}건")
    print(f"   위험(Fraud) 레이블: {fraud_cnt}건 ({fraud_cnt / total_cnt * 100:.1f}%)")
    print(f"   안전(Safe)  레이블: {safe_cnt}건 ({safe_cnt / total_cnt * 100:.1f}%)")

    if fraud_cnt < 10 or safe_cnt < 10:
        print("⚠️ 경고: 데이터 불균형이 너무 심하거나 샘플이 부족하여 학습 효과가 낮을 수 있습니다.")

    # ---------------------------------------------------------
    # 5. 학습용 데이터셋 분리
    # ---------------------------------------------------------
    print(f"\n>> 피처 엔지니어링 모듈의 기준을 따릅니다.")

    target_features = TRAIN_FEATURES
    feature_cols = [f for f in target_features if f in df.columns]

    if len(feature_cols) != len(TRAIN_FEATURES):
        missing = set(TRAIN_FEATURES) - set(feature_cols)
        print(f"⚠️ [주의] 일부 피처가 누락되었습니다: {missing}")

    print(f"   최종 사용 피처({len(feature_cols)}개): {feature_cols}")

    X = df[feature_cols]
    y = df['is_fraud']

    if len(df) < 50:
        print("⚠️ 데이터가 50건 미만입니다. train/test 분리 없이 전체 데이터로 학습합니다.")
        X_train, X_test, y_train, y_test = X, X, y, y
    else:
        if len(np.unique(y)) < 2:
            print("❌ 레이블 클래스가 1개뿐입니다 (모두 안전 or 모두 위험). 학습 불가.")
            return

        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y
        )

    # ---------------------------------------------------------
    # 6. 모델 학습 (Random Forest)
    # ---------------------------------------------------------
    print("\n>> 2. 모델 학습 수행 (Random Forest)...")
    rf_model = RandomForestClassifier(
        n_estimators=200,
        max_depth=10,
        min_samples_leaf=2,
        class_weight='balanced',
        random_state=42,
        n_jobs=-1
    )
    rf_model.fit(X_train, y_train)
    print("   학습 완료!")

    # ---------------------------------------------------------
    # 7. 성능 평가 - 지표 보강
    # ---------------------------------------------------------
    print("\n>> 3. 성능 평가 결과")
    y_pred = rf_model.predict(X_test)

    # 기본 지표
    acc = accuracy_score(y_test, y_pred)
    print(f"   정확도(Accuracy): {acc:.4f}")

    # ROC-AUC
    roc = 0.0
    try:
        if len(np.unique(y_test)) > 1:
            y_pred_proba = rf_model.predict_proba(X_test)[:, 1]
            roc = roc_auc_score(y_test, y_pred_proba)
            print(f"   ROC-AUC 점수    : {roc:.4f}")
    except Exception as e:
        print(f"   (ROC 계산 건너뜀: {e})")

    # Precision / Recall / F1 - 핵심 지표
    print("\n   [상세 분류 리포트]")
    print(classification_report(y_test, y_pred, target_names=['안전(0)', '위험(1)']))

    # Confusion Matrix
    cm = confusion_matrix(y_test, y_pred)
    print("   [Confusion Matrix]")
    print(f"   실제\\예측   안전  위험")
    print(f"   안전(0)  {cm[0][0]:5d} {cm[0][1]:5d}   ← 안전인데 위험으로 예측 (FP)")
    print(f"   위험(1)  {cm[1][0]:5d} {cm[1][1]:5d}   ← 위험인데 안전으로 예측 (FN, 치명적)")

    # Recall 경고
    from sklearn.metrics import recall_score
    recall = recall_score(y_test, y_pred)
    if recall < 0.7:
        print(f"\n   ⚠️  위험 Recall: {recall:.4f} — 실제 위험 매물을 {(1-recall)*100:.1f}% 놓치고 있음")
    else:
        print(f"\n   ✅  위험 Recall: {recall:.4f}")

    # ---------------------------------------------------------
    # 8. 결과 저장
    # ---------------------------------------------------------
    model_dir = os.path.join(PROJECT_ROOT, 'models')
    if not os.path.exists(model_dir):
        os.makedirs(model_dir)

    # 1) 모델 파일 저장
    model_path = os.path.join(model_dir, 'fraud_rf_model.pkl')
    joblib.dump(rf_model, model_path)
    print(f"\n>> 4. 모델 저장 완료: {model_path}")

    # 2) 피처 중요도 시각화
    try:
        importances = rf_model.feature_importances_
        indices = np.argsort(importances)[::-1]
        top_n = min(10, len(feature_cols))

        plt.figure(figsize=(10, 6))
        sns.barplot(
            x=importances[indices][:top_n],
            y=np.array(feature_cols)[indices][:top_n],
            palette='viridis'
        )
        plt.title("AI 모델 중요 변수 (Top Factors)")
        plt.xlabel("중요도 (Importance)")
        plt.tight_layout()

        plot_path = os.path.join(model_dir, 'feature_importance.png')
        plt.savefig(plot_path)
        print(f"   -> 그래프 저장 완료: {plot_path}")
    except Exception as e:
        print(f"   (그래프 저장 실패: {e})")

    # 3) Confusion Matrix 시각화 저장
    try:
        plt.figure(figsize=(6, 5))
        sns.heatmap(
            cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=['안전(예측)', '위험(예측)'],
            yticklabels=['안전(실제)', '위험(실제)']
        )
        plt.title("Confusion Matrix")
        plt.tight_layout()

        cm_path = os.path.join(model_dir, 'confusion_matrix.png')
        plt.savefig(cm_path)
        print(f"   -> Confusion Matrix 저장 완료: {cm_path}")
    except Exception as e:
        print(f"   (Confusion Matrix 저장 실패: {e})")

    print("\n" + "=" * 60)
    print("✅ 학습 종료. 이제 predict.py를 실행할 수 있습니다.")
    print("=" * 60)


if __name__ == "__main__":
    train_and_save_model()