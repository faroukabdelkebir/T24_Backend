package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyVolumeDto {
    private String date;
    private long   rowsRead;
    private long   rowsWritten;
    private int    jobCount;
    private double successRate;
}
