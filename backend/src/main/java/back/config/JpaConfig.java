package back.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "back")
@EntityScan(basePackages = "back")
@DependsOn("flyway")
public class JpaConfig {
    // EntityManagerFactory는 Flyway가 완료된 후 초기화되도록 설정
}
