package wevioo.se.authms.Services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import wevioo.se.authms.Entities.AuthAuditLog;
import wevioo.se.authms.Entities.AuthEventType;
import wevioo.se.authms.Repositories.AuthAuditLogRepository;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuthAuditLogRepository auditRepo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuthEventType eventType,
                    String username,
                    boolean success,
                    String detail,
                    HttpServletRequest request) {

        AuthAuditLog entry = AuthAuditLog.builder()
                .eventType(eventType)
                .username(username)
                .ipAddress(extractIp(request))
                .userAgent(truncate(request.getHeader("User-Agent"), 500))
                .success(success)
                .detail(truncate(detail, 500))
                .build();

        auditRepo.save(entry);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuthEventType eventType, String username, boolean success, String detail) {
        AuthAuditLog entry = AuthAuditLog.builder()
                .eventType(eventType)
                .username(username)
                .success(success)
                .detail(truncate(detail, 500))
                .build();
        auditRepo.save(entry);
    }


    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

