package com.t24extractor.tasklet;

import com.t24extractor.model.FileControl;
import com.t24extractor.repository.t24_repo.FileControlRepository;
import com.t24extractor.utils.Renamer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class FileControlServiceImpl implements IFileControlService{


    @Autowired
    FileControlRepository fileControlRepository;

    private final Renamer renamer = new Renamer();

    @Override
    public String classification(String recid) {

        FileControl fileControl = fileControlRepository.findFileControlByRecid(renamer.ssRenamer(recid)).orElseThrow(() -> new NoSuchElementException("Veuillez verifier que la table F_FILE_CONTROL est alimentee correctement"));
        return "INT".equalsIgnoreCase(fileControl.classification()) ? "F" : "FBNK";
    }
}
