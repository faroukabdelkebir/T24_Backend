package wevioo.se.monitoringms.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wevioo.se.monitoringms.dto.analytics.*;
import wevioo.se.monitoringms.service.AnalyticsService;

import java.util.List;
import java.util.Map;

/**
 * Endpoints analytiques métier (v2 — ajoute la page « Flux »).
 *
 * Page Synthèse :
 *   GET /api/v1/analytics/summary?year=            → KPIs + évolutions vs N-1
 *   GET /api/v1/analytics/transfers/monthly?year=  → volumes/montants mensuels N vs N-1
 *   GET /api/v1/analytics/customers/age-pyramid    → répartition par tranche d'âge
 *   GET /api/v1/analytics/customers/by-officer     → top gestionnaires
 *   GET /api/v1/analytics/billing/aging            → vieillissement des créances
 *   GET /api/v1/analytics/years                    → années disponibles (filtres)
 *   GET /api/v1/analytics/insights?year=           → enseignements automatiques
 *
 * Page Flux :
 *   GET /api/v1/analytics/flows/summary?year=        → KPIs volumes + montants
 *   GET /api/v1/analytics/flows/by-currency?year=    → répartition par devise
 *   GET /api/v1/analytics/flows/by-type?year=        → répartition par type d'opération
 *   GET /api/v1/analytics/flows/heatmap?year=        → activité mois × jour de semaine
 *   GET /api/v1/analytics/flows/columns-status       → colonnes enrichies présentes ?
 *   GET /api/v1/analytics/flows/insights?year=       → enseignements de flux
 *
 * Page Clients :
 *   GET /api/v1/analytics/customers/summary?year=      → KPIs démographie + acquisition
 *   GET /api/v1/analytics/customers/age-histogram      → âges par pas de 5 ans
 *   GET /api/v1/analytics/customers/generations        → répartition par génération
 *   GET /api/v1/analytics/customers/new-monthly?year=  → nouveaux clients N vs N-1
 *   GET /api/v1/analytics/customers/breakdown?by=      → segmentation (secteur, genre…)
 *   GET /api/v1/analytics/customers/dimensions         → dimensions déjà extraites
 *   GET /api/v1/analytics/customers/columns-status     → colonnes enrichies présentes ?
 *   GET /api/v1/analytics/customers/acquisition-years  → années d'entrée en relation
 *   GET /api/v1/analytics/customers/insights?year=     → enseignements clients
 *
 * Page Créances :
 *   GET /api/v1/analytics/billing/summary?year=     → KPIs facturation + recouvrement
 *   GET /api/v1/analytics/billing/monthly?year=     → factures N vs N-1 (+ montants)
 *   GET /api/v1/analytics/billing/breakdown?by=     → segmentation (aging, règlement…)
 *   GET /api/v1/analytics/billing/dimensions        → dimensions déjà extraites
 *   GET /api/v1/analytics/billing/columns-status    → colonnes enrichies présentes ?
 *   GET /api/v1/analytics/billing/years             → années de facturation
 *   GET /api/v1/analytics/billing/insights?year=    → enseignements créances
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytique métier", description = "Indicateurs bancaires issus des données T24 extraites")
public class AnalyticsController {

    private final AnalyticsService analytics;

    // ── Page Synthèse ─────────────────────────────────────────────────────────

    @GetMapping("/summary")
    @Operation(summary = "Indicateurs métier agrégés",
            description = "Clients, virements et factures avec évolution vs année précédente.")
    public ResponseEntity<AnalyticsSummaryDto> summary(
            @Parameter(description = "Année (0 = tout l'historique)")
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.summary(year));
    }

    @GetMapping("/transfers/monthly")
    @Operation(summary = "Volumes et montants mensuels de virements, comparés à N-1")
    public ResponseEntity<List<MonthlyFlowDto>> monthlyTransfers(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.monthlyTransfers(year));
    }

    @GetMapping("/customers/age-pyramid")
    @Operation(summary = "Répartition des clients par tranche d'âge")
    public ResponseEntity<List<BreakdownSliceDto>> agePyramid() {
        return ResponseEntity.ok(analytics.agePyramid());
    }

    @GetMapping("/customers/by-officer")
    @Operation(summary = "Portefeuille clients par gestionnaire (top N)")
    public ResponseEntity<List<OfficerPerfDto>> byOfficer(
            @Parameter(description = "Nombre de gestionnaires à retourner (max 50)")
            @RequestParam(defaultValue = "10") int top) {
        return ResponseEntity.ok(analytics.topOfficers(Math.clamp(top, 1, 50)));
    }

    @GetMapping("/billing/aging")
    @Operation(summary = "Vieillissement des créances",
            description = "Vide tant que la colonne AGING_STATUS n'a pas été extraite.")
    public ResponseEntity<List<BreakdownSliceDto>> billingAging() {
        return ResponseEntity.ok(analytics.billingAging());
    }

    @GetMapping("/years")
    @Operation(summary = "Années disponibles dans l'historique de virements")
    public ResponseEntity<List<Integer>> years() {
        return ResponseEntity.ok(analytics.availableYears());
    }

    @GetMapping("/insights")
    @Operation(summary = "Enseignements calculés automatiquement",
            description = "Gestionnaire leader, concentration du portefeuille, pic d'activité.")
    public ResponseEntity<List<InsightDto>> insights(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.insights(year));
    }

    // ── Page Flux ─────────────────────────────────────────────────────────────

    @GetMapping("/flows/summary")
    @Operation(summary = "KPIs des flux de virements",
            description = "Volumes, montants, ticket moyen et devise dominante. "
                    + "Les montants restent à null tant que AMOUNT_DEBITED n'est pas extraite.")
    public ResponseEntity<FlowsSummaryDto> flowsSummary(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.flowsSummary(year));
    }

    @GetMapping("/flows/by-currency")
    @Operation(summary = "Répartition des flux par devise",
            description = "Vide tant que DEBIT_CURRENCY n'a pas été extraite.")
    public ResponseEntity<List<FlowSliceDto>> flowsByCurrency(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.flowsByCurrency(year));
    }

    @GetMapping("/flows/by-type")
    @Operation(summary = "Répartition des flux par type d'opération",
            description = "Vide tant que TRANSACTION_TYPE n'a pas été extraite.")
    public ResponseEntity<List<FlowSliceDto>> flowsByType(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.flowsByType(year));
    }

    @GetMapping("/flows/heatmap")
    @Operation(summary = "Heatmap d'activité mois × jour de semaine",
            description = "Fonctionne sans extraction enrichie : la date métier est "
                    + "reconstruite depuis l'identifiant T24 (FTyyddd…).")
    public ResponseEntity<List<HeatmapCellDto>> activityHeatmap(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.activityHeatmap(year));
    }

    @GetMapping("/flows/columns-status")
    @Operation(summary = "État des colonnes enrichies de FUNDS_TRANSFER#HIS",
            description = "true = colonne extraite. Pilote le panneau « à débloquer » du front.")
    public ResponseEntity<Map<String, Boolean>> flowsColumnsStatus() {
        return ResponseEntity.ok(analytics.flowsColumnsStatus());
    }

    @GetMapping("/flows/insights")
    @Operation(summary = "Enseignements des flux",
            description = "Devise dominante, type d'opération dominant, ticket moyen.")
    public ResponseEntity<List<InsightDto>> flowsInsights(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.flowsInsights(year));
    }

    // ── Page Clients ──────────────────────────────────────────────────────────

    @GetMapping("/customers/summary")
    @Operation(summary = "KPIs de la page Clients",
            description = "Démographie et acquisition. Les nouveaux clients restent à null "
                    + "tant que DATE_AC_OPND n'est pas extraite.")
    public ResponseEntity<CustomersSummaryDto> customersSummary(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.customersSummary(year));
    }

    @GetMapping("/customers/age-histogram")
    @Operation(summary = "Histogramme des âges par pas de 5 ans")
    public ResponseEntity<List<BreakdownSliceDto>> ageHistogram() {
        return ResponseEntity.ok(analytics.ageHistogram());
    }

    @GetMapping("/customers/generations")
    @Operation(summary = "Répartition des clients par génération",
            description = "Dérivée de l'année de naissance (BIRTH_INCORP_DATE).")
    public ResponseEntity<List<BreakdownSliceDto>> generations() {
        return ResponseEntity.ok(analytics.generations());
    }

    @GetMapping("/customers/new-monthly")
    @Operation(summary = "Nouveaux clients par mois, comparés à N-1",
            description = "Vide tant que DATE_AC_OPND n'a pas été extraite.")
    public ResponseEntity<List<MonthlyFlowDto>> newCustomersMonthly(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.newCustomersMonthly(year));
    }

    @GetMapping("/customers/breakdown")
    @Operation(summary = "Segmentation des clients selon une dimension",
            description = "Dimensions : sector, nationality, gender, target, status. "
                    + "Vide tant que la colonne correspondante n'est pas extraite.")
    public ResponseEntity<List<BreakdownSliceDto>> customersBreakdown(
            @Parameter(description = "Dimension : sector | nationality | gender | target | status")
            @RequestParam(defaultValue = "sector") String by) {
        return ResponseEntity.ok(analytics.customersBreakdown(by));
    }

    @GetMapping("/customers/dimensions")
    @Operation(summary = "Dimensions de segmentation dont la colonne est déjà extraite")
    public ResponseEntity<List<String>> customerDimensions() {
        return ResponseEntity.ok(analytics.availableCustomerDimensions());
    }

    @GetMapping("/customers/columns-status")
    @Operation(summary = "État des colonnes enrichies de CUSTOMER",
            description = "true = colonne extraite. Pilote le panneau « à débloquer » du front.")
    public ResponseEntity<Map<String, Boolean>> customersColumnsStatus() {
        return ResponseEntity.ok(analytics.customersColumnsStatus());
    }

    @GetMapping("/customers/acquisition-years")
    @Operation(summary = "Années d'entrée en relation disponibles",
            description = "Vide tant que DATE_AC_OPND n'a pas été extraite.")
    public ResponseEntity<List<Integer>> acquisitionYears() {
        return ResponseEntity.ok(analytics.acquisitionYears());
    }

    @GetMapping("/customers/insights")
    @Operation(summary = "Enseignements de la page Clients",
            description = "Génération dominante, cœur actif 25–54 ans, pic d'acquisition "
                    + "ou part des dossiers migrés.")
    public ResponseEntity<List<InsightDto>> customersInsights(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.customersInsights(year));
    }

    // ── Page Créances ─────────────────────────────────────────────────────────

    @GetMapping("/billing/summary")
    @Operation(summary = "KPIs de la page Créances",
            description = "Facturation, encours, taux de recouvrement et délai de règlement. "
                    + "Les montants restent à null tant que OR/OS_TOTAL_AMOUNT ne sont pas extraites.")
    public ResponseEntity<BillingSummaryDto> billingSummary(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.billingSummary(year));
    }

    @GetMapping("/billing/monthly")
    @Operation(summary = "Factures émises par mois, comparées à N-1",
            description = "Fonctionne sans extraction enrichie : la date métier est "
                    + "reconstruite depuis l'identifiant T24 (AABILLyyddd…).")
    public ResponseEntity<List<MonthlyFlowDto>> billingMonthly(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.billingMonthly(year));
    }

    @GetMapping("/billing/breakdown")
    @Operation(summary = "Segmentation des créances selon une dimension",
            description = "Dimensions : aging, settle, status, type, method. "
                    + "Vide tant que la colonne correspondante n'est pas extraite.")
    public ResponseEntity<List<FlowSliceDto>> billingBreakdown(
            @Parameter(description = "Dimension : aging | settle | status | type | method")
            @RequestParam(defaultValue = "aging") String by,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.billingBreakdown(by, year));
    }

    @GetMapping("/billing/dimensions")
    @Operation(summary = "Dimensions de segmentation des créances déjà extraites")
    public ResponseEntity<List<String>> billingDimensions() {
        return ResponseEntity.ok(analytics.availableBillingDimensions());
    }

    @GetMapping("/billing/columns-status")
    @Operation(summary = "État des colonnes enrichies de AA_BILL_DETAILS",
            description = "true = colonne extraite. Pilote le panneau « à débloquer » du front.")
    public ResponseEntity<Map<String, Boolean>> billingColumnsStatus() {
        return ResponseEntity.ok(analytics.billingColumnsStatus());
    }

    @GetMapping("/billing/years")
    @Operation(summary = "Années disponibles dans l'historique de facturation")
    public ResponseEntity<List<Integer>> billYears() {
        return ResponseEntity.ok(analytics.billYears());
    }

    @GetMapping("/billing/insights")
    @Operation(summary = "Enseignements de la page Créances",
            description = "Mois record de facturation, part des factures réglées, "
                    + "délai moyen de règlement.")
    public ResponseEntity<List<InsightDto>> billingInsights(
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analytics.billingInsights(year));
    }
}
