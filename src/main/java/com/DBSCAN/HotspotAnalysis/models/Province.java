package com.DBSCAN.HotspotAnalysis.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "province")
@Data // Use Lombok or manually generate Getters/Setters
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer province_id;

    @Column(name = "province_en", nullable = false, unique = true)
    private String province_en;
}