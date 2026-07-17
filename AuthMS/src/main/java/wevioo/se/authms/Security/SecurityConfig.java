package wevioo.se.authms.Security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.util.StringUtils;
import wevioo.se.authms.Config.AppProperties;
import wevioo.se.authms.Security.JwtAuthenticationFilter;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ResourceLoader resourceLoader;


    @Value("${app.oauth2.clients.etl.secret}")
    private String etlClientSecret;

    @Value("${app.oauth2.clients.bff.secret}")
    private String bffClientSecret;

    @Value("${app.oauth2.clients.auth.secret}")
    private String authServiceClientSecret;

    @Value("${app.oauth2.jwk.keystore-path:}")
    private String jwkKeystorePath;

    @Value("${app.oauth2.jwk.keystore-password:}")
    private String jwkKeystorePassword;

    @Value("${app.oauth2.jwk.key-alias:}")
    private String jwkKeyAlias;


    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**", "/bff/**", "/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST, "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        .requestMatchers("/bff/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/bff/ai/api/m1/audit/**",
                                "/bff/ai/api/m1/audit-all/**",
                                "/bff/ai/api/m3/scheduler/recommend/**").hasAnyRole("ADMIN", "MONITOR")
                        .requestMatchers("/bff/ai/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/bff/batch/schema-source/**").hasAnyRole("ADMIN", "MONITOR")
                        .requestMatchers("/bff/batch/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/logout",
                                "/api/v1/auth/logout-all",
                                "/api/v1/auth/change-password"
                        ).authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()

                        .requestMatchers("/api/v1/auth/audit/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain oauth2ServerFilterChain(HttpSecurity http) throws Exception {

        http.with(new OAuth2AuthorizationServerConfigurer(), configurer ->
                configurer.oidc(Customizer.withDefaults())
        );

        http
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        return http.build();
    }


    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbcTemplate,
            TokenSettings defaultTokenSettings,
            PasswordEncoder passwordEncoder) {

        JdbcRegisteredClientRepository repo = new JdbcRegisteredClientRepository(jdbcTemplate);

        seedIfAbsent(repo, buildEtlClient(defaultTokenSettings, passwordEncoder));
        seedIfAbsent(repo, buildFrontendBffClient(defaultTokenSettings, passwordEncoder));
        seedIfAbsent(repo, buildAuthServiceClient(defaultTokenSettings, passwordEncoder));

        return repo;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository repo) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, repo);
    }

    @Bean
    public TokenSettings defaultTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .reuseRefreshTokens(false)
                .build();
    }


    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AppProperties props) {
        return AuthorizationServerSettings.builder()
                .issuer(props.getOauth2().getIssuer())
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = StringUtils.hasText(jwkKeystorePath)
                ? loadRsaKeyFromKeystore()
                : generateEphemeralRsaKey();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private RSAKey loadRsaKeyFromKeystore() {
        try {
            Resource resource = resourceLoader.getResource(jwkKeystorePath);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            char[] password = jwkKeystorePassword.toCharArray();
            try (InputStream is = resource.getInputStream()) {
                keyStore.load(is, password);
            }
            RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(jwkKeyAlias, password);
            RSAPublicKey publicKey =
                    (RSAPublicKey) keyStore.getCertificate(jwkKeyAlias).getPublicKey();

            log.info("Loaded RSA signing key from keystore (alias={})", jwkKeyAlias);
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(jwkKeyAlias)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load RSA key from keystore at " + jwkKeystorePath, ex);
        }
    }

    private RSAKey generateEphemeralRsaKey() {
        log.warn("No JWK keystore configured (app.oauth2.jwk.keystore-path is empty). "
                + "Generating an EPHEMERAL RSA key: issued tokens will NOT survive a restart "
                + "and are invalid across instances. Configure a keystore before production.");
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey  publicKey  = (RSAPublicKey)  keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }


    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (context.getAuthorizedScopes() != null) {
                context.getClaims().claim("scope",    context.getAuthorizedScopes());
                context.getClaims().claim("client_id", context.getRegisteredClient().getClientId());
            }
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


    private RegisteredClient buildEtlClient(TokenSettings ts, PasswordEncoder passwordEncoder) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("etl-service")
                .clientSecret(passwordEncoder.encode(etlClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("user:read")
                .scope("user:write")
                .scope("monitoring:read")
                .tokenSettings(ts)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();
    }

    private RegisteredClient buildFrontendBffClient(TokenSettings ts, PasswordEncoder passwordEncoder) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("frontend-bff")
                .clientSecret(passwordEncoder.encode(bffClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("user:read")
                .scope("user:write")
                .scope("monitoring:read")
                .scope("batch:execute")
                .scope("ai:read")
                .scope("ai:execute")
                .tokenSettings(ts)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();
    }

    /**
     * AuthMS lui-même est enregistré comme client OAuth2 (flow client_credentials)
     * pour ses appels service-à-service vers UserMS. Les scopes user:credentials:*
     * sont réservés à ce client : ni l'ETL ni le BFF ne peuvent lire des hashes
     * de mots de passe.
     */
    private RegisteredClient buildAuthServiceClient(TokenSettings ts, PasswordEncoder passwordEncoder) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("auth-service")
                .clientSecret(passwordEncoder.encode(authServiceClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("user:credentials:read")
                .scope("user:credentials:write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .build())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();
    }

    private void seedIfAbsent(JdbcRegisteredClientRepository repo, RegisteredClient client) {
        if (repo.findByClientId(client.getClientId()) == null) {
            repo.save(client);
        }
    }

    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
