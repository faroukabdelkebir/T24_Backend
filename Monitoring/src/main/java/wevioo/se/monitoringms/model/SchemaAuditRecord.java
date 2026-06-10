package wevioo.se.monitoringms.model;

import lombok.Builder;
import lombok.Data;

/**
 * Mappe SCHEMA_SOURCE_AUDIT — historique des modifications de schéma.
 * Alimenté automatiquement par le trigger SQL Server `schemaSourceTriggerAudit`.
 */
@Data
@Builder
public class SchemaAuditRecord {

    private int     id;
    private String  sourceId;
    private String  sourceTableName;
    private String  sourceColumnName;
    private String  sourceDataType;
    private int     newSourceEnabled;
    private int     oldSourceEnabled;
    private int     newSourceIndexed;
    private int     oldSourceIndexed;
    private String  updatedBy;
    private Long    updatedOn;

    public boolean wasActivated()   { return oldSourceEnabled == 0 && newSourceEnabled == 1; }
    public boolean wasDeactivated() { return oldSourceEnabled == 1 && newSourceEnabled == 0; }
    public boolean wasIndexed()     { return oldSourceIndexed == 0 && newSourceIndexed == 1; }
}
