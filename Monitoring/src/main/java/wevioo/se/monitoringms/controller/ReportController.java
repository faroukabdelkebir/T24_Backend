package wevioo.se.monitoringms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wevioo.se.monitoringms.service.ReportService;

/**
 * Endpoints de reporting.
 *
 * GET /api/v1/reports/daily   → Rapport complet du jour (KPIs, réconciliation, SLA, alertes)
 * GET /api/v1/reports/weekly  → Rapport hebdomadaire avec tendances
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Rapports", description = "Rapports journaliers et hebdomadaires du processus T24")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily")
    @Operation(summary = "Rapport journalier",
            description = "Indicateurs du jour et des 7 derniers jours, réconciliation Oracle/SQL Server, statut SLA et alertes actives.")
    public ResponseEntity<ReportService.DailyReport> getDailyReport() {
        return ResponseEntity.ok(reportService.generateDailyReport());
    }

    @GetMapping("/weekly")
    @Operation(summary = "Rapport hebdomadaire",
            description = "Tendances 7 jours, comparaison semaine précédente, top table par volume.")
    public ResponseEntity<ReportService.WeeklyReport> getWeeklyReport() {
        return ResponseEntity.ok(reportService.generateWeeklyReport());
    }
}
