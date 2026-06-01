package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.PostPlaceRequest;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostPlace;
import hanshin.home_risk_check.community.repository.PostPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostPlaceService {

    private final PostPlaceRepository postPlaceRepository;

    //게시글 장소 생성
    @Transactional
    public PostPlace createPostPlace(Post post, PostPlaceRequest req){
        if(req == null){
            return null;
        }

        PostPlace postPlace = PostPlace.builder()
                                       .latitude(req.latitude())
                                       .longitude(req.longitude())
                                       .address(req.address())
                                       .placeName(req.placeName())
                                       .post(post)
                                       .build();
        return postPlaceRepository.save(postPlace);
    }

    public PostPlace getPostPlace(Post post){
        return postPlaceRepository.findByPost(post).orElse(null);
    }

    //게시글 장소 수정
    @Transactional
    public PostPlace updatePostPlace(Post post, PostPlace postPlace, PostPlaceRequest req){
        //기존 장소가 존재하는데, 수정 요청이 없는 경우 -> 장소 삭제
        if(req == null){
            delete(postPlace);
            return null;
        }

        //기존 장소가 없는 경우 -> 새로 생성
        if (postPlace == null) {
            return createPostPlace(post, req);
        }

        //기존 장소가 존재하는데, 수정 요청이 있는 경우 -> 기존 장소 업데이트
        postPlace.update(req.latitude(), req.longitude(), req.address(), req.placeName());
        return postPlace;
    }

    //게시글 장소 삭제
    @Transactional
    public void delete(PostPlace postPlace){
        if (postPlace == null) {
            return;
        }
        postPlaceRepository.delete(postPlace);
    }
}