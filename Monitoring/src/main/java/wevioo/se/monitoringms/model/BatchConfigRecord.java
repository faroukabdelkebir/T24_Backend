package wevioo.se.monitoringms.model;

import lombok.Builder;
import lombok.Data;

/**
 * Mappe la table BATCH_CONFIG.
 * Configuration de chaque extraction : table cible, type de chargement, date dernier run.
 */
@Data
@Builder
public class BatchConfigRecord {

    private String  id;
    private String  tableName;
    private String  t24SelectionName;
    private String  colonneRef;
    private String  chargement;
    private Long    lastDate;
    private Long    minDate;
    private boolean enabled;
    private boolean scheduler;
    private String  schedulerTime;
}

