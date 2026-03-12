package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
 * 게시글 응답(Response) DTO
 *
 * 서버에서 게시글 정보를 조회한 뒤
 * 클라이언트(프론트)에게 전달할 데이터를 담는 객체
 *
 * 예 응답 JSON
 *
 * {
 *   "postId": 1,
 *   "authorId": 3,
 *   "categoryLabel": "서울시 성동구",
 *   "title": "이 매물 괜찮나요?",
 *   "content": "등기부등본을 보니 조금 이상합니다.",
 *   "createdAt": "...",
 *   "updatedAt": "..."
 * }
 */
@Getter
@Builder
public class PostResponse {

    /*
     * 게시글 ID (PK)
     */
    private Long postId;

    /*
     * 작성자 ID
     */
    private Long authorId;

    /*
     * 게시글 카테고리 라벨
     * 예: "서울시 성동구"
     */
    private String categoryLabel;

    /*
     * 게시글 제목
     */
    private String title;

    /*
     * 게시글 내용
     */
    private String content;

    /*
     * 게시글 생성 시간
     */
    private LocalDateTime createdAt;

    /*
     * 게시글 수정 시간
     */
    private LocalDateTime updatedAt;

    /*
     * Entity → DTO 변환 메서드
     *
     * Post 엔티티를 PostResponse DTO로 변환한다.
     *
     * Service에서 보통 이렇게 사용한다.
     *
     * PostResponse response = PostResponse.from(post);
     */
    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .categoryLabel(post.getCategoryLabel())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}