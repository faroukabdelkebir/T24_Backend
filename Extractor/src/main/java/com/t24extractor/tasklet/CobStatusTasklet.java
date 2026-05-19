package com.t24extractor.tasklet;

import com.t24extractor.exception.DataNotFoundException;
import com.t24extractor.repository.t24_repo.CobRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.t24extractor.commons.Constants.*;

/**
 * Cette tasklet prend en charge de vérifier si le COB est en état STOP
 * Si c'est le cas, l'extracteur se démarre pour vérifier s'il y a des données à extraire.
 */
@Component
@Slf4j
public class CobStatusTasklet implements Tasklet {
    /**
     * CobRepository permettant de requeter la table TSA_SERVICE
     */
    CobRepository cobRepository;



    @Autowired
    public CobStatusTasklet(CobRepository cobRepository) {
        this.cobRepository = cobRepository;
    }

    @Override
    public RepeatStatus execute(@NonNull StepContribution stepContribution, @NonNull ChunkContext chunkContext) throws Exception {
        log.info("*-----------------* Vérification si le COB est en état d'arrêt *-----------------*");


        log.info("*-----------------* Lecture du champ 'SERVICE_CONTROL' de la table TSA_SERVICE *-----------------*");
        String serviceControl = cobRepository.findById(COB_RECID)
                .orElseThrow(() ->
                        new DataNotFoundException("Le champ 'SERVICE_CONTROL' est vide. Prière de vérifier si la base T24 est bien chargée.")
                )
                .serviceControl();
        /*
         * Si la variable serviceControl est "STOP", l'extracteur sera déclenché.
         * Sinon, le job se quittera
         */
        if (COB_STOPPED.equals(serviceControl)) {
            log.info("*-----------------* Le COB est en état d'arrêt, procéder à l'étape  *-----------------*");
        } else {
            stepContribution.setExitStatus(new ExitStatus(COB_RUNNING));
            stepContribution.getStepExecution().getJobExecution().setExitStatus(new ExitStatus(COB_RUNNING));
            log.info("*-----------------* Le COB est en état d'exécution, l'extracteur s'arrêtera en quelques mins  *-----------------*");
        }
        return RepeatStatus.FINISHED;
    }
}
