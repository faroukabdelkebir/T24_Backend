package com.t24extractor.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerConfigController {

    /** Format attendu par LocalTime.parse dans l'AutoExtractionScheduler. */
    private static final Pattern HHMM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private static final String SELECT_CONFIGS = """
            SELECT id, TableName, T24SelectionName, colonne_ref, chargement,
                   last_date, min_date, enabled, scheduler, scheduler_time
            FROM BATCH_CONFIG
            ORDER BY id
            """;

    private final JdbcTemplate jdbcBI;

    public SchedulerConfigController(@Qualifier("jdbcBI") JdbcTemplate jdbcBI) {
        this.jdbcBI = jdbcBI;
    }

    @GetMapping("/config")
    public List<Map<String, Object>> getConfigs() {
        return jdbcBI.queryForList(SELECT_CONFIGS).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Planification automatique : bascule on/off et/ou changement d'heure.
     * Corps : { "scheduler": true|false, "schedulerTime": "HH:mm" } —
     * schedulerTime est optionnel (bascule sans changer l'heure existante).
     */
    @PatchMapping("/config/{id}")
    public ResponseEntity<Map<String, Object>> updateScheduler(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {

        boolean scheduler = Boolean.TRUE.equals(body.get("scheduler"));
        Object rawTime = body.get("schedulerTime");
        String schedulerTime = rawTime == null ? null : String.valueOf(rawTime).trim();
        if (schedulerTime != null && schedulerTime.isEmpty()) {
            schedulerTime = null;
        }

        if (schedulerTime != null && !HHMM.matcher(schedulerTime).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "schedulerTime doit être au format HH:mm (ex. 06:30)"));
        }

        // Activer la planification exige une heure : celle fournie, ou une déjà en base
        if (scheduler && schedulerTime == null && !hasSchedulerTime(id)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Aucune heure définie : fournissez schedulerTime (HH:mm)"));
        }

        int updated = schedulerTime == null
                ? jdbcBI.update("UPDATE BATCH_CONFIG SET scheduler = ? WHERE id = ?",
                scheduler ? 1 : 0, id)
                : jdbcBI.update("UPDATE BATCH_CONFIG SET scheduler = ?, scheduler_time = ? WHERE id = ?",
                scheduler ? 1 : 0, schedulerTime, id);

        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "id", id,
                "scheduler", scheduler,
                "schedulerTime", schedulerTime == null ? "" : schedulerTime,
                "updated", true));
    }

    /** Active/désactive une extraction (colonne enabled, lue aussi par le scheduler). */
    @PatchMapping("/config/{id}/enabled")
    public ResponseEntity<Map<String, Object>> toggleEnabled(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {

        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        int updated = jdbcBI.update(
                "UPDATE BATCH_CONFIG SET enabled = ? WHERE id = ?", enabled ? 1 : 0, id);

        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("id", id, "enabled", enabled, "updated", true));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean hasSchedulerTime(String id) {
        List<String> times = jdbcBI.queryForList(
                "SELECT scheduler_time FROM BATCH_CONFIG WHERE id = ?", String.class, id);
        return !times.isEmpty() && times.get(0) != null && !times.get(0).isBlank();
    }

    /** Aligne les clés JSON sur le modèle BatchConfigRecord du frontend. */
    private Map<String, Object> toDto(Map<String, Object> row) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", row.get("id"));
        dto.put("tableName", row.get("TableName"));
        dto.put("t24SelectionName", row.get("T24SelectionName"));
        dto.put("colonneRef", row.get("colonne_ref"));
        dto.put("chargement", row.get("chargement"));
        dto.put("lastDate", row.get("last_date"));
        dto.put("minDate", row.get("min_date"));
        dto.put("enabled", toBoolean(row.get("enabled")));
        dto.put("scheduler", toBoolean(row.get("scheduler")));
        dto.put("schedulerTime", row.get("scheduler_time"));
        return dto;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return false;
    }
}
