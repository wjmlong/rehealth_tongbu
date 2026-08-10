package org.jeecg.common.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.config.AliyunSmsVerificationProperties;
import org.springframework.stereotype.Service;

/** Alibaba Cloud Dypnsapi implementation for registration verification codes. */
@Slf4j
@Service
public class AliyunSmsVerificationService implements SmsVerificationService {

    private static final String PROVIDER_OK = "OK";
    private static final String VERIFY_PASS = "PASS";
    private static final long NUMERIC_CODE_TYPE = 1L;
    private static final long OVERWRITE_OLD_CODE = 1L;
    private static final long AUTO_RETRY_ENABLED = 1L;
    private static final long CASE_SENSITIVE = 2L;

    private final AliyunSmsVerificationProperties properties;
    private final ClientFactory clientFactory;
    private volatile Client client;

    public AliyunSmsVerificationService(AliyunSmsVerificationProperties properties) {
        this(properties, AliyunSmsVerificationService::createClient);
    }

    AliyunSmsVerificationService(
            AliyunSmsVerificationProperties properties,
            ClientFactory clientFactory
    ) {
        this.properties = properties;
        this.clientFactory = clientFactory;
    }

    @Override
    public SendReceipt sendRegistrationCode(String phoneNumber, String outId) {
        AliyunSmsVerificationProperties.Resolved config = resolveConfiguration();
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phoneNumber)
                .setCountryCode(config.countryCode())
                .setSignName(config.signName())
                .setTemplateCode(config.registerTemplateCode())
                .setTemplateParam(templateParam(config.validMinutes()))
                .setSchemeName(config.schemeName())
                .setOutId(outId)
                .setCodeLength((long) config.codeLength())
                .setCodeType(NUMERIC_CODE_TYPE)
                .setValidTime(config.validTimeSeconds())
                .setInterval((long) config.intervalSeconds())
                .setDuplicatePolicy(OVERWRITE_OLD_CODE)
                .setReturnVerifyCode(false)
                .setAutoRetry(AUTO_RETRY_ENABLED);

        try {
            SendSmsVerifyCodeResponse response = client(config).sendSmsVerifyCode(request);
            SendSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (!isProviderSuccess(body)) {
                throw providerFailure(
                        "Aliyun SMS verification send request failed",
                        body == null ? null : body.getCode(),
                        body == null ? null : body.getRequestId()
                );
            }
            SendSmsVerifyCodeResponseBody.SendSmsVerifyCodeResponseBodyModel model = body.getModel();
            String responseOutId = model != null && hasText(model.getOutId()) ? model.getOutId() : outId;
            String bizId = model == null ? null : model.getBizId();
            log.info(
                    "阿里云短信认证发送成功，requestId={}, bizId={}, outId={}",
                    body.getRequestId(),
                    bizId,
                    responseOutId
            );
            return new SendReceipt(
                    responseOutId,
                    body.getRequestId(),
                    bizId,
                    Math.toIntExact(config.validTimeSeconds()),
                    config.intervalSeconds()
            );
        } catch (SmsVerificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SmsVerificationException("Aliyun SMS verification send request failed", exception);
        }
    }

    @Override
    public boolean checkRegistrationCode(String phoneNumber, String verifyCode, String outId) {
        AliyunSmsVerificationProperties.Resolved config = resolveConfiguration();
        CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                .setPhoneNumber(phoneNumber)
                .setCountryCode(config.countryCode())
                .setVerifyCode(verifyCode)
                .setSchemeName(config.schemeName())
                .setOutId(outId)
                .setCaseAuthPolicy(CASE_SENSITIVE);

        try {
            CheckSmsVerifyCodeResponse response = client(config).checkSmsVerifyCode(request);
            CheckSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (!isProviderSuccess(body)) {
                throw providerFailure(
                        "Aliyun SMS verification check request failed",
                        body == null ? null : body.getCode(),
                        null
                );
            }
            CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel model = body.getModel();
            if (model == null || !hasText(model.getVerifyResult())) {
                throw providerFailure("Aliyun SMS verification check response is incomplete", body.getCode(), null);
            }
            boolean passed = VERIFY_PASS.equals(model.getVerifyResult());
            log.info("阿里云短信认证核验完成，result={}, outId={}", passed ? VERIFY_PASS : "UNKNOWN", outId);
            return passed;
        } catch (SmsVerificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SmsVerificationException("Aliyun SMS verification check request failed", exception);
        }
    }

    private AliyunSmsVerificationProperties.Resolved resolveConfiguration() {
        try {
            return properties.resolve();
        } catch (IllegalStateException exception) {
            throw new SmsVerificationException(exception.getMessage(), exception);
        }
    }

    private Client client(AliyunSmsVerificationProperties.Resolved config) throws Exception {
        Client current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    current = clientFactory.create(config);
                    client = current;
                }
            }
        }
        return current;
    }

    private static Client createClient(AliyunSmsVerificationProperties.Resolved config) throws Exception {
        Config clientConfig = new Config()
                .setAccessKeyId(config.accessKeyId())
                .setAccessKeySecret(config.accessKeySecret())
                .setEndpoint(config.endpoint());
        return new Client(clientConfig);
    }

    private static boolean isProviderSuccess(SendSmsVerifyCodeResponseBody body) {
        return body != null && Boolean.TRUE.equals(body.getSuccess()) && PROVIDER_OK.equals(body.getCode());
    }

    private static boolean isProviderSuccess(CheckSmsVerifyCodeResponseBody body) {
        return body != null && Boolean.TRUE.equals(body.getSuccess()) && PROVIDER_OK.equals(body.getCode());
    }

    private static SmsVerificationException providerFailure(
            String message,
            String providerCode,
            String requestId
    ) {
        log.warn("{}，providerCode={}, requestId={}", message, providerCode, requestId);
        return new SmsVerificationException(message, providerCode, requestId);
    }

    private static String templateParam(int validMinutes) {
        return "{\"code\":\"##code##\",\"min\":\"" + validMinutes + "\"}";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @FunctionalInterface
    interface ClientFactory {
        Client create(AliyunSmsVerificationProperties.Resolved config) throws Exception;
    }
}
