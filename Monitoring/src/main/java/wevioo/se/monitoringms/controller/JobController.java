package wevioo.se.monitoringms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wevioo.se.monitoringms.dto.JobExecutionDto;
import wevioo.se.monitoringms.dto.StepExecutionDto;
import wevioo.se.monitoringms.service.JobService;

import java.util.List;

/**
 * Endpoints pour l'historique des jobs et le drill-down jusqu'aux steps.
 *
 * GET /api/v1/jobs                          → N dernières exécutions (toutes tables)
 * GET /api/v1/jobs/history?days=7           → Exécutions sur N derniers jours
 * GET /api/v1/jobs/{id}                     → Détail d'une exécution
 * GET /api/v1/jobs/{id}/steps               → Steps d'une exécution
 * GET /api/v1/jobs/by-name/{name}           → Historique d'un job par nom
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Traitements", description = "Historique et détail des exécutions de traitements Spring Batch")
public class JobController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "Dernières exécutions de traitements",
            description = "Retourne les N exécutions les plus récentes, toutes tables confondues.")
    public ResponseEntity<List<JobExecutionDto>> getRecent(
            @Parameter(description = "Nombre maximal de résultats (défaut : 50)")
            @RequestParam(defaultValue = "50") int limit) {

        return ResponseEntity.ok(jobService.getRecent(limit));
    }

    @GetMapping("/history")
    @Operation(summary = "Exécutions sur N derniers jours")
    public ResponseEntity<List<JobExecutionDto>> getHistory(
            @Parameter(description = "Nombre de jours (défaut : 7)")
            @RequestParam(defaultValue = "7") int days) {

        return ResponseEntity.ok(jobService.getByDays(days));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une exécution de traitement",
            description = "Inclut les agrégats des étapes : lectures, écritures, éléments ignorés et débit.")
    public ResponseEntity<JobExecutionDto> getById(
            @Parameter(description = "Identifiant d'exécution Spring Batch")
            @PathVariable long id) {

        return ResponseEntity.ok(jobService.getById(id));
    }

    @GetMapping("/{id}/steps")
    @Operation(summary = "Liste des étapes d'une exécution",
            description = "Retourne chaque étape avec ses mesures détaillées.")
    public ResponseEntity<List<StepExecutionDto>> getSteps(@PathVariable long id) {
        return ResponseEntity.ok(jobService.getSteps(id));
    }

    @GetMapping("/by-name/{name}")
    @Operation(summary = "Historique par nom de traitement",
            description = "Noms valides : T24, CUSTOMER.JOB, AA.BILL.DETAILS.JOB, FT.HIS.JOB, SCHEMA_LOADER")
    public ResponseEntity<List<JobExecutionDto>> getByName(
            @PathVariable String name,
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(jobService.getByJobName(name, limit));
    }
}
