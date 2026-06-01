package hanshin.home_risk_check.riskanalysis.entity;

import hanshin.home_risk_check.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/*
 * 사용자별 위험도 분석 이력.
 * 분석이 COMPLETED 된 시점에 핵심 결과를 저장 (마이페이지 분석 이력 조회용).
 * task_id 유니크로 동일 작업 중복 저장 방지.
 */
@Getter
@Entity
@Table(
        name = "risk_analysis_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_risk_analysis_task", columnNames = {"task_id"})
        },
        indexes = {
                @Index(name = "idx_risk_analysis_user_created_at", columnList = "user_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RiskAnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "deposit", nullable = false)
    private long deposit;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public RiskAnalysisHistory(User user, String taskId, String address, long deposit, int riskScore, String riskLevel) {
        this.user = user;
        this.taskId = taskId;
        this.address = address;
        this.deposit = deposit;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }
}