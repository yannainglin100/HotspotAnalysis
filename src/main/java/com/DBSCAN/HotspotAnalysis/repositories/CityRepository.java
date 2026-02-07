package com.DBSCAN.HotspotAnalysis.repositories;

import com.DBSCAN.HotspotAnalysis.models.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {

    // Fetch cities by province ID
    @Query(value = "SELECT * FROM city WHERE province_id = :pId ORDER BY city_id ASC", nativeQuery = true)
    List<City> findByProvinceId(@Param("pId") Integer pId);

    // Fetch cities where province is NULL (No Province)
    @Query(value = "SELECT * FROM city WHERE province_id IS NULL ORDER BY city_id ASC", nativeQuery = true)
    List<City> findByProvinceIsNull();
}