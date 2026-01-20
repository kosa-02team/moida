package back.dto.user;

import back.domain.Users;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long userId;
    private String loginId;
    private String realName;
    private String systemRole;
    private String status;
    private LocalDateTime createdAt;

    public static UserResponse from(Users user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .realName(user.getRealName())
                .systemRole(user.getSystemRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
