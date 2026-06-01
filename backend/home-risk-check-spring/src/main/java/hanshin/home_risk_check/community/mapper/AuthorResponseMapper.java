package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.AuthorResponse;
import hanshin.home_risk_check.file.util.FileUrlResolver;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorResponseMapper {

    private final FileUrlResolver fileUrlResolver;

    public AuthorResponse toAuthorResponse(User user) {
        return AuthorResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(fileUrlResolver.toUrl(user.getProfileImageFile()))
                .build();
    }
}