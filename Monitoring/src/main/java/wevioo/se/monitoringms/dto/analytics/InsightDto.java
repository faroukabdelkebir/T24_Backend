package wevioo.se.monitoringms.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Enseignement calculé automatiquement à partir des données —
 * affiché dans le bandeau « insight » de la page Analytics.
 *
 * La localisation se fait côté client : le backend n'envoie plus de texte
 * traduit mais une clé i18n ({@code detailKey}) et ses paramètres
 * d'interpolation ({@code params}). Le champ {@code detail} est conservé
 * pour les cas sans traduction (valeur brute, ex. code devise).
 */
@Data
@Builder
public class InsightDto {
    private String type;                 // LEADER | CONCENTRATION | PIC_ACTIVITE
    private String titleKey;             // clé i18n du titre
    private String value;                // valeur principale (ex. « 457 »)
    private String detail;               // complément brut sans traduction (fallback)
    private String detailKey;            // clé i18n du complément (interpolable)
    private Map<String, Object> params;  // paramètres d'interpolation du complément
}
