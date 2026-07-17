package wevioo.se.authms.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import java.nio.charset.StandardCharsets;

import java.net.URI;

@Service
public class ProxyService {

    private final WebClient webClient;
    private final OAuth2AuthorizedClientManager clientManager;

    public ProxyService(WebClient proxyWebClient, OAuth2AuthorizedClientManager clientManager) {
        this.webClient = proxyWebClient;
        this.clientManager = clientManager;
    }

    /**
     * Rejoue la requête entrante vers targetBase + targetPath avec le token
     * machine 'frontend-bff'. Statut, corps et content-type du service cible
     * sont retransmis tels quels (y compris les erreurs 4xx/5xx).
     */
    public ResponseEntity<byte[]> forward(HttpServletRequest request,
                                          String targetBase,
                                          String targetPath,
                                          byte[] body) {
        String query = request.getQueryString();
        URI uri = URI.create(targetBase + targetPath + (query != null ? "?" + query : ""));

        try {
            WebClient.RequestBodySpec spec = webClient
                    .method(HttpMethod.valueOf(request.getMethod()))
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToken());

            if (request.getContentType() != null) {
                spec = (WebClient.RequestBodySpec) spec.header(HttpHeaders.CONTENT_TYPE, request.getContentType());
            }

            ResponseEntity<byte[]> upstream =
                    (body != null && body.length > 0 ? spec.bodyValue(body) : spec)
                            .exchangeToMono(resp -> resp.toEntity(byte[].class))
                            .block();

            // NE PAS recopier les headers amont (Transfer-Encoding, Content-Length...)
            var out = ResponseEntity.status(upstream.getStatusCode());
            MediaType ct = upstream.getHeaders().getContentType();
            if (ct != null) out.contentType(ct);
            return out.body(upstream.getBody());

        } catch (WebClientRequestException e) {
            // Service cible éteint / injoignable → 503 propre au lieu d'un 500 opaque
            String msg = "{\"message\":\"Service cible injoignable : " + uri.getHost() + ":" + uri.getPort() + "\"}";
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String machineToken() {
        OAuth2AuthorizedClient client = clientManager.authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId("frontend-bff")
                        .principal("bff-internal")
                        .build());
        if (client == null) {
            throw new IllegalStateException("Impossible d'obtenir le token machine frontend-bff");
        }
        return client.getAccessToken().getTokenValue();
    }
}