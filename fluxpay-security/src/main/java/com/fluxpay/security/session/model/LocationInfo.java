package com.fluxpay.security.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationInfo implements Serializable {
    private String country;
    private String region;
    private String city;
    private String timezone;
    private Double latitude;
    private Double longitude;
}

