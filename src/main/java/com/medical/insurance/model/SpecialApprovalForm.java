package com.medical.insurance.model;

public class SpecialApprovalForm extends InstitutionApprovalForm {
    private String itemType;
    private String projectCode;
    private String drugCode;

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
}
