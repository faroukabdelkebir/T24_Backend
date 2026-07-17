package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

/** Tranche d'une répartition (pyramide des âges, secteurs, devises…). */
@Data
@Builder
public class BreakdownSliceDto {
    private String label;
    private long   value;
    private double pct;
}
