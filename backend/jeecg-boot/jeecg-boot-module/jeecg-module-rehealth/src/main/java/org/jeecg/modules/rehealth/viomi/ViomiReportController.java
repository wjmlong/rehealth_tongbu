package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jeecg.config.shiro.IgnoreAuth;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Callback endpoint the Viomi platform calls to actively push wearable telemetry.
 *
 * <p>Full path: {@code POST /jeecg-boot/rehealth/viomi/report}. Authentication is bypassed
 * ({@link IgnoreAuth}) because the request is authenticated via the Viomi JWT, not a JeecgBoot
 * user session. The response body is written directly to guarantee the exact
 * {@code {"code":1,"msg":"操作成功"}} / {@code {"code":0,"msg":"操作失败"}} contract expected by
 * the Viomi platform.</p>
 */
@Tag(name = "Viomi Adapter")
@RestController
@RequestMapping("/rehealth/viomi")
public class ViomiReportController {

    private final ViomiReportService service;
    private final ObjectMapper objectMapper;

    public ViomiReportController(ViomiReportService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @IgnoreAuth
    @PostMapping("/report")
    @Operation(summary = "云米平台主动上报回调端点")
    public void report(@RequestBody ViomiReportEnvelope envelope,
                       @RequestHeader(value = "Authorization", required = false) String authorization,
                       HttpServletResponse response) throws IOException {
        ViomiAck ack = service.handle(envelope, authorization);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ack);
    }
}
