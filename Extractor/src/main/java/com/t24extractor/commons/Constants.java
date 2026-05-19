package com.t24extractor.commons;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Constants {
    private Constants() {
        /* This utility class should not be instantiated */
    }

    /**
     * La variable ci-dessous représente les colonnes ou lesquelles leur existence dans la structure de chaque table est primordiale
     * @_id represente la clé unique génerée de T24 (RECID) sauf le nommage a été altéré pour respecter la structure du MongoDB ou la clé primaire est _id.
     * @ORIGINAL_ID est une colonne génerée de la colonne _idpour faciliter l'opération du regroupement entre les tables live & History
     * @INSERT_DATE est une colonne générée lors de l'insertion de la ligne en SQLServer.
     */
    public static final String ORIGINAL_ID = "ORIGINAL_ID";
    public static final String INSERT_DATE_COLUMN = "INSERT_DATE";
    protected static final Set<String> mandatoryColumns = new HashSet<>(Arrays.asList("_id", ORIGINAL_ID, INSERT_DATE_COLUMN));

    public static final String VIEW_PREFIX = "V_";

    public static final String ID = "_id";

    public static final int CHUNK_SIZE = 700;

    public static final int BLOB_CHUNK_SIZE = 200;
    public static final int BLOB_FETCH_SIZE = 200;
    public static final String INCREMENTAL = "Incremental";
    public static final String FULL = "Full";
    public static final String BATCH_CONFIG = "BATCH_CONFIG";
    public static final String STATS = "stats";
    public static final String SCHEMA_SOURCE = "SCHEMA_SOURCE";
    public static final String SCHEMA_SOURCE_AUDIT = "SCHEMA_SOURCE_AUDIT";
    public static final String T24_METADATA = "T24_Metadata";
    public static final String RAW_PREFIX = "RAW_";
    public static final String SCHEDULED_OPS = "SCHEDULED_OPS";

    public static final String COB_STOPPED="STOP";
    public static final String COB_RUNNING="COB_RUNNING";
    public static final String COB_RECID="COB";

    public static final String COUNT_ALL = "countAll";
    public static final String COUNT_CHUNK = "countChunk";
    public static final String FAILED_RETRY_COUNT = "FAILED_RETRY_COUNT";


    public static final String DAY_TO_EXTRACT = "DAY_TO_EXTRACT";
    public static final String EXTRACT = "EXTRACT";
    public static final String EXTRACTED = "EXTRACTED";
    public static final String COMPLETED_STATUS = "COMPLETED";
    public static final String FAILED_STATUS = "FAILED";
    public static final String RECID_COLUMN = "RECID";
    public static final String SERVICE_COLUMN = "SERVICE_CONTROL";
    public static final String SERVICE_TABLE = "V_F_TSA_SERVICE";
    public static final String STEP_COB_STATUS = "COB_STATUS";
    public static final String PROCESSED_ROWS = "PROCESSED_ROWS";
    public static final String MULTI_VALUE = "StringMAX";
    public static final String LOCAL_REF = "Local_Ref";
    public static final String SYS_SINGLE = "sys.single";
    public static final String VARCHAR_8000 = "VARCHAR(8000)";
    public static final String VARCHAR_MAX = "VARCHAR(MAX)";
    public static final String SINGLE = "single";
    public static final String S = "S";
    public static final String M = "M";
    public static final String USR = "usr";
    public static final String NEW_TABLE = "NEW_TABLE";
    public static final String MAX = "MAX";
    public static final String STANDARD_SELECTION_DICTIONARY = "Standard_Selection_Dict";
    public static final String BI_STANDARD_SELECTION = "sysFieldOds";
    public static final String ORACLE_TABLE_NAME = "Table_Name";
    public static final String COLONNE_REF = "colonne_ref";
    public static final String LAST_DATE = "last_date";

    public static final String MAX_VALUE = "MAX_VALUE";
    public static final String BI_SYNC = "BI_SYNC";
    public static final String BASIC_NORMALIZATION = "BASIC_NORMALIZATION";
    public static final String QUERY_BUILDER = "QUERY_BUILDER";
    public static final String T24_SELECTION_NAME_PARAMETER = "T24_Selection_Name";
    public static final String T24_DATE_PREFIX = "T24Date";
    public static final int INSERT_CODE = 0;
    public static final int UPDATE_CODE = 1;
    public static final int RETRY_COUNT0 = 0;
    public static final int RETRY_COUNT1 = 1;
    public static final int RETRY_COUNT2 = 2;
    public static final int RETRY_COUNT3 = 3;
    public static final String LAST_EXTRACTION = "LAST_EXTRACTION";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ENGLISH);

    public static final String ID_TYPE= "NVARCHAR(255) NOT NULL";
    public static final String ORIGINAL_ID_TYPE = "NVARCHAR(250) not null";
    public static final String SCHEMA_ENABLED = "SELECT id, table_name, column_name, datatype, _enabled, indexed, ssTag, single_mult, local_ref FROM "+SCHEMA_SOURCE+" WHERE _enabled = 1 and table_name=:tableName";
    public static final String SCHEMA_TABLE = "SELECT id, table_name, column_name, datatype, _enabled, indexed, ssTag, single_mult, local_ref FROM "+SCHEMA_SOURCE+" WHERE table_name = :tableName ";
    public static final String SCHEMA_COLUMN_TABLE = "SELECT id, table_name,  column_name, datatype, _enabled, indexed, ssTag, single_mult, local_ref FROM "+SCHEMA_SOURCE+" WHERE table_name= :tableName AND column_name= :columnName";
    public static final String COUNT_ROWS_TABLE_SCHEMA_SOURCE = "SELECT count(id) FROM "+SCHEMA_SOURCE+" WHERE table_name= :tableName";
    public static final String COUNT_ENABLED_ROWS_TABLE_SCHEMA_SOURCE = "SELECT count(id) FROM "+SCHEMA_SOURCE+" WHERE table_name= :tableName AND _enabled = 1";
    public static final String SCHEMA_AUDIT_TABLE = "SELECT " +
            "id, sourceId, sourceTable_name, sourceColumn_name, sourceDatatype, cast (newSource_enabled as int) as newSource_enabled, cast(oldSource_enabled as int) as oldSource_enabled, cast (newSourceIndexed as int) as newSourceIndexed, cast (oldSourceIndexed as int) as oldSourceIndexed FROM "+SCHEMA_SOURCE_AUDIT+" WHERE sourceTable_name = :tableName";
    public static final String COUNT_QUERY = "SELECT COUNT (RECID) FROM ";
    public static final String BASE_QUERY = "SELECT RECID, XMLRECORD FROM ";
    public static final String SCHEDULED_OPS_QUERY = "SELECT table_name, column_name, op FROM " + SCHEDULED_OPS + " WHERE table_name = :table";
    public static final String IF_NOT_EXISTS_TABLE_PREFIX = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='";

    public static final String RECID = "TN0010001";

    public static final String RUN_ID = "run.id";


    public static final int CHUNKS_NUMBER = 700;
    public static final int FETCH_SIZE = 1000;
    public static final String FUNDS_TRANSFER_HIS = "FUNDS.TRANSFER$HIS";
    public static final String FBNK_CUSTOMER = "FBNK_CUSTOMER";
    public static final String FBNK_BILL_DETAILS = "FBNK_AA_BILL_DETAILS";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String JSON_NORMALIZATION = "JSON_NORMALIZATION";
    public static final String QUERY_BUILDING_STEP = "QueryBuildingStep";
    public static final String AA_BILL_DETAILS = "AA.BILL.DETAILS";
}
