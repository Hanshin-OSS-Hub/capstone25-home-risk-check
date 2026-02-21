package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.crime.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "crime_statistics")
public class CrimeStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String regionName;

    private int homicide; //살인
    private int robbery; //강도
    private int sexualAssault; //성범죄
    private int theft; //절도
    private int violence; //폭력

    @Builder
    public CrimeStat(String regionName, int homicide, int robbery ,int sexualAssault, int theft, int violence) {
        this.regionName = regionName;
        this.homicide = homicide;
        this.robbery = robbery;
        this.sexualAssault = sexualAssault;
        this.theft = theft;
        this.violence = violence;
    }
}
