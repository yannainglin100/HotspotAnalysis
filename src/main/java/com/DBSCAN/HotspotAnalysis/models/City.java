package com.DBSCAN.HotspotAnalysis.models;

import jakarta.persistence.*;

@Entity
@Table(name = "city")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer city_id;

    @Column(name = "city_en")
    private String city_en;

    @Column(name = "population")
    private Long population;

    @ManyToOne
    @JoinColumn(name = "province_id")
    private Province province;

    // --- MANUAL GETTERS AND SETTERS (Fixes the Red Text) ---

    public Integer getCity_id() { return city_id; }
    public void setCity_id(Integer city_id) { this.city_id = city_id; }

    public String getCity_en() { return city_en; }
    public void setCity_en(String city_en) { this.city_en = city_en; }

    public Long getPopulation() { return population; }
    public void setPopulation(Long population) { this.population = population; }

    public Province getProvince() { return province; }
    public void setProvince(Province province) { this.province = province; }
}