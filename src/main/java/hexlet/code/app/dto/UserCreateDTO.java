package hexlet.code.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserCreateDTO {
    @NotNull
    private String email;

    @NotBlank
    private String firstName;

    @NotNull
    private String lastName;

    @NotBlank
    private String password;

}
