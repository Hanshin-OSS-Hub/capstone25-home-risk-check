package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.entity.PostLike;
import hanshin.home_risk_check.user.entity.User;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.repository.PostLikeRepository;
import hanshin.home_risk_check.community.repository.PostLikeRepository.PostLikeCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;

    public List<PostLike> getPostLikes(Post post){
        return postLikeRepository.findByPost(post);
    }

    public long getPostLikeCount(Post post){
        return postLikeRepository.countByPost(post);
    }

    public Map<Long, Long> getPostLikeCounts(List<Post> posts){
        if (CollectionUtils.isEmpty(posts)) {
            return Map.of();
        }
        Map<Long, Long> postLikeCounts = postLikeRepository.countByPosts(posts)
                                                           .stream()
                                                           .collect(Collectors.toMap(PostLikeCount::getPostId, PostLikeCount::getPostLikeCount));

        posts.forEach(post -> postLikeCounts.putIfAbsent(post.getId(), 0L));
        return postLikeCounts;
    }

    @Transactional
    public void create(User user, Post post) {
        if (isLikedBy(user, post)) {
            return;
        }
        PostLike postLike = PostLike.builder()
                                    .user(user)
                                    .post(post)
                                    .build();
        postLikeRepository.save(postLike);
    }

    @Transactional
    public void cancel(User user, Post post) {
        postLikeRepository.findByUserAndPost(user, post)
                          .ifPresent(postLikeRepository::delete);
    }

    @Transactional
    public boolean toggle(User user, Post post) {
        return postLikeRepository.findByUserAndPost(user, post)
                .map(postLike -> {
                    postLikeRepository.delete(postLike);
                    return false;
                })
                .orElseGet(() -> {
                    PostLike postLike = PostLike.builder()
                                                .user(user)
                                                .post(post)
                                                .build();
                    postLikeRepository.save(postLike);
                    return true;
                });
    }

    @Transactional
    public void delete(List<PostLike> postLikes){
        if(CollectionUtils.isEmpty(postLikes)){
            return;
        }
        postLikeRepository.deleteAll(postLikes);
    }

    public boolean isLikedBy(User user, Post post){
        return postLikeRepository.existsByUserAndPost(user, post);
    }
}
