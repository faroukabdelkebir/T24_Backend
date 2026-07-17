package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/** Point mensuel : volumes et montants de l'année N et N-1. */
@Data
@Builder
public class MonthlyFlowDto {
    private int    mois;             // 1..12
    private long   nbOperations;     // année sélectionnée
    private Long   nbOperationsN1;   // même mois, année précédente (null si absent)
    private Double montantTotal;     // null tant que AMOUNT_DEBITED absent
    private Double montantTotalN1;
}

