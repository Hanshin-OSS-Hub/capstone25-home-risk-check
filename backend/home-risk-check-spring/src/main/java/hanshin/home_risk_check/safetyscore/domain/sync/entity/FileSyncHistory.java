package hanshin.home_risk_check.safetyscore.domain.sync.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file_sync_history")
public class FileSyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String fileName; // 예: "accident_hotspots.csv"

    @Column(nullable = false)
    private String fileHash; // 예: "a2b4c6d8..." (SHA-256 지문)

    private LocalDateTime lastUpdated;

    public FileSyncHistory(String fileName, String fileHash) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.lastUpdated = LocalDateTime.now();
    }

    public void updateHash(String newHash) {
        this.fileHash = newHash;
        this.lastUpdated = LocalDateTime.now();
    }
}