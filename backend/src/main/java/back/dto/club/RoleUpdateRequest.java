package back.dto.club;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RoleUpdateRequest {
    
    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "OWNER|ACCOUNTANT|STAFF|MEMBER", message = "유효하지 않은 역할입니다.")
    private String role;
}
