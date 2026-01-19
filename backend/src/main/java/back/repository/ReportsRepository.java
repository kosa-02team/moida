package back.repository;

import back.domain.Reports;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportsRepository extends JpaRepository<Reports, Long> {
    long countByStatus(String status);

    Page<Reports> findByStatus(String status, Pageable pageable);

    // For simplicity, we'll support basic filtering by status.
    // If "All" is selected in UI, service handles calling findAll(pageable).
    // More complex dynamic query (criteria/specification) is better but keeping it
    // simple for now as requested.
}
