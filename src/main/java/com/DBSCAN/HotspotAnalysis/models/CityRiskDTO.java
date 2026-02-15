package com.DBSCAN.HotspotAnalysis.models;

public class CityRiskDTO {
    private String provinceName;
    private String cityName;
    private String score;
    private Double scoreValue;
    private String riskLevel;
    private Double lat;
    private Double lng;

    public CityRiskDTO(String provinceName, String cityName, String score, Double scoreValue, String riskLevel, Double lat, Double lng) {
        this.provinceName = provinceName;
        this.cityName = cityName;
        this.score = score;
        this.scoreValue = scoreValue;
        this.riskLevel = riskLevel;
        this.lat = lat;
        this.lng = lng;
    }

    // Getters
    public String getProvinceName() { return provinceName; }
    public String getCityName() { return cityName; }
    public String getScore() { return score; }
    public Double getScoreValue() { return scoreValue; }
    public String getRiskLevel() { return riskLevel; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
}