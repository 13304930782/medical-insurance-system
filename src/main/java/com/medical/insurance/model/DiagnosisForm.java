package com.medical.insurance.model;

import java.math.BigDecimal;

public class DiagnosisForm {

    private String diaId;
    private String diaName;
    private String diaExpType;
    private String diaExpLevel;
    private BigDecimal diaMaxPrize;
    private String diaStarttime;
    private String diaEndtime;
    private String diaValid;
    private String diaHosLevel;
    private String diaApprovalmark;

    public String getDiaId() { return diaId; }
    public void setDiaId(String diaId) { this.diaId = diaId; }
    public String getDiaName() { return diaName; }
    public void setDiaName(String diaName) { this.diaName = diaName; }
    public String getDiaExpType() { return diaExpType; }
    public void setDiaExpType(String diaExpType) { this.diaExpType = diaExpType; }
    public String getDiaExpLevel() { return diaExpLevel; }
    public void setDiaExpLevel(String diaExpLevel) { this.diaExpLevel = diaExpLevel; }
    public BigDecimal getDiaMaxPrize() { return diaMaxPrize; }
    public void setDiaMaxPrize(BigDecimal diaMaxPrize) { this.diaMaxPrize = diaMaxPrize; }
    public String getDiaStarttime() { return diaStarttime; }
    public void setDiaStarttime(String diaStarttime) { this.diaStarttime = diaStarttime; }
    public String getDiaEndtime() { return diaEndtime; }
    public void setDiaEndtime(String diaEndtime) { this.diaEndtime = diaEndtime; }
    public String getDiaValid() { return diaValid; }
    public void setDiaValid(String diaValid) { this.diaValid = diaValid; }
    public String getDiaHosLevel() { return diaHosLevel; }
    public void setDiaHosLevel(String diaHosLevel) { this.diaHosLevel = diaHosLevel; }
    public String getDiaApprovalmark() { return diaApprovalmark; }
    public void setDiaApprovalmark(String diaApprovalmark) { this.diaApprovalmark = diaApprovalmark; }
}
