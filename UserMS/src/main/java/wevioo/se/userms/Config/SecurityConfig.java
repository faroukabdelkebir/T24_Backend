package wevioo.se.userms.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Endpoints internes (hashes de mots de passe) : réservés au
                        // client OAuth2 "auth-service" via ses scopes dédiés.
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/internal/**")
                        .hasAuthority("SCOPE_user:credentials:read")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/internal/**")
                        .hasAuthority("SCOPE_user:credentials:write")

                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyAuthority(
                                "SCOPE_user:read", "SCOPE_user:write"
                        )
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users/**").hasAuthority("SCOPE_user:write")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/users/**").hasAuthority("SCOPE_user:write")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/**").hasAuthority("SCOPE_user:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasAuthority("SCOPE_user:write")
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        ));

        return http.build();
    }



    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        converter.setPrincipalClaimName("client_id");
        return converter;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Stream<GrantedAuthority> scopeAuthorities = extractScopes(jwt)
                .stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope));

        Stream<GrantedAuthority> roleAuthorities = extractRoles(jwt)
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role));

        return Stream.concat(scopeAuthorities, roleAuthorities).toList();
    }

    private List<String> extractRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }

        return Optional.ofNullable(jwt.getClaimAsString("role"))
                .filter(role -> !role.isBlank())
                .map(List::of)
                .orElseGet(Collections::emptyList);
    }

    private List<String> extractScopes(Jwt jwt) {
        List<String> scopeList = jwt.getClaimAsStringList("scope");
        if (scopeList != null && !scopeList.isEmpty()) {
            return scopeList;
        }
        String scopeString = jwt.getClaimAsString("scope");
        if (scopeString != null && !scopeString.isBlank()) {
            return List.of(scopeString.split(" "));
        }
        List<String> scpList = jwt.getClaimAsStringList("scp");
        return scpList != null ? scpList : Collections.emptyList();
    }
}