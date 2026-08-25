package com.medical.insurance.model;

import java.time.LocalDateTime;

public class VisitForm {
    private String personId;
    private String hospitalizationNumber;
    private String designatedNumber;
    private String medicalCategory;
    private LocalDateTime admissionDate;
    private LocalDateTime dischargeDate;
    private String diseaseCode;
    private String hospitalGrade;
    private String admissionCode;
    private String diagnosedName;
    private String dischargeReason;
    private String settlementFlag;

    public String getPersonId(){return personId;} public void setPersonId(String v){personId=v;}
    public String getHospitalizationNumber(){return hospitalizationNumber;} public void setHospitalizationNumber(String v){hospitalizationNumber=v;}
    public String getDesignatedNumber(){return designatedNumber;} public void setDesignatedNumber(String v){designatedNumber=v;}
    public String getMedicalCategory(){return medicalCategory;} public void setMedicalCategory(String v){medicalCategory=v;}
    public LocalDateTime getAdmissionDate(){return admissionDate;} public void setAdmissionDate(LocalDateTime v){admissionDate=v;}
    public LocalDateTime getDischargeDate(){return dischargeDate;} public void setDischargeDate(LocalDateTime v){dischargeDate=v;}
    public String getDiseaseCode(){return diseaseCode;} public void setDiseaseCode(String v){diseaseCode=v;}
    public String getHospitalGrade(){return hospitalGrade;} public void setHospitalGrade(String v){hospitalGrade=v;}
    public String getAdmissionCode(){return admissionCode;} public void setAdmissionCode(String v){admissionCode=v;}
    public String getDiagnosedName(){return diagnosedName;} public void setDiagnosedName(String v){diagnosedName=v;}
    public String getDischargeReason(){return dischargeReason;} public void setDischargeReason(String v){dischargeReason=v;}
    public String getSettlementFlag(){return settlementFlag;} public void setSettlementFlag(String v){settlementFlag=v;}
}
