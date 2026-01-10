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
public class DeviceInfo implements Serializable {
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String os;
    private String osVersion;
    private String browser;
    private String browserVersion;
}

