package back.dto.admin;

import back.domain.Users;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUserResponse {
    private Long userId;
    private String loginId;
    private String realName;
    private String systemRole;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime bannedAt;

    public static AdminUserResponse from(Users user) {
        return AdminUserResponse.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .realName(user.getRealName())
                .systemRole(user.getSystemRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .bannedAt(user.getBannedAt())
                .build();
    }
}
