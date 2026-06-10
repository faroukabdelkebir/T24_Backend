package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.config.CacheConfig;
import wevioo.se.monitoringms.model.SchemaAuditRecord;
import wevioo.se.monitoringms.model.SchemaSourceRecord;
import wevioo.se.monitoringms.repository.SchemaRepository;

import java.util.List;

/**
 * Service pour l'exploration du schéma T24 et de son historique d'évolution.
 *
 * Utilise SCHEMA_SOURCE (dictionnaire colonnes) et SCHEMA_SOURCE_AUDIT (trigger d'audit).
 */
@Service
@RequiredArgsConstructor
public class SchemaService {

    private final SchemaRepository schemaRepo;

    // ── Schema source ─────────────────────────────────────────────────────────

    @Cacheable(value = CacheConfig.CACHE_SCHEMA, key = "'all'")
    public List<SchemaSourceRecord> getAllColumns() {
        return schemaRepo.findAll();
    }

    @Cacheable(value = CacheConfig.CACHE_SCHEMA, key = "'table_' + #tableName")
    public List<SchemaSourceRecord> getColumnsByTable(String tableName) {
        return schemaRepo.findByTable(tableName);
    }

    @Cacheable(value = CacheConfig.CACHE_SCHEMA, key = "'summary'")
    public List<SchemaRepository.TableSchemaSummary> getSchemaSummary() {
        return schemaRepo.summarizeByTable();
    }

    // ── Audit ────────────────────────────────────────────────────────────────

    public List<SchemaAuditRecord> getRecentAuditEvents(int limit) {
        return schemaRepo.findRecentAudit(limit);
    }

    public List<SchemaAuditRecord> getAuditByTable(String tableName) {
        return schemaRepo.findAuditByTable(tableName);
    }

    public int countRecentDrifts(int days) {
        return schemaRepo.countRecentDrifts(days);
    }
}

