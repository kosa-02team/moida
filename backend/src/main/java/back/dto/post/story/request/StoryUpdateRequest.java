package back.dto.post.story.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

//일정 수정 불가
public record StoryUpdateRequest(

        // null이면 변경 안 함, 값이 있으면 검증 적용
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

        // create와 동일하게 검증하되, null이면 변경 안 함
        @Size(max = 999, message = "태그 멤버는 최대 999명까지 가능합니다.")
        List<
                @NotNull(message = "taggedMemberIds에는 null이 올 수 없습니다.")
                @Positive(message = "taggedMemberIds는 양수여야 합니다.")
                        Long
                > taggedMemberIds
) {
    // 선택: 아무 것도 안 바꾸는 요청 방지용(원하시면)
    public boolean hasAnyUpdate() {
        return content != null || imagesUrl != null || place != null || taggedMemberIds != null;
    }
}
