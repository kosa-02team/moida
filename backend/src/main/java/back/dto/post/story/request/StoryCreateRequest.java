package back.dto.post.story.request;

import back.dto.vote.VoteOptionCreateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.List;

public record StoryCreateRequest(

        // 일정 연결(선택). 있으면 일정 스토리, 없으면 일반 스토리
        @Positive(message = "scheduleId는 양수여야 합니다.") Long scheduleId,

        // 일반 게시글일 때 사용 (투표일 때는 null 가능)
        @Size(max = 500, message = "content는 최대 500자입니다.") String content,

        // 투표 게시글일 때 사용 (제목)
        @Size(max = 200, message = "title은 최대 200자입니다.") String title,

        @Size(max = 9, message = "이미지는 최대 9장까지 가능합니다.") List<@NotBlank(message = "imagesUrl에는 빈 값이 올 수 없습니다.") String> imagesUrl,

        @Size(max = 100, message = "place는 최대 100자입니다.") String place,

        @Size(max = 50, message = "태그 멤버는 최대 999명까지 가능합니다.") List<@NotNull(message = "taggedMemberIds에는 null이 올 수 없습니다.") @Positive(message = "taggedMemberIds는 양수여야 합니다.") Long> taggedMemberIds,

        // 투표 관련 필드 (투표 게시글 생성 시 사용)
        List<VoteOptionCreateRequest> voteOptions,
        LocalDateTime voteDeadline,
        Boolean isAnonymous,
        Boolean allowMultiple) {
}