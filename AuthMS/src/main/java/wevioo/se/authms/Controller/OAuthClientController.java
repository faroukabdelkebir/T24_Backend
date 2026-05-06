package wevioo.se.authms.Controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/v1/oauth/clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OAuthClientController {

    private final RegisteredClientRepository clientRepo;
    private final PasswordEncoder            passwordEncoder;


    @PostMapping
    public ResponseEntity<ClientRegistrationResponse> registerClient(
            @Valid @RequestBody ClientRegistrationRequest request) {

        log.info("Registering new OAuth2 client: {}", request.clientId());

        String rawSecret = UUID.randomUUID().toString().replace("-", "");

        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.clientId())
                .clientSecret(passwordEncoder.encode(rawSecret))
                .clientName(request.clientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scopes(scopes -> scopes.addAll(request.scopes()))
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(
                                Duration.ofSeconds(request.tokenTtlSeconds() > 0
                                        ? request.tokenTtlSeconds() : 3600))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .build();

        clientRepo.save(client);
        log.info("OAuth2 client registered: clientId={}", request.clientId());

        return ResponseEntity.ok(new ClientRegistrationResponse(
                client.getId(),
                client.getClientId(),
                rawSecret,
                "Client registered. Save the client_secret — it will not be shown again."
        ));
    }


    @GetMapping("/{clientId}")
    public ResponseEntity<ClientInfoResponse> getClient(@PathVariable String clientId) {
        RegisteredClient client = clientRepo.findByClientId(clientId);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ClientInfoResponse(
                client.getId(),
                client.getClientId(),
                client.getClientName(),
                client.getScopes().stream().toList(),
                client.getClientIdIssuedAt()
        ));
    }


    public record ClientRegistrationRequest(
            @NotBlank String       clientId,
            @NotBlank String       clientName,
            @NotEmpty List<String> scopes,
            long                   tokenTtlSeconds
    ) {}

    public record ClientRegistrationResponse(
            String id,
            String clientId,
            String clientSecret,
            String message
    ) {}

    public record ClientInfoResponse(
            String              id,
            String              clientId,
            String              clientName,
            List<String>        scopes,
            java.time.Instant   issuedAt
    ) {}
}

