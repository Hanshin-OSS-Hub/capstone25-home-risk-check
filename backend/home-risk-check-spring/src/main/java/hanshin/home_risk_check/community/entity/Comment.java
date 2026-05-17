package hanshin.home_risk_check.community.entity;

import hanshin.home_risk_check.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Builder(access = AccessLevel.PRIVATE)
    public Comment(Post post,
                   User user,
                   String content,
                   Comment parentComment) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.parentComment = parentComment;
    }

    public static Comment createRootComment(Post post, User user, String content){
        return Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .parentComment(null)
                .build();
    }

    public static Comment createReplyComment(Post post, User user, Comment parentComment, String content){
        return Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .parentComment(parentComment)
                .build();
    }
}