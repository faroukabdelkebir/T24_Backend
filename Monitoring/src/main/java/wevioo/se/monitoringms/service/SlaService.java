package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wevioo.se.monitoringms.dto.SlaStatusDto;
import wevioo.se.monitoringms.model.BatchJobExecution;
import wevioo.se.monitoringms.repository.BatchExecutionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Suivi des SLAs — chaque job doit se terminer avant une heure définie.
 *
 * Configuration dans application.properties :
 *   monitor.sla.customer=07:00
 *   monitor.sla.ft-his=07:30
 *   monitor.sla.aa-bill=07:30
 *   monitor.sla.main=08:00
 *
 * Un traitement dépasse son SLA si son heure de fin dépasse l'heure limite.
 * S'il est toujours en cours après cette heure, le SLA est également dépassé.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaService {

    private final BatchExecutionRepository batchRepo;

    @Value("${monitor.sla.customer:07:00}")
    private String slaCustomer;

    @Value("${monitor.sla.ft-his:07:30}")
    private String slaFtHis;

    @Value("${monitor.sla.aa-bill:07:30}")
    private String slaAaBill;

    @Value("${monitor.sla.main:08:00}")
    private String slaMain;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Statut SLA pour tous les jobs configurés.
     */
    public List<SlaStatusDto> getAllSlaStatuses() {
        Map<String, String> slaMap = buildSlaMap();

        return slaMap.entrySet().stream()
                .map(e -> computeSlaStatus(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * Statut SLA pour un job spécifique.
     */
    public SlaStatusDto getSlaStatus(String jobName) {
        Map<String, String> slaMap = buildSlaMap();
        String slaTime = slaMap.getOrDefault(jobName, "08:00");
        return computeSlaStatus(jobName, slaTime);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, String> buildSlaMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("T24",               slaMain);
        map.put("CUSTOMER.JOB",      slaCustomer);
        map.put("AA.BILL.DETAILS.JOB", slaAaBill);
        map.put("FT.HIS.JOB",        slaFtHis);
        return map;
    }

    private SlaStatusDto computeSlaStatus(String jobName, String slaTimeStr) {
        LocalTime deadline = LocalTime.parse(slaTimeStr, TIME_FMT);

        Optional<BatchJobExecution> lastSuccessful =
                batchRepo.findLastSuccessfulByJobName(jobName);

        // Pas d'exécution récente aujourd'hui
        if (lastSuccessful.isEmpty()) {
            List<BatchJobExecution> running = batchRepo.findRunning().stream()
                    .filter(j -> j.getJobName().equals(jobName))
                    .toList();

            if (!running.isEmpty()) {
                // Job en cours — vérifier si la deadline est déjà passée
                LocalDateTime now = LocalDateTime.now();
                boolean breached = now.toLocalTime().isAfter(deadline);
                long lateBy = breached
                        ? Duration.between(deadline.atDate(now.toLocalDate()), now).getSeconds()
                        : 0;

                return SlaStatusDto.builder()
                        .jobName(jobName)
                        .slaDeadline(deadline)
                        .slaBreached(breached)
                        .lateBySeconds(lateBy)
                        .status(breached ? "Délai dépassé" : "En cours")
                        .build();
            }

            return SlaStatusDto.builder()
                    .jobName(jobName)
                    .slaDeadline(deadline)
                    .slaBreached(false)
                    .lateBySeconds(0)
                    .status("Non exécuté")
                    .build();
        }

        BatchJobExecution last = lastSuccessful.get();
        LocalDateTime completionTime = last.getEndTime();

        if (completionTime == null) {
            return SlaStatusDto.builder()
                    .jobName(jobName).slaDeadline(deadline).status("Non exécuté").build();
        }

        LocalTime completionLocal = completionTime.toLocalTime();
        boolean breached = completionLocal.isAfter(deadline);

        long lateBy = 0;
        if (breached) {
            lateBy = Duration.between(
                    deadline.atDate(completionTime.toLocalDate()),
                    completionTime).getSeconds();
        }

        return SlaStatusDto.builder()
                .jobName(jobName)
                .slaDeadline(deadline)
                .lastCompletionTime(completionTime)
                .slaBreached(breached)
                .lateBySeconds(lateBy)
                .status(breached ? "Délai dépassé" : "Dans les délais")
                .build();
    }
}

