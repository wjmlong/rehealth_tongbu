package org.jeecg.modules.rehealth.mobile.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PatientProfileDto {
    public String patientId;
    @Size(max = 128)
    public String name;
    @Pattern(regexp = "male|female")
    public String gender;
    @Min(1)
    @Max(120)
    public Integer age;
    @DecimalMin("50.0")
    @DecimalMax("250.0")
    public Double heightCm;
    @DecimalMin("2.0")
    @DecimalMax("500.0")
    public Double weightKg;
    public Double bmi;
    @Size(max = 100)
    public List<String> diagnoses;
    @Size(max = 100)
    public List<String> medications;
    @Size(max = 100)
    public List<String> allergies;
    public Boolean familyHistory;
    public Boolean smoking;
    public Boolean drinking;
    public Boolean diabetesHistory;
    public Boolean hypertensionHistory;
    public Long updatedAt;
    public Long version;
}
