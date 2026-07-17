package com.t24extractor.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.t24extractor.model.UpdateSchemaSourceEnabledRequest;
import com.t24extractor.exception.DataNotFoundException;
import com.t24extractor.model.SchemaSource;
import com.t24extractor.repository.bi_repos.SchemaSourceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SchemaSourceControllerTest {

    private final SchemaSourceRepository repository = mock(SchemaSourceRepository.class);
    private final SchemaSourceController controller = new SchemaSourceController(repository);

    @Test
    void returnsColumnsByTableName() {
        SchemaSource column = new SchemaSource("id-1", "CUSTOMER", "NAME", "VARCHAR", true, false, "tag", "S", false);
        when(repository.findAllByTableName("CUSTOMER")).thenReturn(List.of(column));

        List<SchemaSource> result = controller.getColumnsByTable("CUSTOMER");

        assertEquals(List.of(column), result);
    }

    @Test
    void updatesColumnEnabledByTableAndColumn() throws Exception {
        SchemaSource column = new SchemaSource("id-1", "CUSTOMER", "NAME", "VARCHAR", false, false, "tag", "S", false);
        when(repository.updateEnabledByTableNameAndColumnName("CUSTOMER", "NAME", false)).thenReturn(1);
        when(repository.findAllByTableNameAndColumnName("CUSTOMER", "NAME")).thenReturn(Optional.of(column));

        SchemaSource result = controller.updateColumnEnabled("CUSTOMER", "NAME", new UpdateSchemaSourceEnabledRequest(false));

        assertEquals(column, result);
        verify(repository).updateEnabledByTableNameAndColumnName("CUSTOMER", "NAME", false);
    }

    @Test
    void throwsWhenColumnDoesNotExist() {
        when(repository.updateEnabledByTableNameAndColumnName("CUSTOMER", "missing", true)).thenReturn(0);

        assertThrows(DataNotFoundException.class,
                () -> controller.updateColumnEnabled("CUSTOMER", "missing", new UpdateSchemaSourceEnabledRequest(true)));
    }
}
