package back.config.security;

import back.domain.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;
    private LocalDateTime expireTime;
    @ManyToOne
    Users user;

    public RefreshToken(String token, Users user) {
        this.token = token;
        this.user = user;
        this.expireTime = LocalDateTime.now().plusDays(14);
    }
}
