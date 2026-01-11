package back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email //아이디를 이메일 형식으로 받음 (스토리 보드  ui 참고)
        @NotBlank
        String loginId,

        @Size(min = 8) //최소 8자 이상
        @NotBlank
        String password,

        @Pattern(regexp = "^[가-힣]{3}$") //3글자로 이루어진 한글 , 즉 이름만 받음
        String realName) {

}
