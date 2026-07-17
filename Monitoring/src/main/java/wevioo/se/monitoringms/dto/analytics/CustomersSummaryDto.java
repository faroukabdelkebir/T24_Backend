package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * KPIs de la page « Clients » — démographie et acquisition.
 * Les champs nullables restent à null tant que la colonne source
 * n'a pas été activée dans SCHEMA_SOURCE (extraction enrichie).
 */
@Data
@Builder
public class CustomersSummaryDto {
    private long   totalClients;
    private Double ageMoyen;
    private long   nbGestionnaires;
    private Double pctMigration;       // % de dossiers issus de la migration initiale (AUTHORISER)
    private Long   nouveauxClients;    // année sélectionnée — null tant que DATE_AC_OPND absente
    private Double deltaNouveauxPct;   // évolution vs année précédente
    private Double pctFemmes;          // null tant que GENDER absente
    private Integer nbNationalites;    // null tant que NATIONALITY absente
    private Integer nbSecteurs;        // null tant que SECTOR absente
}

