package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.entity.Comment;
import hanshin.home_risk_check.community.util.AuthorValidator;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommentResponseMapper {

    private final AuthorResponseMapper authorResponseMapper;

    public CommentResponse toCommentResponse(Comment comment, boolean isWrittenByMe, long replyCount) {
        return CommentResponse.builder()
                .id(comment.getId())
                .author(authorResponseMapper.toAuthorResponse(comment.getUser()))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .isWrittenByMe(isWrittenByMe)
                .replyCount(replyCount)
                .build();
    }

    public Slice<CommentResponse> toCommentResponses(
            Slice<Comment> comments,
            Map<Long, Boolean> isWrittenByMe,
            Map<Long, Long> replyCounts
    ) {
        return comments.map(comment -> toCommentResponse(
                comment,
                isWrittenByMe.getOrDefault(comment.getId(), false),
                replyCounts.getOrDefault(comment.getId(), 0L)
        ));
    }
}