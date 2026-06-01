package hanshin.home_risk_check.user.dto;

import java.time.LocalDateTime;
import hanshin.home_risk_check.user.entity.Role;
import lombok.Builder;

@Builder
public record UserResponse (
    Long id,
    String email,
    String nickname,
    String profileImageUrl,
    Role role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}