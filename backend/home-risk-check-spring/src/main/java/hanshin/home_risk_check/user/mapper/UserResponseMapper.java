package hanshin.home_risk_check.user.mapper;

import hanshin.home_risk_check.file.util.FileUrlResolver;
import hanshin.home_risk_check.user.dto.UserResponse;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserResponseMapper {

    private final FileUrlResolver fileUrlResolver;

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(fileUrlResolver.toUrl(user.getProfileImageFile()))
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}