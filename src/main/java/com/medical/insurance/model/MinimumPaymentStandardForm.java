package com.medical.insurance.model;

import java.math.BigDecimal;

public class MinimumPaymentStandardForm {
    private String medicalCategory;
    private String medicalPersonnelCategory;
    private String hospitalLevel;
    private BigDecimal minimumPaymentStandard;

    public String getMedicalCategory() { return medicalCategory; }
    public void setMedicalCategory(String medicalCategory) { this.medicalCategory = medicalCategory; }
    public String getMedicalPersonnelCategory() { return medicalPersonnelCategory; }
    public void setMedicalPersonnelCategory(String medicalPersonnelCategory) { this.medicalPersonnelCategory = medicalPersonnelCategory; }
    public String getHospitalLevel() { return hospitalLevel; }
    public void setHospitalLevel(String hospitalLevel) { this.hospitalLevel = hospitalLevel; }
    public BigDecimal getMinimumPaymentStandard() { return minimumPaymentStandard; }
    public void setMinimumPaymentStandard(BigDecimal minimumPaymentStandard) { this.minimumPaymentStandard = minimumPaymentStandard; }
}
