package com.t24extractor.repository.bi_repos;


import com.t24extractor.model.MetaDataTable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaDataRepository extends CrudRepository<MetaDataTable, String> {

}
