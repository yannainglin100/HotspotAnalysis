//package com.DBSCAN.HotspotAnalysis.models;
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Entity
//@Table(name = "province")
//@Data // Use Lombok or manually generate Getters/Setters
//public class Province {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer province_id;
//
//    @Column(name = "province_en", nullable = false, unique = true)
//    private String province_en;
//}
package com.DBSCAN.HotspotAnalysis.models;

import jakarta.persistence.*;

@Entity
@Table(name = "province")
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer province_id;

    private String province_en;

    // Manual Getter for the ID
    public Integer getProvince_id() {
        return province_id;
    }

    public void setProvince_id(Integer province_id) {
        this.province_id = province_id;
    }

    public String getProvince_en() { return province_en; }
    public void setProvince_en(String province_en) { this.province_en = province_en; }
}