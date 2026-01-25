package back.config;

import back.domain.Users;
import back.domain.post.Posts;
import back.dto.post.story.request.StoryCreateRequest;
import back.repository.UserRepository;
import back.repository.post.PostRepository;
import back.service.post.PostService;
import back.service.post.ai.PostVectorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostService postService;
    private final PostRepository postRepository;

    @Bean
    public CommandLineRunner initAdminAccount(DemoVectorInitializer demoVectorInitializer) {
        return args -> {
            if (!userRepository.existsByLoginId("admin@admin.com")) {
                Users admin = new Users("admin@admin.com", passwordEncoder.encode("admin1234"), "관리자");
                admin.changeSystemRole("ADMIN");
                userRepository.save(admin);
                System.out.println("초기 관리자 계정이 생성되었습니다. (ID: admin / PW: admin1234)");
            }
            if (postRepository.count() == 0) {
                initPost();
            }
        };
    }
    @Transactional
    public void initPost() {
        Long clubId = 1L;

        List<Long> writerUserIds = List.of(
                27L, 2L, 3L, 4L, 5L,
                6L, 7L, 8L, 9L, 10L
        );

        for (int i = 0; i < 10; i++) {
            StoryCreateRequest request = new StoryCreateRequest(
                    (long) (i + 1), // scheduleId
                    contents.get(i),
                    titles.get(i),
                    List.of(
                            "/uploads/images/story/" + (i + 1) + "/1.jpg",
                            "/uploads/images/story/" + (i + 1) + "/2.jpg"
                    ),
                    places.get(i),
                    taggedMembers.get(i),
                    null,
                    null,
                    null,
                    null
            );

            postService.createStory(
                    clubId,
                    writerUserIds.get(i),
                    request
            );
        }
    }

    private static final List<String> titles = List.of(
            "오늘은 여의도 공원 한 바퀴",
            "러닝 전에 커피 마시면 안 된다고",
            "비 온 뒤라 공기가 좋아서",
            "오늘은 초보자도 있어서",
            "뛰고 나서 치킨 얘기만",
            "이어폰 끼고 혼자 뛰는 사람",
            "오늘은 러닝보다 수다",
            "출발 전에 스트레칭 대충",
            "끝나고 강변에서 야경",
            "오늘은 유독 컨디션"
    );

    private static final List<String> contents = List.of(
            "오늘은 여의도 공원 한 바퀴.\n\n초반엔 다들 말 많다가 2km 지나니까 말수 급감.\n\n끝나고 편의점에서 아이스크림 하나씩 들고 앉아있는데,\n“뛰는 것보다 이 시간이 더 좋다”는 말에 다들 고개 끄덕임.",
            "러닝 전에 커피 마시면 안 된다고 했는데\n\n결국 두 명이나 아이스 아메리카노 들고 등장.\n\n뛰다가 배 아프다며 중간에 화장실 찾느라 코스 이탈.\n다음부턴 커피 금지로 합의.",
            "비 온 뒤라 공기가 좋아서 속도 욕심냈다가\n\n후반에 다 같이 페이스 무너짐.\n\n마지막 500m는 걷다 뛰다 반복.\n그래도 끝나고 사진 찍을 땐 다들 웃고 있음.",
            "오늘은 초보자도 있어서 속도 낮춰서 진행.\n\n옆에서 계속 “지금 괜찮죠?” 물어보는 배려 덕분에\n처음 나온 사람도 끝까지 완주.\n러닝은 역시 같이 해야 오래 가는 듯.",
            "뛰고 나서 치킨 얘기만 10분째.\n\n“운동했으니까 괜찮다” vs “이러면 뭐 하러 뛰냐”.\n\n결국 반반 나뉘어서 치킨 팀, 귀가 팀으로 해산.",
            "이어폰 끼고 혼자 뛰는 사람,\n끝까지 옆에서 맞춰주는 사람,\n사진 담당까지 역할이 자연스럽게 나뉨.\n\n말 안 해도 굴러가는 게 이제 팀 같다.",
            "오늘은 러닝보다 수다 비중이 더 높았던 날.\n\n속도는 느렸지만 시간은 제일 빨리 감.\n땀보다 웃음이 더 많이 난 러닝.",
            "출발 전에 스트레칭 대충 했다가\n\n첫 1km에서 다리 뻐근함 호소자 속출.\n\n다음 모임부터는 스트레칭 담당 지정하기로 결정.",
            "끝나고 강변에서 야경 보면서 잠깐 멍 때림.\n\n누가 먼저랄 것도 없이 사진 찍고 공유.\n러닝이 핑계고, 사실 이 분위기가 좋은 듯.",
            "오늘은 유독 컨디션 안 좋은 사람이 많았던 날.\n\n그래서 목표 거리 줄이고 일찍 종료.\n\n“이런 날도 있어야 오래 한다”는 말이 오늘의 명언."
    );

    private static final List<String> places = List.of(
            "여의도공원", "한강공원", "잠수교", "올림픽공원", "망원한강공원",
            "반포한강공원", "뚝섬유원지", "석촌호수", "청계천", "서울숲"
    );

    private static final List<List<Long>> taggedMembers = List.of(
            List.of(27L, 2L, 3L),
            List.of(27L, 4L, 5L),
            List.of(27L, 6L, 7L, 8L),
            List.of(27L, 9L),
            List.of(27L, 10L, 11L),
            List.of(27L, 12L, 13L),
            List.of(27L, 14L),
            List.of(27L, 2L, 5L),
            List.of(27L, 3L, 6L),
            List.of(27L, 4L, 7L)
    );


}

