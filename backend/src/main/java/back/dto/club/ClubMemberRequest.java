package back.dto.club;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubMemberRequest {
    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
    private String nickname;
}
