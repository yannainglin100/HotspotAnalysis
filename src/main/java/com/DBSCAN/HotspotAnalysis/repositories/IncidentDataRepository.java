package com.DBSCAN.HotspotAnalysis.repositories;

import com.DBSCAN.HotspotAnalysis.models.IncidentData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IncidentDataRepository extends JpaRepository<IncidentData, Long> {

    @Query(value = "SELECT i.* FROM incident_data i " +
            "JOIN city c ON i.city_id = c.city_id " +
            "WHERE (:pId = 0 OR c.province_id = :pId OR (c.province_id IS NULL AND :pId = 0)) " +
            "AND (i.city_id = :cId) " +
            "AND (i.case_type_id = :ctId) " +
            "AND (EXTRACT(YEAR FROM i.formatted_datetime) = :year) " +
            "ORDER BY i.formatted_datetime DESC",
            countQuery = "SELECT count(*) FROM incident_data i " +
                    "JOIN city c ON i.city_id = c.city_id " +
                    "WHERE (:pId = 0 OR c.province_id = :pId OR (c.province_id IS NULL AND :pId = 0)) " +
                    "AND (i.city_id = :cId) " +
                    "AND (i.case_type_id = :ctId) " +
                    "AND (EXTRACT(YEAR FROM i.formatted_datetime) = :year)",
            nativeQuery = true)
    Page<IncidentData> findFilteredPage(@Param("pId") Integer pId,
                                        @Param("cId") Integer cId,
                                        @Param("ctId") Integer ctId,
                                        @Param("year") Integer year,
                                        Pageable pageable);

    @Query(value = "SELECT DISTINCT EXTRACT(YEAR FROM formatted_datetime) FROM incident_data ORDER BY 1 DESC", nativeQuery = true)
    List<Object> findAllYears();
}