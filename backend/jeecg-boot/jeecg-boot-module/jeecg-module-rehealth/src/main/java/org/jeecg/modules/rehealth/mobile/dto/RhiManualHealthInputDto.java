package org.jeecg.modules.rehealth.mobile.dto;

/**
 * User-entered health archive values used by the Android RHI calculation.
 * The authenticated user id is never accepted from the request body.
 */
public class RhiManualHealthInputDto {
    public Double sedentaryHoursPerDay;
    public Double waistCircumferenceCm;
    public Double vo2MaxMlKgMin;
    public Double hba1cPercent;
    public Double egfrMlMin173m2;
    public Double cuffSbp7dMean;
    public Double cuffDbp7dMean;
    public Integer cuffValidDays;
    public Boolean cuffConfirmed;
    public Double fastingGlucoseMmolL;
    public Double totalCholesterolMmolL;
    public Double ldlMmolL;
    public Double hdlMmolL;
    public Double triglyceridesMmolL;
    public Boolean labConfirmed;
    public Long labRecordedAt;
    public Long updatedAt;
}
