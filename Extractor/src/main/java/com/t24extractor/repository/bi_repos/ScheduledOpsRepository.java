package com.t24extractor.repository.bi_repos;


import com.t24extractor.model.ScheduledOps;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.t24extractor.commons.Constants.SCHEDULED_OPS_QUERY;

@Repository
public interface ScheduledOpsRepository extends CrudRepository<ScheduledOps, Long> {

    @Query(SCHEDULED_OPS_QUERY)
    List<ScheduledOps> findAllByTable(@Param("table") String tableName);

}
