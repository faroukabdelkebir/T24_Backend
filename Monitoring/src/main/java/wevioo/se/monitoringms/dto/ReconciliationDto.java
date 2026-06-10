package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReconciliationDto {

    private String  tableName;
    private int     businessDate;
    private int     oracleCount;
    private int     sqlServerCount;
    private int     delta;              // oracleCount - sqlServerCount
    private double  reconciliationRate; // % d'items correctement chargés
    private boolean reconciled;         // true si dans la tolérance configurée
    private String  status;
}
