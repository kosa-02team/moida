package back.config;

import back.domain.Users;
import back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdminAccount() {
        return args -> {
            if (!userRepository.existsByLoginId("admin@admin.com")) {
                Users admin = new Users("admin@admin.com", passwordEncoder.encode("admin1234"), "관리자");
                admin.changeSystemRole("ADMIN");
                userRepository.save(admin);
                System.out.println("초기 관리자 계정이 생성되었습니다. (ID: admin / PW: admin1234)");
            }
        };
    }
}
