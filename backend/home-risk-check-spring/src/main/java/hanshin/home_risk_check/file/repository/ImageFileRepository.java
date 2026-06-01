package hanshin.home_risk_check.file.repository;

import hanshin.home_risk_check.file.entity.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {}
