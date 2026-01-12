package back.dto.posts.story.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record StoryCreateRequest(

        // 일정 연결(선택). 있으면 일정 스토리, 없으면 일반 스토리
        @Positive(message = "scheduleId는 양수여야 합니다.")
        Long scheduleId,

        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 500, message = "content는 최대 500자입니다.")
        String content,

        @Size(max = 9, message = "이미지는 최대 9장까지 가능합니다.")
        List<
                @NotBlank(message = "imagesUrl에는 빈 값이 올 수 없습니다.")
                @URL(message = "imagesUrl은 올바른 URL 형식이어야 합니다.")
                        String
                > imagesUrl,

        @Size(max = 100, message = "place는 최대 100자입니다.")
        String place,

        @Size(max = 50, message = "태그 멤버는 최대 999명까지 가능합니다.")
        List<
                @NotNull(message = "taggedMemberIds에는 null이 올 수 없습니다.")
                @Positive(message = "taggedMemberIds는 양수여야 합니다.")
                        Long
                > taggedMemberIds
) {}