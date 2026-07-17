package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AlertDto {
    private String alertKey;
    private String        type;        // fallback brut (déprécié)
    private String        typeKey;     // clé i18n du type
    private String        severity;    // code : CRITICAL | WARNING | INFO
    private String        jobName;
    private String        message;     // fallback brut (déprécié)
    private String        messageKey;  // clé i18n du message
    private Map<String, Object> params; // paramètres d'interpolation du message
    private LocalDateTime triggeredAt;
    private boolean       resolved;
}
