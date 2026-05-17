package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_poll")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPoll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "question", nullable = false, length = 100)
    private String question;

    @Column(name = "allow_multiple", nullable = false)
    private boolean allowMultiple;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Builder
    public PostPoll(String question, boolean allowMultiple, Post post) {
        this.question = question;
        this.allowMultiple = allowMultiple;
        this.post = post;
    }
}