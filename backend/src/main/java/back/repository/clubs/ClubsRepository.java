package back.repository.clubs;

import back.domain.Clubs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubsRepository extends JpaRepository<Clubs, Long> {
    long countByStatus(String status);

    Page<Clubs> findByStatus(String status, Pageable pageable);

    Page<Clubs> findByNameContaining(String name, Pageable pageable);
}
