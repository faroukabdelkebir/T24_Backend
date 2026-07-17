package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * Indicateurs métier agrégés — bandeau KPI de la page Analytics.
 * Les champs nullables restent à null tant que la colonne source
 * n'a pas été activée dans SCHEMA_SOURCE (extraction enrichie).
 */
@Data
@Builder
public class AnalyticsSummaryDto {
    private long    totalClients;
    private Double  ageMoyen;
    private long    nbGestionnaires;
    private long    nbVirements;          // fenêtre = année sélectionnée (ou tout)
    private Double  deltaVirementsPct;    // évolution vs année précédente, null si N/A
    private long    nbFactures;
    private Double  deltaFacturesPct;
    private Double  montantVirements;     // null tant que AMOUNT_DEBITED absent
    private String  devisePrincipale;     // null tant que DEBIT_CURRENCY absent
}


