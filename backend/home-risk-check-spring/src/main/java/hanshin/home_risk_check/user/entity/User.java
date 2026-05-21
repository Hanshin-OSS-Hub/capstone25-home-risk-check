package hanshin.home_risk_check.user.entity;

import hanshin.home_risk_check.community.entity.ImageFile;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_nickname", columnNames = {"nickname"}),
                @UniqueConstraint(name = "uk_profile_image", columnNames = {"profile_image_file_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_image_file_id")
    private ImageFile profileImageFile;

    @Column(name = "reg_date", nullable = false)
    @CreatedDate
    private LocalDateTime regDate;

    @Column(name = "upd_date", nullable = false)
    @LastModifiedDate
    private LocalDateTime updDate;

    @Builder
    public User(String email, String passwordHash, String nickname, Role role, ImageFile profileImageFile) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.profileImageFile = profileImageFile;
    }

    public void update(String newPasswordHash, String newNickname, ImageFile newProfileImageFile) {
        this.passwordHash = newPasswordHash;
        this.nickname = newNickname;
        this.profileImageFile = newProfileImageFile;
    }
}