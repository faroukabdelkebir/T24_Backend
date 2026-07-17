package wevioo.se.authms.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff")
public class ProxyController {

    private final ProxyService proxy;

    @Value("${app.bff.targets.userms}")
    private String usermsUrl;

    @Value("${app.bff.targets.aiagents}")
    private String aiagentsUrl;

    @Value("${app.bff.targets.extractor}")
    private String extractorUrl;

    public ProxyController(ProxyService proxy) {
        this.proxy = proxy;
    }

    /** /users/**  →  UserMS /api/v1/users/** */
    @RequestMapping("/users/**")
    public ResponseEntity<byte[]> users(HttpServletRequest req,
                                        @RequestBody(required = false) byte[] body) {
        return proxy.forward(req, usermsUrl, "/api/v1/users" + suffix(req, "/bff/users"), body);
    }

    /** /ai/**  →  AI Agents /** (chemins Django conservés, slash final inclus) */
    @RequestMapping("/ai/**")
    public ResponseEntity<byte[]> ai(HttpServletRequest req,
                                     @RequestBody(required = false) byte[] body) {
        return proxy.forward(req, aiagentsUrl, suffix(req, "/bff/ai"), body);
    }

    /** /batch/**  →  Extracteur /api/v1/** */
    @RequestMapping("/batch/**")
    public ResponseEntity<byte[]> batch(HttpServletRequest req,
                                        @RequestBody(required = false) byte[] body) {
        return proxy.forward(req, extractorUrl, "/api/v1" + suffix(req, "/bff/batch"), body);
    }

    private String suffix(HttpServletRequest req, String prefix) {
        String path = req.getRequestURI();
        return path.length() > prefix.length() ? path.substring(prefix.length()) : "";
    }
}
