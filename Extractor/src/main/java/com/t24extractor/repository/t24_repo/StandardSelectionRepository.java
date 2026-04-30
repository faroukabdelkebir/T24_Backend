package com.t24extractor.repository.t24_repo;

import com.t24extractor.model.StandardSelectionRecord;
import com.t24extractor.model.T24Table;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StandardSelectionRepository extends CrudRepository<StandardSelectionRecord, String> {
    @Override
    @Cacheable(value = "ss")
    Optional<StandardSelectionRecord> findById(String t24TableName);
}
