package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * 게시글 Repository
 * DB의 post 테이블에 접근하는 인터페이스
 *
 * JpaRepository<Post, Long>
 * - Post: 관리할 엔티티 타입
 * - Long: Post 엔티티의 PK 타입 (postId)
 *
 * 기본적으로 save(), findById(), findAll(), delete() 같은 메서드는
 * JpaRepository가 이미 제공함
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /*
     * 카테고리별 게시글 목록 조회
     * createdAt 기준 내림차순 정렬 (최신글 먼저)
     *
     * 반환 타입이 Page<Post> 이므로
     * 페이징 처리(page, size)와 전체 페이지 정보까지 함께 가져올 수 있음
     *
     * 예:
     * categoryLabel = "서울시 성동구"
     * page = 0, size = 10
     *
     * -> "서울시 성동구" 카테고리의 게시글 10개를 최신순으로 조회
     */
    Page<Post> findAllByCategoryLabelOrderByCreatedAtDesc(String categoryLabel, Pageable pageable);

    /*
     * 전체 게시글 목록 조회
     * createdAt 기준 내림차순 정렬 (최신글 먼저)
     *
     * category 조건 없이 전체 게시글을 페이지 단위로 조회할 때 사용
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}