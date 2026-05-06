package wevioo.se.authms.Config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Oauth2 oauth2 = new Oauth2();
    private final ResetPassword resetPassword = new ResetPassword();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank
        private String secret;
        @Positive
        private long accessTokenExpirationMs  = 3_600_000L;
        @Positive
        private long refreshTokenExpirationMs = 604_800_000L;
    }

    @Getter
    @Setter
    public static class Oauth2 {
        @NotBlank
        private String issuer = "http://localhost:8090";
    }

    @Getter
    @Setter
    public static class ResetPassword {
        @Positive
        private int tokenExpirationMinutes = 15;
        @NotBlank
        private String baseUrl = "http://localhost:4200";
    }
}