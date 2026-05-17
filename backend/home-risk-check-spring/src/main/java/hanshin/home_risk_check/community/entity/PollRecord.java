package hanshin.home_risk_check.community.entity;

import hanshin.home_risk_check.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity()
@Table(
        name = "poll_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poll_option_user", columnNames = {"poll_id", "option_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_option", columnList = "option_id"),
                @Index(name = "idx_poll_user", columnList = "poll_id, user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_poll_id", nullable = false)
    private PostPoll postPoll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_option_id", nullable = false)
    private PollOption pollOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public PollRecord(PostPoll postPoll, PollOption pollOption, User user) {
        this.postPoll = postPoll;
        this.pollOption = pollOption;
        this.user = user;
    }
}