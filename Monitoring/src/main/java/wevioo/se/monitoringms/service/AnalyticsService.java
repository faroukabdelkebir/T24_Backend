package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.dto.analytics.*;
import wevioo.se.monitoringms.repository.AnalyticsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Couche analytique métier .
 *
 * Chaque KPI est comparé, quand c'est possible, à la période N-1
 * (même logique que les rapports « Évol. vs A-1 » des analystes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    /** Ajouter la constante dans CacheConfig et déclarer le cache (TTL conseillé : 5 min). */
    public static final String CACHE_ANALYTICS = "analytics";

    private final AnalyticsRepository repo;

    // ── Bandeau KPI (page Synthèse) ──────────────────────────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'summary-' + #year")
    public AnalyticsSummaryDto summary(int year) {
        long ftN  = repo.countTransfers(year);
        long ftN1 = year > 0 ? repo.countTransfers(year - 1) : 0;
        long biN  = repo.countBills(year);
        long biN1 = year > 0 ? repo.countBills(year - 1) : 0;

        return AnalyticsSummaryDto.builder()
                .totalClients(repo.countClients())
                .ageMoyen(repo.averageAge())
                .nbGestionnaires(repo.countOfficers())
                .nbVirements(ftN)
                .deltaVirementsPct(delta(ftN, ftN1))
                .nbFactures(biN)
                .deltaFacturesPct(delta(biN, biN1))
                .montantVirements(repo.sumTransfers(year))
                .devisePrincipale(dominantCurrency(year))
                .build();
    }

    // ── Page Flux : KPIs ─────────────────────────────────────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'flows-summary-' + #year")
    public FlowsSummaryDto flowsSummary(int year) {
        long   nbN      = repo.countTransfers(year);
        long   nbN1     = year > 0 ? repo.countTransfers(year - 1) : 0;
        Double amountN  = repo.sumTransfers(year);
        Double amountN1 = year > 0 ? repo.sumTransfers(year - 1) : null;

        List<Map<String, Object>> currencies = repo.flowsByCurrency(year);
        List<Map<String, Object>> types      = repo.flowsByType(year);

        return FlowsSummaryDto.builder()
                .nbOperations(nbN)
                .deltaOpsPct(delta(nbN, nbN1))
                .montantTotal(amountN)
                .deltaMontantPct(deltaAmount(amountN, amountN1))
                .ticketMoyen(amountN != null && nbN > 0 ? round1(amountN / nbN) : null)
                .deviseDominante(currencies.isEmpty()
                        ? null : String.valueOf(currencies.get(0).get("label")))
                .nbDevises(currencies.isEmpty() ? null : currencies.size())
                .nbTypes(types.isEmpty() ? null : types.size())
                .build();
    }

    /** Colonnes enrichies présentes / manquantes — pilote le panneau « à débloquer ». */
    public Map<String, Boolean> flowsColumnsStatus() {
        return repo.ftEnrichedColumnsStatus();
    }

    // ── Évolution mensuelle N vs N-1 (volumes + montants) ────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'monthly-' + #year")
    public List<MonthlyFlowDto> monthlyTransfers(int year) {
        Map<Integer, Long>   current    = indexByMonth(repo.monthlyTransfers(year));
        Map<Integer, Long>   previous   = year > 0
                ? indexByMonth(repo.monthlyTransfers(year - 1)) : Map.of();
        Map<Integer, Double> amounts    = indexAmounts(repo.monthlyAmounts(year));
        Map<Integer, Double> amountsN1  = year > 0
                ? indexAmounts(repo.monthlyAmounts(year - 1)) : Map.of();

        List<MonthlyFlowDto> out = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            out.add(MonthlyFlowDto.builder()
                    .mois(m)
                    .nbOperations(current.getOrDefault(m, 0L))
                    .nbOperationsN1(previous.isEmpty() ? null : previous.getOrDefault(m, 0L))
                    .montantTotal(amounts.isEmpty() ? null : amounts.getOrDefault(m, 0.0))
                    .montantTotalN1(amountsN1.isEmpty() ? null : amountsN1.getOrDefault(m, 0.0))
                    .build());
        }
        return out;
    }

    // ── Page Flux : répartitions ─────────────────────────────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'flows-currency-' + #year")
    public List<FlowSliceDto> flowsByCurrency(int year) {
        return toFlowSlices(repo.flowsByCurrency(year));
    }

    @Cacheable(value = CACHE_ANALYTICS, key = "'flows-type-' + #year")
    public List<FlowSliceDto> flowsByType(int year) {
        return toFlowSlices(repo.flowsByType(year));
    }

    @Cacheable(value = CACHE_ANALYTICS, key = "'flows-heatmap-' + #year")
    public List<HeatmapCellDto> activityHeatmap(int year) {
        return repo.activityHeatmap(year).stream()
                .map(r -> HeatmapCellDto.builder()
                        .mois(((Number) r.get("mois")).intValue())
                        .jourSemaine(((Number) r.get("jour")).intValue())
                        .nb(((Number) r.get("nb")).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Page Flux : insights automatiques ────────────────────────────────────

    /**
     * Enseignements de la page Flux : devise dominante, type d'opération
     * dominant, ticket moyen. Chaque insight n'apparaît que si la colonne
     * qui le rend calculable a été extraite.
     */
    @Cacheable(value = CACHE_ANALYTICS, key = "'flows-insights-' + #year")
    public List<InsightDto> flowsInsights(int year) {
        List<InsightDto> out = new ArrayList<>();

        List<FlowSliceDto> currencies = flowsByCurrency(year).stream()
                .filter(s -> !"\u2014".equals(s.getLabel())).collect(Collectors.toList());
        if (!currencies.isEmpty()) {
            FlowSliceDto lead = currencies.get(0);
            out.add(InsightDto.builder()
                    .type("LEADER")
                    .titleKey("analytics.flows.insight.currency")
                    .value(lead.getLabel())
                    .detailKey("analytics.flows.insight.currencyDetail")
                    .params(Map.of("pct", lead.getPct(), "count", lead.getNbOperations()))
                    .build());
        }

        List<FlowSliceDto> types = flowsByType(year).stream()
                .filter(s -> !"\u2014".equals(s.getLabel())).collect(Collectors.toList());
        if (!types.isEmpty()) {
            FlowSliceDto lead = types.get(0);
            out.add(InsightDto.builder()
                    .type("CONCENTRATION")
                    .titleKey("analytics.flows.insight.type")
                    .value(lead.getLabel())
                    .detailKey("analytics.flows.insight.typeDetail")
                    .params(Map.of("pct", lead.getPct()))
                    .build());
        }

        FlowsSummaryDto s = flowsSummary(year);
        if (s.getTicketMoyen() != null) {
            out.add(InsightDto.builder()
                    .type("PIC_ACTIVITE")
                    .titleKey("analytics.flows.insight.ticket")
                    .value(String.format("%,.0f", s.getTicketMoyen()))
                    .detail(s.getDeviseDominante() == null ? "" : s.getDeviseDominante())
                    .build());
        }
        return out;
    }

    // ── Page Clients : KPIs ──────────────────────────────────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'customers-summary-' + #year")
    public CustomersSummaryDto customersSummary(int year) {
        Long newN  = repo.countNewCustomers(year);
        Long newN1 = (newN != null && year > 0) ? repo.countNewCustomers(year - 1) : null;

        List<Map<String, Object>> genders       = repo.customerBreakdown("gender", 10);
        List<Map<String, Object>> nationalities = repo.customerBreakdown("nationality", 500);
        List<Map<String, Object>> sectors       = repo.customerBreakdown("sector", 500);

        return CustomersSummaryDto.builder()
                .totalClients(repo.countClients())
                .ageMoyen(round1OrNull(repo.averageAge()))
                .nbGestionnaires(repo.countOfficers())
                .pctMigration(round1OrNull(repo.migrationShare()))
                .nouveauxClients(newN)
                .deltaNouveauxPct(newN != null && newN1 != null ? delta(newN, newN1) : null)
                .pctFemmes(femaleShare(genders))
                .nbNationalites(nationalities.isEmpty() ? null : nationalities.size())
                .nbSecteurs(sectors.isEmpty() ? null : sectors.size())
                .build();
    }

    /** Colonnes enrichies présentes / manquantes — pilote le panneau « à débloquer ». */
    public Map<String, Boolean> customersColumnsStatus() {
        return repo.customerEnrichedColumnsStatus();
    }

    /** Années d'entrée en relation disponibles (filtre de la page Clients). */
    public List<Integer> acquisitionYears() {
        return repo.acquisitionYears();
    }

    // ── Page Clients : démographie ───────────────────────────────────────────

    /** Histogramme des âges par pas de 5 ans (labels « 40–44 », « 85 + »). */
    @Cacheable(value = CACHE_ANALYTICS, key = "'customers-histogram'")
    public List<BreakdownSliceDto> ageHistogram() {
        List<Map<String, Object>> rows = repo.ageHistogram();
        long total = rows.stream().mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        return rows.stream()
                .map(r -> {
                    int band = ((Number) r.get("band")).intValue();
                    String label = band >= 85 ? "85 +" : band + "–" + (band + 4);
                    return slice(label, ((Number) r.get("nb")).longValue(), total);
                })
                .collect(Collectors.toList());
    }

    /** Répartition par génération (dérivée de l'année de naissance). */
    @Cacheable(value = CACHE_ANALYTICS, key = "'customers-generations'")
    public List<BreakdownSliceDto> generations() {
        List<Map<String, Object>> rows = repo.generations();
        long total = rows.stream().mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        return rows.stream()
                .map(r -> slice(String.valueOf(r.get("label")),
                        ((Number) r.get("nb")).longValue(), total))
                .collect(Collectors.toList());
    }

    /** Nouveaux clients par mois, N vs N-1 — vide tant que DATE_AC_OPND absente. */
    @Cacheable(value = CACHE_ANALYTICS, key = "'customers-new-monthly-' + #year")
    public List<MonthlyFlowDto> newCustomersMonthly(int year) {
        List<Map<String, Object>> raw = repo.newCustomersMonthly(year);
        if (raw.isEmpty()) return List.of();

        Map<Integer, Long> current  = indexByMonth(raw);
        Map<Integer, Long> previous = year > 0
                ? indexByMonth(repo.newCustomersMonthly(year - 1)) : Map.of();

        List<MonthlyFlowDto> out = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            out.add(MonthlyFlowDto.builder()
                    .mois(m)
                    .nbOperations(current.getOrDefault(m, 0L))
                    .nbOperationsN1(previous.isEmpty() ? null : previous.getOrDefault(m, 0L))
                    .montantTotal(null)
                    .montantTotalN1(null)
                    .build());
        }
        return out;
    }

    /** Segmentation selon une dimension autorisée (secteur, nationalité, genre…). */
    @Cacheable(value = CACHE_ANALYTICS, key = "'customers-breakdown-' + #dimension")
    public List<BreakdownSliceDto> customersBreakdown(String dimension) {
        List<Map<String, Object>> rows = repo.customerBreakdown(dimension, 12);
        long total = rows.stream().mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        return rows.stream()
                .map(r -> slice(String.valueOf(r.get("label")),
                        ((Number) r.get("nb")).longValue(), total))
                .collect(Collectors.toList());
    }

    /** Dimensions de segmentation dont la colonne est déjà extraite. */
    public List<String> availableCustomerDimensions() {
        return AnalyticsRepository.CUSTOMER_DIMENSIONS.entrySet().stream()
                .filter(e -> repo.hasColumn(AnalyticsRepository.CUS_TABLE, e.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    // ── Page Clients : insights automatiques ────────────────────────────────

    /**
     * Enseignements de la page Clients : génération dominante, cœur actif
     * (part des 25–54 ans) et, selon les données, pic d'acquisition ou part
     * des dossiers issus de la migration initiale.
     */
    @Cacheable(value = CACHE_ANALYTICS, key = "'customers-insights-' + #year")
    public List<InsightDto> customersInsights(int year) {
        List<InsightDto> out = new ArrayList<>();

        generations().stream()
                .max((a, b) -> Long.compare(a.getValue(), b.getValue()))
                .ifPresent(top -> out.add(InsightDto.builder()
                        .type("LEADER")
                        .titleKey("analytics.customers.insight.generation")
                        .value(top.getLabel())
                        .detailKey("analytics.customers.insight.generationDetail")
                        .params(Map.of("count", top.getValue(), "pct", top.getPct()))
                        .build()));

        Double core = repo.ageShare(25, 54);
        if (core != null) {
            out.add(InsightDto.builder()
                    .type("CONCENTRATION")
                    .titleKey("analytics.customers.insight.core")
                    .value(round1(core) + " %")
                    .detailKey("analytics.customers.insight.coreDetail")
                    .build());
        }

        // Pic d'acquisition si l'entrée en relation est extraite, sinon part migrée.
        List<MonthlyFlowDto> acquisition = newCustomersMonthly(year);
        if (!acquisition.isEmpty()) {
            acquisition.stream()
                    .max((a, b) -> Long.compare(a.getNbOperations(), b.getNbOperations()))
                    .ifPresent(peak -> out.add(InsightDto.builder()
                            .type("PIC_ACTIVITE")
                            .titleKey("analytics.customers.insight.peak")
                            .value(String.format("%02d/%s", peak.getMois(),
                                    year > 0 ? year : "—"))
                            .detailKey("analytics.customers.insight.peakDetail")
                            .params(Map.of("count", peak.getNbOperations()))
                            .build()));
        } else {
            Double migration = repo.migrationShare();
            if (migration != null) {
                out.add(InsightDto.builder()
                        .type("PIC_ACTIVITE")
                        .titleKey("analytics.customers.insight.migration")
                        .value(round1(migration) + " %")
                        .detailKey("analytics.customers.insight.migrationDetail")
                        .build());
            }
        }
        return out;
    }

    // ── Page Créances : KPIs ─────────────────────────────────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'billing-summary-' + #year")
    public BillingSummaryDto billingSummary(int year) {
        long   nbN      = repo.countBills(year);
        long   nbN1     = year > 0 ? repo.countBills(year - 1) : 0;
        Double billed   = repo.billedAmount(year);
        Double pending  = repo.outstandingAmount(year);

        Double recovery = null;
        if (billed != null && pending != null && billed > 0) {
            recovery = round1(100.0 * (billed - pending) / billed);
        }

        return BillingSummaryDto.builder()
                .nbFactures(nbN)
                .deltaFacturesPct(delta(nbN, nbN1))
                .montantFacture(billed)
                .encoursTotal(pending)
                .tauxRecouvrement(recovery)
                .pctPayees(paidShare(year))
                .delaiMoyenReglement(round1OrNull(repo.avgSettlementDelay(year)))
                .build();
    }

    /** Colonnes enrichies présentes / manquantes — pilote le panneau « à débloquer ». */
    public Map<String, Boolean> billingColumnsStatus() {
        return repo.billingEnrichedColumnsStatus();
    }

    /** Années de facturation disponibles (filtre de la page Créances). */
    public List<Integer> billYears() {
        return repo.billYears();
    }

    // ── Page Créances : évolution & segmentation ─────────────────────────────

    /** Factures par mois, N vs N-1, avec montants facturés si extraits. */
    @Cacheable(value = CACHE_ANALYTICS, key = "'billing-monthly-' + #year")
    public List<MonthlyFlowDto> billingMonthly(int year) {
        Map<Integer, Long>   current   = indexByMonth(repo.monthlyBills(year));
        Map<Integer, Long>   previous  = year > 0
                ? indexByMonth(repo.monthlyBills(year - 1)) : Map.of();
        Map<Integer, Double> amounts   = indexAmounts(repo.monthlyBillAmounts(year));
        Map<Integer, Double> amountsN1 = year > 0
                ? indexAmounts(repo.monthlyBillAmounts(year - 1)) : Map.of();

        List<MonthlyFlowDto> out = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            out.add(MonthlyFlowDto.builder()
                    .mois(m)
                    .nbOperations(current.getOrDefault(m, 0L))
                    .nbOperationsN1(previous.isEmpty() ? null : previous.getOrDefault(m, 0L))
                    .montantTotal(amounts.isEmpty() ? null : amounts.getOrDefault(m, 0.0))
                    .montantTotalN1(amountsN1.isEmpty() ? null : amountsN1.getOrDefault(m, 0.0))
                    .build());
        }
        return out;
    }

    /** Segmentation des créances (aging, règlement, statut, type, mode de paiement). */
    @Cacheable(value = CACHE_ANALYTICS, key = "'billing-breakdown-' + #dimension + '-' + #year")
    public List<FlowSliceDto> billingBreakdown(String dimension, int year) {
        return toFlowSlices(repo.billingBreakdown(dimension, year, 12));
    }

    /** Dimensions de segmentation des créances dont la colonne est déjà extraite. */
    public List<String> availableBillingDimensions() {
        return AnalyticsRepository.BILLING_DIMENSIONS.entrySet().stream()
                .filter(e -> repo.hasColumn(AnalyticsRepository.BILL_TABLE, e.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    // ── Page Créances : insights automatiques ────────────────────────────────

    /**
     * Enseignements de la page Créances : mois record de facturation
     * (calculable dès aujourd'hui via les IDs AABILLyyddd…), part des
     * factures réglées et délai moyen de règlement après enrichissement.
     */
    @Cacheable(value = CACHE_ANALYTICS, key = "'billing-insights-' + #year")
    public List<InsightDto> billingInsights(int year) {
        List<InsightDto> out = new ArrayList<>();

        repo.monthlyBills(year).stream()
                .max((a, b) -> Long.compare(((Number) a.get("nb")).longValue(),
                        ((Number) b.get("nb")).longValue()))
                .ifPresent(peak -> out.add(InsightDto.builder()
                        .type("LEADER")
                        .titleKey("analytics.billing.insight.peak")
                        .value(String.format("%02d/%s", ((Number) peak.get("mois")).intValue(),
                                year > 0 ? year : "—"))
                        .detailKey("analytics.billing.insight.peakDetail")
                        .params(Map.of("count", peak.get("nb")))
                        .build()));

        Double paid = paidShare(year);
        if (paid != null) {
            out.add(InsightDto.builder()
                    .type("CONCENTRATION")
                    .titleKey("analytics.billing.insight.paid")
                    .value(paid + " %")
                    .detailKey("analytics.billing.insight.paidDetail")
                    .build());
        }

        Double delay = repo.avgSettlementDelay(year);
        if (delay != null) {
            out.add(InsightDto.builder()
                    .type("PIC_ACTIVITE")
                    .titleKey("analytics.billing.insight.delay")
                    .value(round1(Math.abs(delay)) + " j")
                    .detailKey(delay >= 0 ? "analytics.billing.insight.delayAfter"
                            : "analytics.billing.insight.delayBefore")
                    .build());
        }
        return out;
    }

    /** Part des factures réglées (SETTLE_STATUS) — null tant qu'absente. */
    private Double paidShare(int year) {
        List<Map<String, Object>> rows = repo.billingBreakdown("settle", year, 50);
        if (rows.isEmpty()) return null;
        long total = rows.stream().mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        long paid = rows.stream()
                .filter(r -> {
                    String label = String.valueOf(r.get("label")).toUpperCase();
                    // REPAID / PAID / SETTLED / PART.SETTLED comptent comme réglées
                    return label.contains("PAID") || label.contains("SETTLED");
                })
                .mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        return total == 0 ? null : round1(100.0 * paid / total);
    }

    // ── Répartitions (page Synthèse) ─────────────────────────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'pyramid'")
    public List<BreakdownSliceDto> agePyramid() {
        List<Map<String, Object>> rows = repo.agePyramid();
        long total = rows.stream().mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        return rows.stream()
                .map(r -> slice((String) r.get("tranche"), ((Number) r.get("nb")).longValue(), total))
                .collect(Collectors.toList());
    }

    @Cacheable(value = CACHE_ANALYTICS, key = "'officers-' + #top")
    public List<OfficerPerfDto> topOfficers(int top) {
        return repo.portfolioByOfficer(top).stream()
                .map(r -> OfficerPerfDto.builder()
                        .officer(String.valueOf(r.get("officer")))
                        .nbClients(((Number) r.get("nb")).longValue())
                        .ageMoyen(r.get("age_moyen") == null ? null : ((Number) r.get("age_moyen")).doubleValue())
                        .partPct(round1(((Number) r.get("part_pct")).doubleValue()))
                        .build())
                .collect(Collectors.toList());
    }

    @Cacheable(value = CACHE_ANALYTICS, key = "'aging'")
    public List<BreakdownSliceDto> billingAging() {
        List<Map<String, Object>> rows = repo.billingAging();
        long total = rows.stream().mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        return rows.stream()
                .map(r -> slice(String.valueOf(r.get("label")), ((Number) r.get("nb")).longValue(), total))
                .collect(Collectors.toList());
    }

    public List<Integer> availableYears() {
        return repo.transferYears();
    }

    // ── Insights (page Synthèse) ─────────────────────────────────────────────

    @Cacheable(value = CACHE_ANALYTICS, key = "'insights-' + #year")
    public List<InsightDto> insights(int year) {
        List<InsightDto> out = new ArrayList<>();

        List<OfficerPerfDto> officers = topOfficers(5);
        if (!officers.isEmpty()) {
            OfficerPerfDto leader = officers.get(0);
            out.add(InsightDto.builder()
                    .type("LEADER")
                    .titleKey("analytics.insight.leader")
                    .value(leader.getOfficer())
                    .detailKey("analytics.insight.leaderDetail")
                    .params(Map.of("count", leader.getNbClients(), "pct", leader.getPartPct()))
                    .build());

            double top5 = round1(officers.stream().mapToDouble(OfficerPerfDto::getPartPct).sum());
            out.add(InsightDto.builder()
                    .type("CONCENTRATION")
                    .titleKey("analytics.insight.concentration")
                    .value(top5 + " %")
                    .detailKey("analytics.insight.concentrationDetail")
                    .params(Map.of("count", repo.countOfficers()))
                    .build());
        }

        repo.monthlyTransfers(year).stream()
                .max((a, b) -> Long.compare(((Number) a.get("nb")).longValue(),
                        ((Number) b.get("nb")).longValue()))
                .ifPresent(peak -> out.add(InsightDto.builder()
                        .type("PIC_ACTIVITE")
                        .titleKey("analytics.insight.peak")
                        .value(String.format("%02d/%s", ((Number) peak.get("mois")).intValue(),
                                year > 0 ? year : "—"))
                        .detailKey("analytics.insight.peakDetail")
                        .params(Map.of("count", peak.get("nb")))
                        .build()));

        return out;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String dominantCurrency(int year) {
        List<Map<String, Object>> currencies = repo.flowsByCurrency(year);
        return currencies.isEmpty() ? null : String.valueOf(currencies.get(0).get("label"));
    }

    private List<FlowSliceDto> toFlowSlices(List<Map<String, Object>> rows) {
        boolean hasAmounts = rows.stream().anyMatch(r -> r.get("montant") != null);
        double amountTotal = rows.stream().filter(r -> r.get("montant") != null)
                .mapToDouble(r -> ((Number) r.get("montant")).doubleValue()).sum();
        // Part sur les montants seulement s'ils sont significatifs ; sinon sur
        // le volume (encours tous soldés => 0 partout n'apporte rien)
        boolean pctOnAmounts = hasAmounts && amountTotal > 0;
        double total = pctOnAmounts
                ? amountTotal
                : rows.stream().mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();

        return rows.stream()
                .map(r -> {
                    long   nb     = ((Number) r.get("nb")).longValue();
                    Double amount = r.get("montant") == null
                            ? null : ((Number) r.get("montant")).doubleValue();
                    double base   = pctOnAmounts ? (amount == null ? 0 : amount) : nb;
                    return FlowSliceDto.builder()
                            .label(String.valueOf(r.get("label")))
                            .nbOperations(nb)
                            .montantTotal(amount)
                            .ticketMoyen(amount != null && nb > 0 ? round1(amount / nb) : null)
                            .pct(total == 0 ? 0 : round1(100.0 * base / total))
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getPct(), a.getPct()))
                .collect(Collectors.toList());
    }

    private static Map<Integer, Long> indexByMonth(List<Map<String, Object>> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r.get("mois")).intValue(),
                r -> ((Number) r.get("nb")).longValue(),
                (a, b) -> a));
    }

    private static Map<Integer, Double> indexAmounts(List<Map<String, Object>> rows) {
        return rows.stream()
                .filter(r -> r.get("montant") != null)
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("mois")).intValue(),
                        r -> ((Number) r.get("montant")).doubleValue(),
                        (a, b) -> a));
    }

    private static Double delta(long current, long previous) {
        if (previous <= 0) return null;
        return round1(100.0 * (current - previous) / previous);
    }

    private static Double round1OrNull(Double v) {
        return v == null ? null : round1(v);
    }

    /** Part des clientes — null si GENDER absente ou valeurs inexploitables. */
    private static Double femaleShare(List<Map<String, Object>> genderRows) {
        if (genderRows.isEmpty()) return null;
        long total = genderRows.stream()
                .mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        long female = genderRows.stream()
                .filter(r -> String.valueOf(r.get("label")).toUpperCase().startsWith("F"))
                .mapToLong(r -> ((Number) r.get("nb")).longValue()).sum();
        return total == 0 ? null : round1(100.0 * female / total);
    }

    private static Double deltaAmount(Double current, Double previous) {
        if (current == null || previous == null || previous <= 0) return null;
        return round1(100.0 * (current - previous) / previous);
    }

    private static BreakdownSliceDto slice(String label, long value, long total) {
        return BreakdownSliceDto.builder()
                .label(label)
                .value(value)
                .pct(total == 0 ? 0 : round1(100.0 * value / total))
                .build();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
