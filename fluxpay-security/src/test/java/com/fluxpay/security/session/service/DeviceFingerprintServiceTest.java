package com.fluxpay.security.session.service;

import com.fluxpay.security.session.model.DeviceInfo;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceFingerprintServiceTest {

    @InjectMocks
    private DeviceFingerprintService deviceFingerprintService;

    @Test
    void generateFingerprint_ShouldGenerateValidFingerprint() {
        HttpServletRequest request = SessionTestDataFactory.createMockHttpServletRequest();

        String fingerprint = deviceFingerprintService.generateFingerprint(request);

        assertThat(fingerprint).isNotNull().isNotEmpty();
    }

    @Test
    void extractDeviceInfo_ShouldExtractDesktopInfo() {
        HttpServletRequest request = SessionTestDataFactory.createMockHttpServletRequest();

        DeviceInfo deviceInfo = deviceFingerprintService.extractDeviceInfo(request);

        assertThat(deviceInfo).isNotNull();
        assertThat(deviceInfo.getDeviceType()).isEqualTo("desktop");
        assertThat(deviceInfo.getBrowser()).isEqualTo("Chrome");
        assertThat(deviceInfo.getOs()).isEqualTo("Windows");
    }

    @Test
    void extractDeviceInfo_ShouldExtractMobileInfo() {
        HttpServletRequest request = SessionTestDataFactory.createMockMobileRequest();

        DeviceInfo deviceInfo = deviceFingerprintService.extractDeviceInfo(request);

        assertThat(deviceInfo).isNotNull();
        assertThat(deviceInfo.getDeviceType()).isEqualTo("mobile");
    }

    @Test
    void extractDeviceInfo_ShouldExtractTabletInfo() {
        HttpServletRequest request = SessionTestDataFactory.createMockTabletRequest();

        DeviceInfo deviceInfo = deviceFingerprintService.extractDeviceInfo(request);

        assertThat(deviceInfo).isNotNull();
        assertThat(deviceInfo.getDeviceType()).isIn("tablet", "mobile");
    }

    @Test
    void getClientIpAddress_ShouldExtractFromXForwardedFor() {
        HttpServletRequest request = SessionTestDataFactory.createMockHttpServletRequest();

        String ipAddress = deviceFingerprintService.getClientIpAddress(request);

        assertThat(ipAddress).isEqualTo("192.168.1.100");
    }

    @Test
    void getClientIpAddress_ShouldExtractFromRemoteAddr() {
        HttpServletRequest request = SessionTestDataFactory.createMockMobileRequest();

        String ipAddress = deviceFingerprintService.getClientIpAddress(request);

        assertThat(ipAddress).isEqualTo("192.168.1.101");
    }

    @Test
    void generateFingerprint_ShouldBeDifferentForDifferentRequests() {
        HttpServletRequest request1 = SessionTestDataFactory.createMockHttpServletRequest();
        HttpServletRequest request2 = SessionTestDataFactory.createMockMobileRequest();

        String fingerprint1 = deviceFingerprintService.generateFingerprint(request1);
        String fingerprint2 = deviceFingerprintService.generateFingerprint(request2);

        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }
}


