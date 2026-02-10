package com.DBSCAN.HotspotAnalysis.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_data")
public class IncidentData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne
    @JoinColumn(name = "case_type_id")
    private CaseType caseType;

    private Double latitude;
    private Double longitude;

    @Column(name = "formatted_datetime")
    private LocalDateTime formattedDatetime;

    // Helper for UI ID Formatting (C000000001)
    public String getFormattedId() {
        if (this.id == null) return "C---------";
        return String.format("C%09d", this.id);
    }

    // Standard Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
    public CaseType getCaseType() { return caseType; }
    public void setCaseType(CaseType caseType) { this.caseType = caseType; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDateTime getFormattedDatetime() { return formattedDatetime; }
    public void setFormattedDatetime(LocalDateTime formattedDatetime) { this.formattedDatetime = formattedDatetime; }
}