package com.medical.insurance.model;

import java.math.BigDecimal;

public class SegmentRatioForm {
    private String medicalCategory;
    private String medicalPersonnelCategory;
    private String hospitalLevel;
    private BigDecimal maximumAmount;
    private BigDecimal minimumAmount;
    private BigDecimal reimbursementProportion;

    public String getMedicalCategory() { return medicalCategory; }
    public void setMedicalCategory(String medicalCategory) { this.medicalCategory = medicalCategory; }
    public String getMedicalPersonnelCategory() { return medicalPersonnelCategory; }
    public void setMedicalPersonnelCategory(String medicalPersonnelCategory) { this.medicalPersonnelCategory = medicalPersonnelCategory; }
    public String getHospitalLevel() { return hospitalLevel; }
    public void setHospitalLevel(String hospitalLevel) { this.hospitalLevel = hospitalLevel; }
    public BigDecimal getMaximumAmount() { return maximumAmount; }
    public void setMaximumAmount(BigDecimal maximumAmount) { this.maximumAmount = maximumAmount; }
    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public void setMinimumAmount(BigDecimal minimumAmount) { this.minimumAmount = minimumAmount; }
    public BigDecimal getReimbursementProportion() { return reimbursementProportion; }
    public void setReimbursementProportion(BigDecimal reimbursementProportion) { this.reimbursementProportion = reimbursementProportion; }
}
