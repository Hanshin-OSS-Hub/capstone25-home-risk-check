package hanshin.home_risk_check.user.mapper;

import hanshin.home_risk_check.user.dto.UserResponse;
import hanshin.home_risk_check.user.entity.Role;
import hanshin.home_risk_check.user.entity.User;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-13T13:26:07+0900",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse from(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String email = null;
        String nickname = null;
        String profileImageUrl = null;
        Role role = null;
        LocalDateTime regDate = null;
        LocalDateTime updDate = null;

        id = user.getId();
        email = user.getEmail();
        nickname = user.getNickname();
        profileImageUrl = user.getProfileImageUrl();
        role = user.getRole();
        regDate = user.getRegDate();
        updDate = user.getUpdDate();

        UserResponse userResponse = new UserResponse( id, email, nickname, profileImageUrl, role, regDate, updDate );

        return userResponse;
    }
}
