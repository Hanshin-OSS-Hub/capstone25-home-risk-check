package hanshin.home_risk_check.user.dto;

import java.time.LocalDateTime;
import hanshin.home_risk_check.user.entity.Role;

public record UserResponse (
    Long id,
    String email,
    String nickname,
    String profileImageUrl,
    Role role,
    LocalDateTime regDate,
    LocalDateTime updDate
) {}