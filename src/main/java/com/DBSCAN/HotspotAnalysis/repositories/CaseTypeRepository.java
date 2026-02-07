package com.DBSCAN.HotspotAnalysis.repositories;

import com.DBSCAN.HotspotAnalysis.models.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Integer> {

    @Query(value = "SELECT * FROM case_type \n" +
            "ORDER BY weight DESC, case_type_id ASC", nativeQuery = true)
    List<CaseType> findAllOrderedById();
}