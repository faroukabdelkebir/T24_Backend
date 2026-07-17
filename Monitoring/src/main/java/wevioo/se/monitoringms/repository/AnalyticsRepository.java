package wevioo.se.monitoringms.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Requêtes analytiques métier sur les tables extraites de T24.
 * (v4.2 — satellites dédoublonnés : résistant aux réinsertions du Json_Flattener)
 *
 * Particularité : les identifiants T24 encodent la date métier
 * (FTyyddd…, AABILLyyddd…), ce qui permet de reconstruire l'axe temps
 * même quand PROCESSING_DATE n'a pas encore été extraite.
 *
 * Champs multivalués (single_mult = 'M') : le Json_Flattener de l'extracteur
 * les normalise dans des tables satellites nommées « TABLE#CHAMP »
 * (_id, CHAMP nvarchar(355), MV_SEQUENCE, SEQUENCE). La colonne du même nom
 * dans la table principale ne contient que le JSON brut — ne jamais la
 * requêter directement pour l'analytique.
 *
 * Dégradation contrôlée : {@link #hasColumn} / {@link #tableExists} vérifient
 * (et mettent en cache) la présence des colonnes et satellites « enrichis » —
 * les indicateurs concernés restent à null tant que l'extraction complète
 * n'a pas été relancée.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AnalyticsRepository {

    private final JdbcTemplate jdbc;

    public static final String FT_TABLE   = "FUNDS_TRANSFER#HIS";
    public static final String CUS_TABLE  = "CUSTOMER";
    public static final String BILL_TABLE = "AA_BILL_DETAILS";

    /** Nom d'une table satellite de champ multivalué (convention Json_Flattener). */
    private static String satellite(String table, String field) {
        return table + "#" + field;
    }

    /**
     * Filtre « une ligne par enregistrement » sur un satellite : on ne garde
     * que la première multivaleur / sous-valeur pour les répartitions.
     */
    private static final String FIRST_MV =
            "COALESCE(s.MV_SEQUENCE, 1) = 1 AND COALESCE(s.[SEQUENCE], 1) = 1";

    /** Dimensions de segmentation client autorisées (clé API → colonne SQL). */
    public static final Map<String, String> CUSTOMER_DIMENSIONS = Map.of(
            "sector",      "SECTOR",
            "nationality", "NATIONALITY",
            "gender",      "GENDER",
            "target",      "TARGET",
            "status",      "CUSTOMER_STATUS");

    /** Dimensions de segmentation des créances autorisées (clé API → colonne SQL). */
    public static final Map<String, String> BILLING_DIMENSIONS = Map.of(
            "aging",   "AGING_STATUS",
            "settle",  "SETTLE_STATUS",
            "status",  "BILL_STATUS",
            "type",    "BILL_TYPE",
            "method",  "PAYMENT_METHOD");

    /** Expression SQL : date métier reconstruite depuis un ID T24. */
    private static final String FT_DATE =
            "DATEADD(DAY, TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 5, 3)) - 1, " +
                    "DATEFROMPARTS(2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 3, 2)), 1, 1))";
    private static final String BILL_DATE =
            "DATEADD(DAY, TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 9, 3)) - 1, " +
                    "DATEFROMPARTS(2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 7, 2)), 1, 1))";

    private static final String FT_ID_FILTER   = "ORIGINAL_ID LIKE 'FT[0-9][0-9][0-9][0-9][0-9]%'";
    private static final String BILL_ID_FILTER = "ORIGINAL_ID LIKE 'AABILL[0-9][0-9][0-9][0-9][0-9]%'";

    /** Année extraite de l'ID FT (2000 + yy). */
    private static final String FT_YEAR =
            "2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 3, 2))";

    private final Map<String, Boolean> columnCache = new ConcurrentHashMap<>();

    /** La colonne existe-t-elle dans la base BI ? (résultat mis en cache) */
    public boolean hasColumn(String table, String column) {
        return columnCache.computeIfAbsent(table + "." + column, k -> {
            Integer n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            boolean present = n != null && n > 0;
            if (!present) {
                log.debug("Colonne absente (extraction non enrichie) : {}.{}", table, column);
            }
            return present;
        });
    }

    /** La table (satellite) existe-t-elle dans la base BI ? (résultat mis en cache) */
    public boolean tableExists(String table) {
        return columnCache.computeIfAbsent("TABLE::" + table, k -> {
            Integer n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys.tables WHERE name = ?",
                    Integer.class, table);
            boolean present = n != null && n > 0;
            if (!present) {
                log.debug("Table satellite absente (extraction non enrichie) : {}", table);
            }
            return present;
        });
    }

    /** État des colonnes « enrichies » attendues pour la page Flux. */
    public Map<String, Boolean> ftEnrichedColumnsStatus() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        for (String col : List.of("AMOUNT_DEBITED", "DEBIT_CURRENCY",
                "TRANSACTION_TYPE", "PROCESSING_DATE")) {
            status.put(col, hasColumn(FT_TABLE, col));
        }
        return status;
    }

    /** État des colonnes « enrichies » attendues pour la page Clients. */
    public Map<String, Boolean> customerEnrichedColumnsStatus() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        for (String col : List.of("SECTOR", "NATIONALITY", "GENDER",
                "TARGET", "CUSTOMER_STATUS")) {
            status.put(col, hasColumn(CUS_TABLE, col));
        }
        // Multivalué → normalisé par le Json_Flattener dans CUSTOMER#DATE_AC_OPND
        status.put("DATE_AC_OPND", tableExists(satellite(CUS_TABLE, "DATE_AC_OPND")));
        return status;
    }

    // ── Clients : socle ───────────────────────────────────────────────────────

    public long countClients() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM CUSTOMER", Long.class);
        return n == null ? 0 : n;
    }

    public Double averageAge() {
        return jdbc.queryForObject(
                "SELECT AVG(TRY_CONVERT(float, NULLIF(AGE, 'NULL'))) FROM CUSTOMER",
                Double.class);
    }

    public long countOfficers() {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT ACCOUNT_OFFICER) FROM CUSTOMER " +
                        "WHERE ACCOUNT_OFFICER IS NOT NULL AND ACCOUNT_OFFICER <> 'NULL'",
                Long.class);
        return n == null ? 0 : n;
    }

    /** Portefeuille par gestionnaire, trié décroissant. */
    public List<Map<String, Object>> portfolioByOfficer(int top) {
        return jdbc.queryForList("""
                SELECT TOP (?) ACCOUNT_OFFICER AS officer,
                       COUNT(*) AS nb,
                       AVG(TRY_CONVERT(float, NULLIF(AGE, 'NULL'))) AS age_moyen,
                       CAST(100.0 * COUNT(*) / SUM(COUNT(*)) OVER () AS float) AS part_pct
                FROM CUSTOMER
                WHERE ACCOUNT_OFFICER IS NOT NULL AND ACCOUNT_OFFICER <> 'NULL'
                GROUP BY ACCOUNT_OFFICER
                ORDER BY nb DESC
                """, top);
    }

    /** Répartition des clients par tranche d'âge (6 tranches, page Synthèse). */
    public List<Map<String, Object>> agePyramid() {
        return jdbc.queryForList("""
                SELECT tranche, ordre, COUNT(*) AS nb
                FROM (
                    SELECT CASE
                             WHEN a < 25              THEN N'< 25'
                             WHEN a BETWEEN 25 AND 34 THEN N'25–34'
                             WHEN a BETWEEN 35 AND 44 THEN N'35–44'
                             WHEN a BETWEEN 45 AND 54 THEN N'45–54'
                             WHEN a BETWEEN 55 AND 64 THEN N'55–64'
                             ELSE N'65 +'
                           END AS tranche,
                           CASE
                             WHEN a < 25              THEN 1
                             WHEN a BETWEEN 25 AND 34 THEN 2
                             WHEN a BETWEEN 35 AND 44 THEN 3
                             WHEN a BETWEEN 45 AND 54 THEN 4
                             WHEN a BETWEEN 55 AND 64 THEN 5
                             ELSE 6
                           END AS ordre
                    FROM (SELECT TRY_CONVERT(int, NULLIF(AGE, 'NULL')) AS a FROM CUSTOMER) x
                    WHERE a IS NOT NULL
                ) t
                GROUP BY tranche, ordre
                ORDER BY ordre
                """);
    }

    // ── Clients : page dédiée ─────────────────────────────────────────────────

    /** Histogramme des âges par pas de 5 ans (85+ regroupés). */
    public List<Map<String, Object>> ageHistogram() {
        return jdbc.queryForList("""
                SELECT band, COUNT(*) AS nb
                FROM (
                    SELECT CASE WHEN a >= 85 THEN 85 ELSE (a / 5) * 5 END AS band
                    FROM (SELECT TRY_CONVERT(int, NULLIF(AGE, 'NULL')) AS a FROM CUSTOMER) x
                    WHERE a IS NOT NULL AND a >= 0
                ) t
                GROUP BY band
                ORDER BY band
                """);
    }

    /** Répartition par génération, dérivée de l'année de naissance (BIRTH_INCORP_DATE). */
    public List<Map<String, Object>> generations() {
        return jdbc.queryForList("""
                SELECT gen AS label, ordre, COUNT(*) AS nb
                FROM (
                    SELECT CASE
                             WHEN y >= 1997 THEN N'Gén. Z (1997+)'
                             WHEN y >= 1981 THEN N'Millennials (1981–96)'
                             WHEN y >= 1965 THEN N'Gén. X (1965–80)'
                             WHEN y >= 1946 THEN N'Boomers (1946–64)'
                             ELSE               N'Avant 1946'
                           END AS gen,
                           CASE
                             WHEN y >= 1997 THEN 1
                             WHEN y >= 1981 THEN 2
                             WHEN y >= 1965 THEN 3
                             WHEN y >= 1946 THEN 4
                             ELSE 5
                           END AS ordre
                    FROM (SELECT TRY_CONVERT(int, LEFT(NULLIF(BIRTH_INCORP_DATE, 'NULL'), 4)) AS y
                          FROM CUSTOMER) x
                    WHERE y BETWEEN 1900 AND 2026
                ) t
                GROUP BY gen, ordre
                ORDER BY ordre
                """);
    }

    /** Part des clients dont l'âge est compris entre deux bornes (incluses). */
    public Double ageShare(int from, int to) {
        return jdbc.queryForObject("""
                SELECT 100.0 * SUM(CASE WHEN a BETWEEN ? AND ? THEN 1 ELSE 0 END) / COUNT(*)
                FROM (SELECT TRY_CONVERT(int, NULLIF(AGE, 'NULL')) AS a FROM CUSTOMER) x
                WHERE a IS NOT NULL
                """, Double.class, from, to);
    }

    /** Part des dossiers issus de la migration initiale (marqueur dans AUTHORISER). */
    public Double migrationShare() {
        return jdbc.queryForObject("""
                SELECT 100.0 * SUM(CASE WHEN AUTHORISER LIKE '%MIGRATION%' THEN 1 ELSE 0 END)
                       / COUNT(*)
                FROM CUSTOMER
                WHERE AUTHORISER IS NOT NULL AND AUTHORISER <> 'NULL'
                """, Double.class);
    }

    /**
     * Date d'entrée en relation par client : première valeur (MIN) du
     * satellite CUSTOMER#DATE_AC_OPND — un client peut porter plusieurs
     * dates d'ouverture de compte, la plus ancienne fait foi.
     */
    private static final String ACQ_FIRST_DATE = """
            SELECT s._id,
                   MIN(TRY_CONVERT(date, NULLIF(s.DATE_AC_OPND, 'NULL'), 112)) AS d
            FROM [CUSTOMER#DATE_AC_OPND] s
            GROUP BY s._id
            """;

    /** Années disponibles d'entrée en relation — vide tant que le satellite est absent. */
    public List<Integer> acquisitionYears() {
        if (!tableExists(satellite(CUS_TABLE, "DATE_AC_OPND"))) return List.of();
        return jdbc.queryForList("""
                SELECT DISTINCT YEAR(d) AS y
                FROM (%s) t
                WHERE d IS NOT NULL
                ORDER BY y DESC
                """.formatted(ACQ_FIRST_DATE), Integer.class);
    }

    /** Nouveaux clients par mois — vide tant que le satellite est absent. */
    public List<Map<String, Object>> newCustomersMonthly(int year) {
        if (!tableExists(satellite(CUS_TABLE, "DATE_AC_OPND"))) return List.of();
        return jdbc.queryForList("""
                SELECT MONTH(d) AS mois, COUNT(*) AS nb
                FROM (%s) t
                WHERE d IS NOT NULL AND (? = 0 OR YEAR(d) = ?)
                GROUP BY MONTH(d)
                ORDER BY mois
                """.formatted(ACQ_FIRST_DATE), year, year);
    }

    /** Nombre de nouveaux clients — null tant que le satellite est absent. */
    public Long countNewCustomers(int year) {
        if (!tableExists(satellite(CUS_TABLE, "DATE_AC_OPND"))) return null;
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM (%s) t
                WHERE d IS NOT NULL AND (? = 0 OR YEAR(d) = ?)
                """.formatted(ACQ_FIRST_DATE), Long.class, year, year);
    }

    /**
     * Segmentation des clients selon une dimension autorisée
     * (cf. {@link #CUSTOMER_DIMENSIONS}) — vide tant que la colonne est absente.
     */
    public List<Map<String, Object>> customerBreakdown(String dimensionKey, int top) {
        String column = CUSTOMER_DIMENSIONS.get(dimensionKey);
        if (column == null || !hasColumn(CUS_TABLE, column)) return List.of();
        return jdbc.queryForList("""
                SELECT TOP (?) COALESCE(NULLIF(NULLIF(%s, ''), 'NULL'), '—') AS label,
                       COUNT(*) AS nb
                FROM CUSTOMER
                GROUP BY COALESCE(NULLIF(NULLIF(%s, ''), 'NULL'), '—')
                ORDER BY nb DESC
                """.formatted(column, column), top);
    }

    // ── Virements : volumes ───────────────────────────────────────────────────

    /** Années présentes dans l'historique de virements (axe des filtres). */
    public List<Integer> transferYears() {
        return jdbc.queryForList(
                "SELECT DISTINCT " + FT_YEAR + " AS y FROM [" + FT_TABLE + "] " +
                        "WHERE " + FT_ID_FILTER + " ORDER BY y DESC",
                Integer.class);
    }

    /** Volumes mensuels des virements d'une année (année = 0 → toutes). */
    public List<Map<String, Object>> monthlyTransfers(int year) {
        return jdbc.queryForList("""
                SELECT MONTH(%s) AS mois, COUNT(*) AS nb
                FROM [%s]
                WHERE %s AND (? = 0 OR %s = ?)
                GROUP BY MONTH(%s)
                ORDER BY mois
                """.formatted(FT_DATE, FT_TABLE, FT_ID_FILTER, FT_YEAR, FT_DATE),
                year, year);
    }

    public long countTransfers(int year) {
        Long n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM [%s]
                WHERE %s AND (? = 0 OR %s = ?)
                """.formatted(FT_TABLE, FT_ID_FILTER, FT_YEAR),
                Long.class, year, year);
        return n == null ? 0 : n;
    }

    // ── Virements : montants (page Flux) ──────────────────────────────────────

    /** Somme des montants débités — null tant que AMOUNT_DEBITED absent. */
    public Double sumTransfers(int year) {
        if (!hasColumn(FT_TABLE, "AMOUNT_DEBITED")) return null;
        return jdbc.queryForObject("""
                SELECT SUM(TRY_CONVERT(decimal(18,3), AMOUNT_DEBITED)) FROM [%s]
                WHERE %s AND (? = 0 OR %s = ?)
                """.formatted(FT_TABLE, FT_ID_FILTER, FT_YEAR),
                Double.class, year, year);
    }

    /** Montants mensuels — liste vide tant que AMOUNT_DEBITED absent. */
    public List<Map<String, Object>> monthlyAmounts(int year) {
        if (!hasColumn(FT_TABLE, "AMOUNT_DEBITED")) return List.of();
        return jdbc.queryForList("""
                SELECT MONTH(%s) AS mois,
                       SUM(TRY_CONVERT(decimal(18,3), AMOUNT_DEBITED)) AS montant
                FROM [%s]
                WHERE %s AND (? = 0 OR %s = ?)
                GROUP BY MONTH(%s)
                ORDER BY mois
                """.formatted(FT_DATE, FT_TABLE, FT_ID_FILTER, FT_YEAR, FT_DATE),
                year, year);
    }

    /**
     * Répartition des flux par devise — vide tant que DEBIT_CURRENCY absente.
     * Le montant reste null si AMOUNT_DEBITED n'est pas encore extraite.
     */
    public List<Map<String, Object>> flowsByCurrency(int year) {
        if (!hasColumn(FT_TABLE, "DEBIT_CURRENCY")) return List.of();
        String amount = hasColumn(FT_TABLE, "AMOUNT_DEBITED")
                ? "SUM(TRY_CONVERT(decimal(18,3), AMOUNT_DEBITED))"
                : "CAST(NULL AS decimal(18,3))";
        return jdbc.queryForList("""
                SELECT COALESCE(NULLIF(NULLIF(DEBIT_CURRENCY, ''), 'NULL'), 'TND') AS label,
                       COUNT(*) AS nb, %s AS montant
                FROM [%s]
                WHERE %s AND (? = 0 OR %s = ?)
                GROUP BY COALESCE(NULLIF(NULLIF(DEBIT_CURRENCY, ''), 'NULL'), 'TND')
                ORDER BY nb DESC
                """.formatted(amount, FT_TABLE, FT_ID_FILTER, FT_YEAR),
                year, year);
    }

    /** Répartition par type d'opération — vide tant que TRANSACTION_TYPE absente. */
    public List<Map<String, Object>> flowsByType(int year) {
        if (!hasColumn(FT_TABLE, "TRANSACTION_TYPE")) return List.of();
        String amount = hasColumn(FT_TABLE, "AMOUNT_DEBITED")
                ? "SUM(TRY_CONVERT(decimal(18,3), AMOUNT_DEBITED))"
                : "CAST(NULL AS decimal(18,3))";
        return jdbc.queryForList("""
                SELECT COALESCE(NULLIF(NULLIF(TRANSACTION_TYPE, ''), 'NULL'), '—') AS label,
                       COUNT(*) AS nb, %s AS montant
                FROM [%s]
                WHERE %s AND (? = 0 OR %s = ?)
                GROUP BY COALESCE(NULLIF(NULLIF(TRANSACTION_TYPE, ''), 'NULL'), '—')
                ORDER BY nb DESC
                """.formatted(amount, FT_TABLE, FT_ID_FILTER, FT_YEAR),
                year, year);
    }

    /**
     * Heatmap d'activité mois × jour de semaine — fonctionne dès aujourd'hui
     * (date reconstruite depuis l'ID). 0 = lundi … 6 = dimanche, indépendant
     * de DATEFIRST grâce à DATEDIFF depuis le 1900-01-01 (un lundi).
     */
    public List<Map<String, Object>> activityHeatmap(int year) {
        return jdbc.queryForList("""
                SELECT MONTH(d) AS mois,
                       (DATEDIFF(DAY, 0, d) %% 7) AS jour,
                       COUNT(*) AS nb
                FROM (
                    SELECT %s AS d FROM [%s] WHERE %s
                ) src
                WHERE d IS NOT NULL AND (? = 0 OR YEAR(d) = ?)
                GROUP BY MONTH(d), (DATEDIFF(DAY, 0, d) %% 7)
                """.formatted(FT_DATE, FT_TABLE, FT_ID_FILTER),
                year, year);
    }

    // ── Facturation (AA_BILL_DETAILS) ─────────────────────────────────────────

    /** État des colonnes « enrichies » attendues pour la page Créances. */
    public Map<String, Boolean> billingEnrichedColumnsStatus() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        // Multivalués → normalisés par le Json_Flattener dans AA_BILL_DETAILS#CHAMP
        for (String field : List.of("SETTLE_STATUS", "AGING_STATUS", "BILL_TYPE")) {
            status.put(field, tableExists(satellite(BILL_TABLE, field)));
        }
        // Monovalués → colonnes directes de la table principale
        for (String col : List.of("OR_TOTAL_AMOUNT", "OS_TOTAL_AMOUNT",
                "PAYMENT_DATE", "ACTUAL_PAY_DATE")) {
            status.put(col, hasColumn(BILL_TABLE, col));
        }
        return status;
    }

    /** Années présentes dans l'historique de facturation (axe des filtres). */
    public List<Integer> billYears() {
        return jdbc.queryForList(
                "SELECT DISTINCT 2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 7, 2)) AS y " +
                        "FROM AA_BILL_DETAILS WHERE " + BILL_ID_FILTER + " ORDER BY y DESC",
                Integer.class);
    }

    public long countBills(int year) {
        Long n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM AA_BILL_DETAILS
                WHERE %s AND (? = 0 OR 2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 7, 2)) = ?)
                """.formatted(BILL_ID_FILTER),
                Long.class, year, year);
        return n == null ? 0 : n;
    }

    public List<Map<String, Object>> monthlyBills(int year) {
        return jdbc.queryForList("""
                SELECT MONTH(%s) AS mois, COUNT(*) AS nb
                FROM AA_BILL_DETAILS
                WHERE %s AND (? = 0 OR 2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 7, 2)) = ?)
                GROUP BY MONTH(%s)
                ORDER BY mois
                """.formatted(BILL_DATE, BILL_ID_FILTER, BILL_DATE),
                year, year);
    }

    /** Montants facturés mensuels (OR_TOTAL_AMOUNT) — vide tant qu'absente. */
    public List<Map<String, Object>> monthlyBillAmounts(int year) {
        if (!hasColumn(BILL_TABLE, "OR_TOTAL_AMOUNT")) return List.of();
        return jdbc.queryForList("""
                SELECT MONTH(%s) AS mois,
                       SUM(TRY_CONVERT(decimal(18,3), OR_TOTAL_AMOUNT)) AS montant
                FROM AA_BILL_DETAILS
                WHERE %s AND (? = 0 OR 2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 7, 2)) = ?)
                GROUP BY MONTH(%s)
                ORDER BY mois
                """.formatted(BILL_DATE, BILL_ID_FILTER, BILL_DATE),
                year, year);
    }

    /** Montant total facturé (Σ OR_TOTAL_AMOUNT) — null tant qu'absente. */
    public Double billedAmount(int year) {
        return sumBillColumn("OR_TOTAL_AMOUNT", year);
    }

    /** Encours total (Σ OS_TOTAL_AMOUNT) — null tant qu'absente. */
    public Double outstandingAmount(int year) {
        return sumBillColumn("OS_TOTAL_AMOUNT", year);
    }

    private Double sumBillColumn(String column, int year) {
        // Colonne issue d'une liste fermée interne — jamais d'entrée utilisateur.
        if (!hasColumn(BILL_TABLE, column)) return null;
        return jdbc.queryForObject("""
                SELECT SUM(TRY_CONVERT(decimal(18,3), %s)) FROM AA_BILL_DETAILS
                WHERE %s AND (? = 0 OR 2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 7, 2)) = ?)
                """.formatted(column, BILL_ID_FILTER),
                Double.class, year, year);
    }

    /**
     * Segmentation des créances selon une dimension autorisée
     * (cf. {@link #BILLING_DIMENSIONS}). Les dimensions sont des champs
     * multivalués : la valeur est lue dans la table satellite
     * AA_BILL_DETAILS#CHAMP (première multivaleur), jointe à la table
     * principale pour le filtre année et l'encours.
     * Vide tant que le satellite est absent.
     */
    public List<Map<String, Object>> billingBreakdown(String dimensionKey, int year, int top) {
        String field = BILLING_DIMENSIONS.get(dimensionKey);
        if (field == null || !tableExists(satellite(BILL_TABLE, field))) return List.of();
        String amount = hasColumn(BILL_TABLE, "OS_TOTAL_AMOUNT")
                ? "SUM(TRY_CONVERT(decimal(18,3), b.OS_TOTAL_AMOUNT))"
                : "CAST(NULL AS decimal(18,3))";
        return jdbc.queryForList("""
                SELECT TOP (?) COALESCE(NULLIF(NULLIF(s.val, ''), 'NULL'), '—') AS label,
                       COUNT(*) AS nb, %s AS montant
                FROM (
                    -- DISTINCT : le Json_Flattener peut réinsérer les mêmes lignes
                    -- à chaque run ; une facture = une valeur, jamais plus
                    SELECT DISTINCT s._id, s.[%s] AS val
                    FROM [%s] s
                    WHERE %s
                ) s
                JOIN [%s] b ON b._id = s._id
                WHERE b.%s
                  AND (? = 0 OR 2000 + TRY_CONVERT(int, SUBSTRING(b.ORIGINAL_ID, 7, 2)) = ?)
                GROUP BY COALESCE(NULLIF(NULLIF(s.val, ''), 'NULL'), '—')
                ORDER BY nb DESC
                """.formatted(amount, field, satellite(BILL_TABLE, field),
                        FIRST_MV, BILL_TABLE, BILL_ID_FILTER),
                top, year, year);
    }

    /**
     * Délai moyen de règlement en jours (échéance → paiement effectif) —
     * null tant que PAYMENT_DATE et ACTUAL_PAY_DATE ne sont pas extraites.
     * Positif = payé après l'échéance, négatif = payé en avance.
     */
    public Double avgSettlementDelay(int year) {
        if (!hasColumn(BILL_TABLE, "PAYMENT_DATE")
                || !hasColumn(BILL_TABLE, "ACTUAL_PAY_DATE")) return null;
        return jdbc.queryForObject("""
                SELECT AVG(CAST(DATEDIFF(DAY, echeance, paiement) AS float))
                FROM (
                    SELECT TRY_CONVERT(date, NULLIF(PAYMENT_DATE, 'NULL'), 112)    AS echeance,
                           TRY_CONVERT(date, NULLIF(ACTUAL_PAY_DATE, 'NULL'), 112) AS paiement
                    FROM AA_BILL_DETAILS
                    WHERE %s AND (? = 0 OR 2000 + TRY_CONVERT(int, SUBSTRING(ORIGINAL_ID, 7, 2)) = ?)
                ) t
                WHERE echeance IS NOT NULL AND paiement IS NOT NULL
                """.formatted(BILL_ID_FILTER),
                Double.class, year, year);
    }

    /** Vieillissement des créances (compat page Synthèse) — vide tant que le satellite est absent. */
    public List<Map<String, Object>> billingAging() {
        if (!tableExists(satellite(BILL_TABLE, "AGING_STATUS"))) {
            return List.of();
        }
        return jdbc.queryForList("""
                SELECT s.AGING_STATUS AS label, COUNT(DISTINCT s._id) AS nb
                FROM [%s] s
                WHERE %s
                  AND s.AGING_STATUS IS NOT NULL AND s.AGING_STATUS <> 'NULL'
                GROUP BY s.AGING_STATUS
                ORDER BY nb DESC
                """.formatted(satellite(BILL_TABLE, "AGING_STATUS"), FIRST_MV));
    }
}
