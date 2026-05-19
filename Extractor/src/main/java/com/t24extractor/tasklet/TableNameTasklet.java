package com.t24extractor.tasklet;

import com.t24extractor.model.TafjVoc;
import com.t24extractor.repository.t24_repo.TafjVocRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * Ce tasklet sert à requeter les noms des tables T24 sur la base de données (Oracle)
 * La logique de requete se commence par determiner la classification de la table T24 si elle est F ou FBNK, et se termine par la requête sur TAFJ_VOC
 */
@Slf4j
@Component
public class TableNameTasklet implements Tasklet {

    private final TafjVocRepository tafjVocRepository;

    private final FileControlServiceImpl fileControlService;

    public TableNameTasklet(TafjVocRepository tafjVocRepository, FileControlServiceImpl fileControlService) {
        this.tafjVocRepository = tafjVocRepository;
        this.fileControlService = fileControlService;
    }

    @Setter
    String t24TableName;

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) throws Exception {

        log.debug("T24 Name {}", this.t24TableName);

        /*
            Concatenation entre la classification & le nom de la table T24
            Ex: Classification= FBNK. + t24Name= ACCOUNT ==> FBNK.ACCOUNT
         */

        log.debug("Le retour de Service Classification est {}", String.join(".", fileControlService.classification(this.t24TableName), this.t24TableName));
        String t24SelectionName = String.join(".", fileControlService.classification(this.t24TableName), this.t24TableName);


        /*
            Le correspondant de la table T24 sur Oracle
         */
       TafjVoc tafjVoc =  tafjVocRepository.findTafjVocByRecid(t24SelectionName)
                .orElseThrow(() -> new NoSuchElementException(String.format("Veuillez verifier que la table %s existe.", t24SelectionName)));

        /*
            Le singleMult des donnees T24 sur Oracle(Blob, XML, Clob)
         */

        log.info(String.valueOf(tafjVoc));
        String dbFileName = tafjVoc.orclFileName();
        char isBlob = tafjVoc.isBlob().charAt(0);


        /*
          Passer les variables aux autres steps à travers le contexte.
         */
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("Table_Name", dbFileName);

        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("Type", isBlob);

        return RepeatStatus.FINISHED;
    }

}
