package org.jeecg.modules.rehealth.mobile.dto;

import java.util.List;

public class PatientProfileDto {
    public String patientId;
    public String name;
    public String gender;
    public Integer age;
    public Double heightCm;
    public Double weightKg;
    public Double bmi;
    public List<String> diagnoses;
    public List<String> medications;
    public List<String> allergies;
    public Boolean familyHistory;
    public Boolean smoking;
    public Boolean drinking;
    public Boolean diabetesHistory;
    public Boolean hypertensionHistory;
    public Long updatedAt;
}
