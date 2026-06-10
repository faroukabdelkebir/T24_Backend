package wevioo.se.monitoringms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wevioo.se.monitoringms.model.SchemaAuditRecord;
import wevioo.se.monitoringms.model.SchemaSourceRecord;
import wevioo.se.monitoringms.repository.SchemaRepository;
import wevioo.se.monitoringms.service.SchemaService;

import java.util.List;

/**
 * Endpoints du schéma T24.
 *
 * GET /api/v1/schema/summary           → Résumé par table (total / actives / indexées)
 * GET /api/v1/schema/columns           → Toutes les colonnes
 * GET /api/v1/schema/columns/{table}   → Colonnes d'une table
 * GET /api/v1/schema/audit             → Dernières modifications (trigger)
 * GET /api/v1/schema/audit/{table}     → Audit pour une table
 */
@RestController
@RequestMapping("/api/v1/schema")
@RequiredArgsConstructor
@Tag(name = "Schéma", description = "Exploration du dictionnaire SCHEMA_SOURCE et historique des évolutions")
public class SchemaController {

    private final SchemaService schemaService;

    @GetMapping("/summary")
    @Operation(summary = "Résumé du schéma par table T24")
    public ResponseEntity<List<SchemaRepository.TableSchemaSummary>> getSummary() {
        return ResponseEntity.ok(schemaService.getSchemaSummary());
    }

    @GetMapping("/columns")
    @Operation(summary = "Toutes les colonnes du schéma")
    public ResponseEntity<List<SchemaSourceRecord>> getAllColumns() {
        return ResponseEntity.ok(schemaService.getAllColumns());
    }

    @GetMapping("/columns/{table}")
    @Operation(summary = "Colonnes d'une table T24 spécifique")
    public ResponseEntity<List<SchemaSourceRecord>> getColumnsByTable(@PathVariable String table) {
        return ResponseEntity.ok(schemaService.getColumnsByTable(table));
    }

    @GetMapping("/audit")
    @Operation(summary = "Dernières modifications du schéma (déclencheur SCHEMA_SOURCE_AUDIT)")
    public ResponseEntity<List<SchemaAuditRecord>> getRecentAudit(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(schemaService.getRecentAuditEvents(limit));
    }

    @GetMapping("/audit/{table}")
    @Operation(summary = "Historique des modifications pour une table donnée")
    public ResponseEntity<List<SchemaAuditRecord>> getAuditByTable(@PathVariable String table) {
        return ResponseEntity.ok(schemaService.getAuditByTable(table));
    }
}

