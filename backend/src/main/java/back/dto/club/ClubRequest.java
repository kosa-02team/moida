package back.dto.club;

import back.domain.club.Clubs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubRequest {

    @NotBlank(message = "모임 이름은 필수입니다.")
    @Size(min = 2, max = 20, message = "모임 이름은 2자 이상 20자 이하여야 합니다.")
    private String clubName;

    @Pattern(regexp = "PUBLIC|PRIVATE", message = "운영 설정이 공개인지 비공개인지 선택해주세요. (선택 안할 시 공개로 설정됩니다.)")
    private String visibility;

    @Pattern(regexp = "OPERATION_FEE|FAIR_SETTLEMENT", message = "유형은 운영비형 또는 공정정산형 중 선택해주세요. (선택 안할 시 운영비형으로 설정됩니다.)")
    private String type;

    @jakarta.validation.constraints.Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
    @jakarta.validation.constraints.Max(value = 999, message = "최대 인원은 1000명 미만이어야 합니다.")
    private Integer maxMembers;

    @Pattern(regexp = "STUDY|SPORTS|SOCIAL|HOBBY|FINANCE|ETC", message = "카테고리는 STUDY, SPORTS, SOCIAL, HOBBY, FINANCE, ETC 중 선택해주세요. (선택 안할 시 ETC로 설정됩니다.)")
    private String category;

    public Clubs.Visibility getVisibilityEnum() {
        if (visibility == null || visibility.isEmpty()) {
            return Clubs.Visibility.PUBLIC;
        }
        return Clubs.Visibility.valueOf(visibility);
    }

    public Clubs.Type getTypeEnum() {
        if (type == null || type.isEmpty()) {
            return Clubs.Type.OPERATION_FEE;
        }
        return Clubs.Type.valueOf(type);
    }

    public Clubs.Category getCategoryEnum() {
        if (category == null || category.isEmpty()) {
            return Clubs.Category.ETC;
        }
        return Clubs.Category.valueOf(category);
    }
}
