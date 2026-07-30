package org.jeecg.modules.rehealth.viomi;

/**
 * Inbound active-report envelope pushed by the Viomi platform.
 *
 * <p>Field names match the Viomi OpenAPI contract exactly. {@code ResultData} is a JSON string that
 * must be parsed separately per {@code DataType}.</p>
 */
public class ViomiReportEnvelope {

    public String DataType;
    public String ResultData;
    public String AccessToken;
    public String Imei;
    public String ReqId;
    public String Time;
    public String CommandCode;

    // Public fields so Jackson (default PUBLIC_ONLY field visibility) binds the
    // Viomi envelope; mirror the existing rehealth DTO style.
}
