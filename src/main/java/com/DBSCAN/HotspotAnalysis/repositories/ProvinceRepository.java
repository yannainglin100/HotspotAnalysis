package com.DBSCAN.HotspotAnalysis.repositories;

import com.DBSCAN.HotspotAnalysis.models.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {

    // Plain, simple SQL. No Spring "magic" involved.
    @Query(value = "SELECT * FROM province ORDER BY province_id ASC", nativeQuery = true)
    List<Province> findAllOrderedById();
}