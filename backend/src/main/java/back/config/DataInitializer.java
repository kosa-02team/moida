package back.config;

import back.domain.Users;
import back.domain.post.Posts;
import back.repository.UserRepository;
import back.repository.post.PostRepository;
import back.service.post.ai.PostVectorService;
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
    private final PostRepository postRepository;
    private final PostVectorService postVectorService;

    @Bean
    public CommandLineRunner initAdminAccount(DemoVectorInitializer demoVectorInitializer) {
        return args -> {
            if (!userRepository.existsByLoginId("admin@admin.com")) {
                Users admin = new Users("admin@admin.com", passwordEncoder.encode("admin1234"), "관리자");
                admin.changeSystemRole("ADMIN");
                userRepository.save(admin);
                System.out.println("초기 관리자 계정이 생성되었습니다. (ID: admin / PW: admin1234)");
            }

            demoVectorInitializer.initVectors();


        };
    }

}

