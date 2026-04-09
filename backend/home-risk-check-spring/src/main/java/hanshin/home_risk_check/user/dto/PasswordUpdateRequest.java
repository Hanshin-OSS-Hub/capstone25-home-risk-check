package hanshin.home_risk_check.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordUpdateRequest {
    private String currentPassword;
    private String newPassword;
}