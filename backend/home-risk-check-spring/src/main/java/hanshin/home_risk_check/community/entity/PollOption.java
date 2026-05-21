package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "poll_option",
        uniqueConstraints = {
                @UniqueConstraint(name = "uK_order_poll", columnNames = {"poll_order", "post_poll_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "poll_order", nullable = false)
    private int pollOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_poll_id", nullable = false)
    private PostPoll postPoll;

    @Builder
    public PollOption(String optionName, int pollOrder, PostPoll postPoll){
        this.optionName = optionName;
        this.pollOrder = pollOrder;
        this.postPoll = postPoll;
    }
}