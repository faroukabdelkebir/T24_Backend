package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/** Performance d'un gestionnaire de portefeuille (ACCOUNT_OFFICER). */
@Data
@Builder
public class OfficerPerfDto {
    private String officer;
    private long   nbClients;
    private Double ageMoyen;
    private double partPct;     // part du portefeuille total en %
}

