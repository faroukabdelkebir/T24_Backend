package wevioo.se.userms.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import wevioo.se.userms.Entities.Role;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateUserRequest(

        @Email(message = "Must be a valid email address")
        @Size(max = 150)
        String email,

        @Size(min = 8, max = 128)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String password,

        @Size(max = 80)
        String firstName,

        @Size(max = 80)
        String lastName,

        Role role,

        Boolean active

) {}
