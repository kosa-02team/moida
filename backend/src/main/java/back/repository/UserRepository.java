package back.repository;

import back.domain.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    long countByStatus(String status);

    Page<Users> findByRealNameContaining(String realName, Pageable pageable);

    Page<Users> findByStatus(String status, Pageable pageable);
}
