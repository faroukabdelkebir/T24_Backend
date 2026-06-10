package wevioo.se.monitoringms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wevioo.se.monitoringms.model.BatchConfigRecord;
import wevioo.se.monitoringms.repository.BatchConfigRepository;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de configuration des extractions.
 *
 * GET   /api/v1/config/batch          → Liste toutes les configs BATCH_CONFIG
 * GET   /api/v1/config/batch/{id}     → Config d'une table
 * PATCH /api/v1/config/batch/{id}     → Activer/désactiver une extraction
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Tag(name = "Configuration", description = "Consultation et mise à jour de la configuration BATCH_CONFIG")
public class ConfigController {

    private final BatchConfigRepository configRepo;

    @GetMapping("/batch")
    @Operation(summary = "Liste toutes les configurations d'extraction")
    public ResponseEntity<List<BatchConfigRecord>> getAllConfigs() {
        return ResponseEntity.ok(configRepo.findAll());
    }

    @GetMapping("/batch/{id}")
    @Operation(summary = "Configuration d'une table spécifique")
    public ResponseEntity<BatchConfigRecord> getById(@PathVariable String id) {
        return configRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/batch/{id}/toggle")
    @Operation(summary = "Activer ou désactiver une extraction",
            description = "Corps de la requête : { \"enabled\": true|false }")
    public ResponseEntity<Map<String, Object>> toggleEnabled(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {

        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        int updated = configRepo.setEnabled(id, enabled);

        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "id", id,
                "enabled", enabled,
                "updated", true
        ));
    }
}
