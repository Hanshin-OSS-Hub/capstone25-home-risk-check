package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.crime.repository;

import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.crime.entity.CrimeStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrimeRepository extends JpaRepository<CrimeStat, Long> {
}
