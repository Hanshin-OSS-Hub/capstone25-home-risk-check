package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.PostPlaceResponse;
import hanshin.home_risk_check.community.entity.PostPlace;
import org.springframework.stereotype.Component;

@Component
public class PostPlaceResponseMapper {

    public PostPlaceResponse toPostPlaceResponse(PostPlace postPlace) {
        if (postPlace == null) {
            return null;
        }
        return PostPlaceResponse.builder()
                .id(postPlace.getId())
                .latitude(postPlace.getLatitude())
                .longitude(postPlace.getLongitude())
                .address(postPlace.getAddress())
                .placeName(postPlace.getPlaceName())
                .build();
    }
}