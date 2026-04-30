package com.t24extractor.repository.t24_repo;


import com.t24extractor.model.FileControl;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileControlRepository extends CrudRepository<FileControl, String> {

  Optional<FileControl> findFileControlByRecid(String recid);
}