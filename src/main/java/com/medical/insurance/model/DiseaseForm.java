package com.medical.insurance.model;

public class DiseaseForm {

    private String diseaseId;
    private String diseaseName;
    private String diseaseType;
    private String diseaseReimbursementStandards;
    private String notes;

    public String getDiseaseId() { return diseaseId; }
    public void setDiseaseId(String diseaseId) { this.diseaseId = diseaseId; }
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
    public String getDiseaseType() { return diseaseType; }
    public void setDiseaseType(String diseaseType) { this.diseaseType = diseaseType; }
    public String getDiseaseReimbursementStandards() { return diseaseReimbursementStandards; }
    public void setDiseaseReimbursementStandards(String diseaseReimbursementStandards) { this.diseaseReimbursementStandards = diseaseReimbursementStandards; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
