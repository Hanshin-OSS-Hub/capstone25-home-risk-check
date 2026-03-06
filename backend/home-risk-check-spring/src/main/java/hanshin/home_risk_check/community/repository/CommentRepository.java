package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostIdOrderByRootCommentIdAscDepthAscCreatedAtAsc(Long postId);
}