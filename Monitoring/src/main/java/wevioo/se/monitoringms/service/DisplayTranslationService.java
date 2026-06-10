package wevioo.se.monitoringms.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Traduit les valeurs techniques destinées à être affichées sans modifier
 * les noms de classes, d'attributs ou les valeurs stockées en base.
 */
@Service
public class DisplayTranslationService {

    private static final Map<String, String> STATUSES = Map.ofEntries(
            Map.entry("ABANDONED", "Abandonné"),
            Map.entry("COMPLETED", "Terminé"),
            Map.entry("EXECUTING", "En cours d'exécution"),
            Map.entry("FAILED", "Échec"),
            Map.entry("NOOP", "Aucune action"),
            Map.entry("STARTED", "En cours"),
            Map.entry("STARTING", "Démarrage"),
            Map.entry("STOPPED", "Arrêté"),
            Map.entry("STOPPING", "Arrêt en cours"),
            Map.entry("UNKNOWN", "Inconnu")
    );

    public String translateStatus(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return STATUSES.getOrDefault(value.toUpperCase(), value);
    }
}
