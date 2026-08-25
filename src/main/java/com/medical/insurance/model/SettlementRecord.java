package com.medical.insurance.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SettlementRecord {
    private Long settlementId;
    private String settlementNo;
    private String hospitalizationNumber;
    private int transactionType;
    private Long originalSettlementId;
    private BigDecimal totalFee;
    private BigDecimal eligibleFee;
    private BigDecimal overLimitSelfFee;
    private BigDecimal deductibleSelfFee;
    private BigDecimal segmentSelfFee;
    private BigDecimal personalFee;
    private BigDecimal fundFee;
    private String settlementStatus;
    private String cancelReason;
    private Long operatorId;
    private LocalDateTime settledAt;

    public Long getSettlementId(){return settlementId;} public void setSettlementId(Long value){settlementId=value;}
    public String getSettlementNo(){return settlementNo;} public void setSettlementNo(String value){settlementNo=value;}
    public String getHospitalizationNumber(){return hospitalizationNumber;} public void setHospitalizationNumber(String value){hospitalizationNumber=value;}
    public int getTransactionType(){return transactionType;} public void setTransactionType(int value){transactionType=value;}
    public Long getOriginalSettlementId(){return originalSettlementId;} public void setOriginalSettlementId(Long value){originalSettlementId=value;}
    public BigDecimal getTotalFee(){return totalFee;} public void setTotalFee(BigDecimal value){totalFee=value;}
    public BigDecimal getEligibleFee(){return eligibleFee;} public void setEligibleFee(BigDecimal value){eligibleFee=value;}
    public BigDecimal getOverLimitSelfFee(){return overLimitSelfFee;} public void setOverLimitSelfFee(BigDecimal value){overLimitSelfFee=value;}
    public BigDecimal getDeductibleSelfFee(){return deductibleSelfFee;} public void setDeductibleSelfFee(BigDecimal value){deductibleSelfFee=value;}
    public BigDecimal getSegmentSelfFee(){return segmentSelfFee;} public void setSegmentSelfFee(BigDecimal value){segmentSelfFee=value;}
    public BigDecimal getPersonalFee(){return personalFee;} public void setPersonalFee(BigDecimal value){personalFee=value;}
    public BigDecimal getFundFee(){return fundFee;} public void setFundFee(BigDecimal value){fundFee=value;}
    public String getSettlementStatus(){return settlementStatus;} public void setSettlementStatus(String value){settlementStatus=value;}
    public String getCancelReason(){return cancelReason;} public void setCancelReason(String value){cancelReason=value;}
    public Long getOperatorId(){return operatorId;} public void setOperatorId(Long value){operatorId=value;}
    public LocalDateTime getSettledAt(){return settledAt;} public void setSettledAt(LocalDateTime value){settledAt=value;}
}
