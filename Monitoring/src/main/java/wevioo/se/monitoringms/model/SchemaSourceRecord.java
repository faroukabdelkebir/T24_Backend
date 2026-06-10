package wevioo.se.monitoringms.model;

import lombok.Builder;
import lombok.Data;

/**
 * Mappe SCHEMA_SOURCE — dictionnaire des colonnes T24 mappées vers SQL Server.
 */
@Data
@Builder
public class SchemaSourceRecord {

    private String  id;
    private String  tableName;
    private String  columnName;
    private String  dataType;
    private boolean enabled;
    private boolean indexed;
    private String  ssTag;
    private String  singleMult;    // 'S' = single-value, 'M' = multi-value
    private boolean localRef;
    private Long    insertDate;
}
