package com.t24extractor.repository.t24_repo;

import com.t24extractor.model.CobStatus;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CobRepository extends CrudRepository<CobStatus, String> {

  Optional<CobStatus> findByRecid(String recid);
}
