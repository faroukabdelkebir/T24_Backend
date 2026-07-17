package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * KPIs de la page « Flux » — virements en volume et en montant.
 * Les montants restent à null tant que AMOUNT_DEBITED n'est pas extraite.
 */
@Data
@Builder
public class FlowsSummaryDto {
    private long    nbOperations;
    private Double  deltaOpsPct;        // évolution vs année précédente
    private Double  montantTotal;       // null tant que AMOUNT_DEBITED absent
    private Double  deltaMontantPct;
    private Double  ticketMoyen;        // montantTotal / nbOperations
    private String  deviseDominante;    // null tant que DEBIT_CURRENCY absent
    private Integer nbDevises;
    private Integer nbTypes;            // null tant que TRANSACTION_TYPE absent
}

