package hanshin.home_risk_check.community.dto;

import lombok.Builder;

@Builder
public record PostPlaceResponse(
    Long id,
    Double latitude,
    Double longitude,
    String address,
    String placeName
) {}