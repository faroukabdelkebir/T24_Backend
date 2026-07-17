package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/** Tranche d'une répartition de flux (par devise, par type d'opération). */
@Data
@Builder
public class FlowSliceDto {
    private String label;
    private long   nbOperations;
    private Double montantTotal;   // null tant que AMOUNT_DEBITED absent
    private Double ticketMoyen;
    private double pct;            // part en % (du montant si dispo, sinon du volume)
}