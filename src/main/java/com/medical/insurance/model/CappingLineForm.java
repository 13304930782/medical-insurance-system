package com.medical.insurance.model;

import java.math.BigDecimal;

public class CappingLineForm {
    private String medicalPersonnelCategory;
    private BigDecimal cappingLineFee;

    public String getMedicalPersonnelCategory() { return medicalPersonnelCategory; }
    public void setMedicalPersonnelCategory(String medicalPersonnelCategory) { this.medicalPersonnelCategory = medicalPersonnelCategory; }
    public BigDecimal getCappingLineFee() { return cappingLineFee; }
    public void setCappingLineFee(BigDecimal cappingLineFee) { this.cappingLineFee = cappingLineFee; }
}
