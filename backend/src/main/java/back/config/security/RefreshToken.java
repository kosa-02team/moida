package back.config.security;

import back.domain.Users;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    String token;

    LocalDateTime expireTime;

    @ManyToOne
    Users user;

    public RefreshToken(String token, Users user) {
        this.token = token;
        this.user = user;
        this.expireTime = LocalDateTime.now().plusDays(14);
    }
}
