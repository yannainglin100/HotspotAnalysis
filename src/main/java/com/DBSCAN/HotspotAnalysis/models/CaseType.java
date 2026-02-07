package com.DBSCAN.HotspotAnalysis.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "case_type")
@Data
public class CaseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer case_type_id;

    @Column(name = "case_type_en", nullable = false)
    private String case_type_en;

    @Column(name = "weight")
    private Integer weight;
}