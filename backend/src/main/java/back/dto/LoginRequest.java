package back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Email
        @NotBlank
        String loginId,

        @NotBlank
        String password) {

}
