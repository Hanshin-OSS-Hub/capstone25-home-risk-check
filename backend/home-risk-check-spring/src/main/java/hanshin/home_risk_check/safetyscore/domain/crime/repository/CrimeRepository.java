package hanshin.home_risk_check.safetyscore.domain.crime.repository;


import hanshin.home_risk_check.safetyscore.domain.crime.entity.CrimeStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrimeRepository extends JpaRepository<CrimeStat, Long> {
}
