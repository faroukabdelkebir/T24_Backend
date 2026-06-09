package com.t24extractor.tasklet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.t24extractor.support.BatchTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class QueryTaskletTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void buildsFullQueryWithoutPartitionForSmallTable() throws Exception {
        QueryTasklet tasklet = configuredTasklet("Full");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString())).thenReturn(10L);
        BatchTestContext context = xmlContext();

        tasklet.execute(context.contribution(), context.chunkContext());

        assertEquals("SELECT  RECID , XMLRECORD FROM FBNK_CUSTOMER ",
                context.jobExecution().getExecutionContext().getString("QUERY"));
    }

    @Test
    void buildsQuotedIncrementalRangeAndPartitionClause() throws Exception {
        QueryTasklet tasklet = configuredTasklet("Incremental");
        tasklet.setColumnRef("BOOKING_DATE");
        tasklet.setLastDate("20260101");
        tasklet.setFutureDate("20260131");
        tasklet.setDateFormat("varchar");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString())).thenReturn(1000L);
        BatchTestContext context = xmlContext();

        tasklet.execute(context.contribution(), context.chunkContext());

        assertEquals(
                "SELECT  RECID , XMLRECORD FROM FBNK_CUSTOMER WHERE BOOKING_DATE > '20260101' AND BOOKING_DATE <= '20260131' AND MOD(ORA_HASH(ROWID), ?) = ?",
                context.jobExecution().getExecutionContext().getString("QUERY"));
    }

    @Test
    void doesNotExposeXmlQueryForBlobTable() throws Exception {
        QueryTasklet tasklet = configuredTasklet("Full");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString())).thenReturn(0L);
        BatchTestContext context = BatchTestContext.create();
        context.jobExecution().getExecutionContext().put("Type", 'B');

        tasklet.execute(context.contribution(), context.chunkContext());

        assertEquals(false, context.jobExecution().getExecutionContext().containsKey("QUERY"));
    }

    private QueryTasklet configuredTasklet(String loadingType) {
        QueryTasklet tasklet = new QueryTasklet(jdbcTemplate);
        tasklet.setTableName("FBNK_CUSTOMER");
        tasklet.setChargement(loadingType);
        return tasklet;
    }

    private BatchTestContext xmlContext() {
        BatchTestContext context = BatchTestContext.create();
        context.jobExecution().getExecutionContext().put("Type", 'X');
        return context;
    }
}
