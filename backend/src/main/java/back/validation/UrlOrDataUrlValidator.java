package back.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlOrDataUrlValidator implements ConstraintValidator<UrlOrDataUrl, String> {

    @Override
    public void initialize(UrlOrDataUrl constraintAnnotation) {
        // 초기화 로직 없음
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // null이나 빈 값은 다른 검증(@NotBlank 등)에서 처리
        }
        
        // data: URL 형식인지 확인 (data:image/... 형식)
        if (value.startsWith("data:image/")) {
            return true;
        }
        
        // 일반 URL 검증
        try {
            new URL(value);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
