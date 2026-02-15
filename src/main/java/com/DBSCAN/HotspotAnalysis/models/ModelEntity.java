package com.DBSCAN.HotspotAnalysis.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "model")
public class ModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer model_id;

    private String model_name;
    private String provinces; // "0" or specific ID
    private String years;     // "0" or specific year
    private Double epsilon;
    private Integer minpts;
    private Integer total_clusters;
    private Double noise_percentage;
    private Double dbi;
    private LocalDateTime trained_date_time;
    private Boolean select_this;
    private Integer points_processed;

    // Getters and Setters
    public Integer getModel_id() { return model_id; }
    public void setModel_id(Integer model_id) { this.model_id = model_id; }
    public String getModel_name() { return model_name; }
    public void setModel_name(String model_name) { this.model_name = model_name; }
    public String getProvinces() { return provinces; }
    public void setProvinces(String provinces) { this.provinces = provinces; }
    public String getYears() { return years; }
    public void setYears(String years) { this.years = years; }
    public Double getEpsilon() { return epsilon; }
    public void setEpsilon(Double epsilon) { this.epsilon = epsilon; }
    public Integer getMinpts() { return minpts; }
    public void setMinpts(Integer minpts) { this.minpts = minpts; }
    public Double getDbi() { return dbi; }
    public void setDbi(Double dbi) { this.dbi = dbi; }
    public LocalDateTime getTrained_date_time() { return trained_date_time; }
    public void setTrained_date_time(LocalDateTime trained_date_time) { this.trained_date_time = trained_date_time; }
    public Boolean getSelect_this() { return select_this; }
    public void setSelect_this(Boolean select_this) { this.select_this = select_this; }
}