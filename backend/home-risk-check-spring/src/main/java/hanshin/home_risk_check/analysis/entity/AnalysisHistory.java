package hanshin.home_risk_check.analysis.entity;

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
        name = "analysis_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_analysis_task", columnNames = {"task_id"})
        },
        indexes = {
                @Index(name = "idx_analysis_user_created_at", columnList = "user_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    // 분석 당시 위험도+안전등급 합본(AnalysisSnapshot)의 JSON 스냅샷.
    // 목록 요약·상세 모두 이 JSON을 역직렬화해 사용 (요약 필드 중복 컬럼 없음).
    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public AnalysisHistory(User user, String taskId, String resultJson) {
        this.user = user;
        this.taskId = taskId;
        this.resultJson = resultJson;
    }
}