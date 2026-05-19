package com.t24extractor.repository.t24_repo;

import com.t24extractor.model.TafjVoc;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;

public interface TafjVocRepository extends CrudRepository<TafjVoc, String> {
    Optional<TafjVoc> findTafjVocByRecid(String recid);
    @Cacheable(value = "tafj")
    Optional<TafjVoc> findTafjVocByOrclFileName(String orclFileName);
}
