package org.jeecg.common.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.tea.TeaException;
import org.jeecg.config.AliyunSmsVerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AliyunSmsVerificationServiceTest {

    private Client client;
    private AliyunSmsVerificationService service;

    @BeforeEach
    void setUp() {
        client = mock(Client.class);
        AliyunSmsVerificationProperties properties = new AliyunSmsVerificationProperties();
        properties.setEnabled(true);
        properties.setAccessKeyId("dypns-ak-id");
        properties.setAccessKeySecret("dypns-ak-secret");
        properties.setSignName("系统赠送签名");
        service = new AliyunSmsVerificationService(properties, ignored -> client);
    }

    @Test
    void sendsProviderGeneratedSixDigitCodeWithConfirmedTemplate() throws Exception {
        SendSmsVerifyCodeResponseBody.SendSmsVerifyCodeResponseBodyModel model =
                new SendSmsVerifyCodeResponseBody.SendSmsVerifyCodeResponseBodyModel()
                        .setOutId("provider-out")
                        .setBizId("biz-1");
        SendSmsVerifyCodeResponseBody body = new SendSmsVerifyCodeResponseBody()
                .setSuccess(true)
                .setCode("OK")
                .setRequestId("request-1")
                .setModel(model);
        when(client.sendSmsVerifyCode(any())).thenReturn(new SendSmsVerifyCodeResponse().setBody(body));

        SmsVerificationService.SendReceipt receipt =
                service.sendRegistrationCode("13800138000", "client-out");

        ArgumentCaptor<SendSmsVerifyCodeRequest> captor = ArgumentCaptor.forClass(SendSmsVerifyCodeRequest.class);
        verify(client).sendSmsVerifyCode(captor.capture());
        SendSmsVerifyCodeRequest request = captor.getValue();
        assertEquals("100001", request.getTemplateCode());
        assertEquals("{\"code\":\"##code##\",\"min\":\"5\"}", request.getTemplateParam());
        assertEquals(6L, request.getCodeLength());
        assertEquals(300L, request.getValidTime());
        assertEquals(60L, request.getInterval());
        assertEquals(1L, request.getDuplicatePolicy());
        assertEquals(Boolean.FALSE, request.getReturnVerifyCode());
        assertEquals("provider-out", receipt.outId());
        assertEquals("request-1", receipt.requestId());
        assertEquals("biz-1", receipt.bizId());
        assertEquals(300, receipt.validSeconds());
        assertEquals(60, receipt.intervalSeconds());
    }

    @Test
    void acceptsOnlyPassVerificationResult() throws Exception {
        when(client.checkSmsVerifyCode(any()))
                .thenReturn(checkResponse("PASS"))
                .thenReturn(checkResponse("UNKNOWN"));

        assertTrue(service.checkRegistrationCode("13800138000", "123456", "out-1"));
        assertFalse(service.checkRegistrationCode("13800138000", "654321", "out-1"));

        ArgumentCaptor<CheckSmsVerifyCodeRequest> captor = ArgumentCaptor.forClass(CheckSmsVerifyCodeRequest.class);
        verify(client, org.mockito.Mockito.times(2)).checkSmsVerifyCode(captor.capture());
        CheckSmsVerifyCodeRequest request = captor.getAllValues().get(0);
        assertEquals("rehealth-register", request.getSchemeName());
        assertEquals("86", request.getCountryCode());
        assertEquals("out-1", request.getOutId());
    }

    @Test
    void failsClosedWhenProviderCallIsNotSuccessful() throws Exception {
        CheckSmsVerifyCodeResponseBody body = new CheckSmsVerifyCodeResponseBody()
                .setSuccess(false)
                .setCode("FREQUENCY_FAIL")
                .setMessage("limited");
        when(client.checkSmsVerifyCode(any())).thenReturn(new CheckSmsVerifyCodeResponse().setBody(body));

        SmsVerificationException exception = assertThrows(
                SmsVerificationException.class,
                () -> service.checkRegistrationCode("13800138000", "123456", "out-1")
        );

        assertEquals("FREQUENCY_FAIL", exception.getProviderCode());
    }

    @Test
    void preservesProviderDetailsFromTeaException() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("RequestId", "request-error-1");
        TeaException providerException = new TeaException();
        providerException.setCode("INVALID_PARAMETERS");
        providerException.setStatusCode(400);
        providerException.setData(data);
        when(client.sendSmsVerifyCode(any())).thenThrow(providerException);

        SmsVerificationException exception = assertThrows(
                SmsVerificationException.class,
                () -> service.sendRegistrationCode("13800138000", "out-1")
        );

        assertEquals("INVALID_PARAMETERS", exception.getProviderCode());
        assertEquals("request-error-1", exception.getRequestId());
        assertEquals(providerException, exception.getCause());
    }

    private static CheckSmsVerifyCodeResponse checkResponse(String verifyResult) {
        CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel model =
                new CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel()
                        .setOutId("out-1")
                        .setVerifyResult(verifyResult);
        CheckSmsVerifyCodeResponseBody body = new CheckSmsVerifyCodeResponseBody()
                .setSuccess(true)
                .setCode("OK")
                .setModel(model);
        return new CheckSmsVerifyCodeResponse().setBody(body);
    }
}
