package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/** Cellule de la heatmap d'activité : mois × jour de semaine. */
@Data
@Builder
public class HeatmapCellDto {
    private int  mois;          // 1..12
    private int  jourSemaine;   // 0 = lundi … 6 = dimanche
    private long nb;
}
