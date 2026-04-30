package com.t24extractor.repository.bi_repos;


import com.t24extractor.model.Stats;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsRepository extends CrudRepository<Stats, Long> {
}
