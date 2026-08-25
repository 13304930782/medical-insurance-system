package com.medical.insurance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InstitutionApprovalForm {
    private String approvalNumber;
    private String personId;
    private String approvalCategory;
    private LocalDate startDate;
    private LocalDate terminationDate;
    private String medicalInstitutionCode;
    private String approvalOpinions;
    private String approver;
    private LocalDateTime approvalDate;
    private String approvalFlag;

    public String getApprovalNumber() { return approvalNumber; }
    public void setApprovalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; }
    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    public String getApprovalCategory() { return approvalCategory; }
    public void setApprovalCategory(String approvalCategory) { this.approvalCategory = approvalCategory; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
    public String getMedicalInstitutionCode() { return medicalInstitutionCode; }
    public void setMedicalInstitutionCode(String medicalInstitutionCode) { this.medicalInstitutionCode = medicalInstitutionCode; }
    public String getApprovalOpinions() { return approvalOpinions; }
    public void setApprovalOpinions(String approvalOpinions) { this.approvalOpinions = approvalOpinions; }
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }
    public String getApprovalFlag() { return approvalFlag; }
    public void setApprovalFlag(String approvalFlag) { this.approvalFlag = approvalFlag; }
}
