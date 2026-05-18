package hanshin.home_risk_check.community.dto;

public record AuthorResponse(
    Long id,
    String nickname,
    String profileImageUrl
) {}