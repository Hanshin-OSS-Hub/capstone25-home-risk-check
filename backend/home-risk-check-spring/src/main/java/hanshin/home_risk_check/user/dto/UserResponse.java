package hanshin.home_risk_check.user.dto;

import hanshin.home_risk_check.user.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Role role;
    private LocalDateTime regDate;
    private LocalDateTime updDate;
}