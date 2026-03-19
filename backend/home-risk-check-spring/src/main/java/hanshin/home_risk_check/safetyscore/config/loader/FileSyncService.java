package hanshin.home_risk_check.safetyscore.config.loader;

import hanshin.home_risk_check.safetyscore.domain.sync.entity.FileSyncHistory;
import hanshin.home_risk_check.safetyscore.domain.sync.repository.FileSyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileSyncService {

    private final FileSyncHistoryRepository fileSyncHistoryRepository;

    // 파일의 내용을 추출하여 암호화하여 저장
    public String calculateHash(Resource resource){
        try (InputStream is = resource.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesBuffer = new byte[1024]; // 서버에 부담안가게 1024바이트씩 저장
            int bytesRead;

            while((bytesRead = is.read(bytesBuffer)) != -1) { //저장되어있는 데이터 크기 반환,  -1(파일의 끝) 이 될때까지 반복, is 가 1024바이트 다음부터 알아서 가져와줌
                digest.update(bytesBuffer, 0, bytesRead); //
            }
            byte[] hashedBytes = digest.digest();

            // 바이트를 16진수 문자열로 변환
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();


        } catch (Exception e) {
            throw new RuntimeException("파일 해시 계산 중 오류 발생: " + resource.getFilename(), e);
        }

    }

    // 파일이 변경 되었는지 확인
    public boolean isChanged (String fileName, String currentHash){
        return fileSyncHistoryRepository.findByFileName(fileName)
                .map(history -> !history.getFileHash().equals(currentHash)) //지문의 내용을 비교하여 다르면 true, 같으면 false 반환
                .orElse(true); //DB에 예전기록이 없으면 새로운 파일로 간주
    }

    // DB 갱신하기
    @Transactional
    public void updateSyncHistory(String fileName, String newHash) {
        FileSyncHistory history = fileSyncHistoryRepository.findByFileName(fileName)
                .orElse(new FileSyncHistory(fileName, newHash));

        history.updateHash(newHash); // 바뀐 지문의 내용 저장
        fileSyncHistoryRepository.save(history);
        log.debug("{} 파일 동기화 완료 및 내용 갱신", fileName);
    }
}
