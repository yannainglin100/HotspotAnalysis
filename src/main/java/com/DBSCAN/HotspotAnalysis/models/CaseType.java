package com.DBSCAN.HotspotAnalysis.models;

import jakarta.persistence.*;

@Entity
@Table(name = "case_type")
public class CaseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer case_type_id;

    private String case_type_en;
    private Integer weight;

    public Integer getCase_type_id() {
        return case_type_id;
    }

    public void setCase_type_id(Integer case_type_id) {
        this.case_type_id = case_type_id;
    }

    public String getCase_type_en() {
        return case_type_en;
    }

    public void setCase_type_en(String case_type_en) {
        this.case_type_en = case_type_en;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}