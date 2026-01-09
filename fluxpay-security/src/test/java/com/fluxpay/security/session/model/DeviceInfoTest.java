package com.fluxpay.security.session.model;

import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceInfoTest {

    @Test
    void builder_ShouldCreateDeviceInfo() {
        DeviceInfo deviceInfo = DeviceInfo.builder()
                .deviceId("device-123")
                .deviceType("desktop")
                .os("Windows")
                .osVersion("10")
                .browser("Chrome")
                .browserVersion("120")
                .build();

        assertThat(deviceInfo).isNotNull();
        assertThat(deviceInfo.getDeviceType()).isEqualTo("desktop");
        assertThat(deviceInfo.getBrowser()).isEqualTo("Chrome");
    }

    @Test
    void setters_ShouldUpdateFields() {
        DeviceInfo deviceInfo = SessionTestDataFactory.createDeviceInfo();

        deviceInfo.setDeviceType("mobile");
        deviceInfo.setOs("Android");

        assertThat(deviceInfo.getDeviceType()).isEqualTo("mobile");
        assertThat(deviceInfo.getOs()).isEqualTo("Android");
    }

    @Test
    void noArgsConstructor_ShouldWork() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId("test-device");

        assertThat(deviceInfo.getDeviceId()).isEqualTo("test-device");
    }

    @Test
    void allFieldsCanBeSet() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId("id");
        deviceInfo.setDeviceType("tablet");
        deviceInfo.setDeviceName("iPad");
        deviceInfo.setOs("iOS");
        deviceInfo.setOsVersion("16");
        deviceInfo.setBrowser("Safari");
        deviceInfo.setBrowserVersion("16");

        assertThat(deviceInfo.getDeviceId()).isEqualTo("id");
        assertThat(deviceInfo.getDeviceType()).isEqualTo("tablet");
        assertThat(deviceInfo.getDeviceName()).isEqualTo("iPad");
        assertThat(deviceInfo.getOs()).isEqualTo("iOS");
        assertThat(deviceInfo.getOsVersion()).isEqualTo("16");
        assertThat(deviceInfo.getBrowser()).isEqualTo("Safari");
        assertThat(deviceInfo.getBrowserVersion()).isEqualTo("16");
    }
}


