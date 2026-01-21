package back.dto.club;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(
        @NotNull(message = "새 모임장의 멤버 ID는 필수입니다.")
        Long newOwnerMemberId
) {
}
