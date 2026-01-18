package back.repository;

import back.domain.Users;
import java.util.Optional;
import back.config.security.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(Users user);
    void deleteByUser(Users user);
}
