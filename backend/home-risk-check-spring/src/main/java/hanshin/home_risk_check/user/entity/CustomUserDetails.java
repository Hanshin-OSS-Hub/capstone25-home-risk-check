package hanshin.home_risk_check.user.entity;

import hanshin.home_risk_check.file.entity.ImageFile;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();

        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return user.getRole().getValue();
            }
        });
        return collection;
    }

    @Override
    @NonNull
    public String getUsername() { return user.getEmail(); }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    public User getUser() { return user; }
    public Long getUserId() { return user.getId(); }
    public String getEmail() { return user.getEmail(); }
    public String getNickname() { return user.getNickname(); }
    public ImageFile getProfileImageFile() { return user.getProfileImageFile(); }
    public String getRole() { return user.getRole().getValue(); }
}