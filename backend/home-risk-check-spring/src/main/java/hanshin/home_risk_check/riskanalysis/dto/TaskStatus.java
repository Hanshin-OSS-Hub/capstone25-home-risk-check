package hanshin.home_risk_check.riskanalysis.dto;

/*
 * 위험도 분석 작업 상태. FastAPI TaskStatus 와 동일.
 */
public enum TaskStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
