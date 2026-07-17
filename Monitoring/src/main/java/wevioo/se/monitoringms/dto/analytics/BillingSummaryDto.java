package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * KPIs de la page « Créances » — facturation, encours et recouvrement.
 * Les champs nullables restent à null tant que la colonne source
 * n'a pas été activée dans SCHEMA_SOURCE (extraction enrichie).
 */
@Data
@Builder
public class BillingSummaryDto {
    private long   nbFactures;             // fenêtre = année sélectionnée (ou tout)
    private Double deltaFacturesPct;       // évolution vs année précédente
    private Double montantFacture;         // Σ OR_TOTAL_AMOUNT — null tant qu'absente
    private Double encoursTotal;           // Σ OS_TOTAL_AMOUNT — null tant qu'absente
    private Double tauxRecouvrement;       // 100 × (1 − encours / facturé)
    private Double pctPayees;              // % de factures SETTLE_STATUS = payée
    private Double delaiMoyenReglement;    // jours entre échéance et paiement effectif
}

