package hanshin.home_risk_check.user.service;

import hanshin.home_risk_check.user.entity.CustomUserDetails;
import hanshin.home_risk_check.user.entity.User;
import hanshin.home_risk_check.user.repository.UserRepository;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@Nonnull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                                  .orElseThrow(() -> new UsernameNotFoundException("유저 없음: " + email));
        return new CustomUserDetails(user);
    }
}