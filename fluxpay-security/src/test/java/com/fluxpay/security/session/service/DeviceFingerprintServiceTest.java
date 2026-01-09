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
import static org.mockito.Mockito.when;

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

    @Test
    void extractDeviceInfo_WithNullUserAgent_ShouldReturnEmptyDeviceInfo() {
        HttpServletRequest request = SessionTestDataFactory.createMockHttpServletRequest();
        when(request.getHeader("User-Agent")).thenReturn(null);

        DeviceInfo deviceInfo = deviceFingerprintService.extractDeviceInfo(request);

        assertThat(deviceInfo).isNotNull();
        assertThat(deviceInfo.getDeviceId()).isNotNull();
    }

    @Test
    void getClientIpAddress_WithMultipleIps_ShouldReturnFirstIp() {
        HttpServletRequest request = SessionTestDataFactory.createMockHttpServletRequest();
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");

        String ipAddress = deviceFingerprintService.getClientIpAddress(request);

        assertThat(ipAddress).isEqualTo("10.0.0.1");
    }

    @Test
    void getClientIpAddress_WithUnknownIp_ShouldFallbackToNextHeader() {
        HttpServletRequest request = SessionTestDataFactory.createMockHttpServletRequest();
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("172.16.0.1");

        String ipAddress = deviceFingerprintService.getClientIpAddress(request);

        assertThat(ipAddress).isEqualTo("172.16.0.1");
    }

    @Test
    void generateFingerprint_WithNullHeaders_ShouldGenerateValidFingerprint() {
        HttpServletRequest request = SessionTestDataFactory.createMockHttpServletRequest();
        when(request.getHeader("User-Agent")).thenReturn(null);
        when(request.getHeader("Accept-Language")).thenReturn(null);
        when(request.getHeader("Accept-Encoding")).thenReturn(null);

        String fingerprint = deviceFingerprintService.generateFingerprint(request);

        assertThat(fingerprint).isNotNull().isNotEmpty();
    }
}


