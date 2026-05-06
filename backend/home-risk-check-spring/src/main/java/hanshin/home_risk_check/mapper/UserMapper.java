package hanshin.home_risk_check.mapper;

import hanshin.home_risk_check.user.dto.SignupRequest;
import hanshin.home_risk_check.user.dto.UserResponse;
import hanshin.home_risk_check.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "profileImageUrl", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "regDate", ignore = true)
    @Mapping(target = "updDate", ignore = true)
    User toEntity(SignupRequest request);

    UserResponse from(User user);
}