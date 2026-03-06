package hanshin.home_risk_check.community.dto;

import lombok.Getter;

@Getter
public class PostUpdateRequest {
    private String categoryLabel;
    private String title;
    private String content;
}