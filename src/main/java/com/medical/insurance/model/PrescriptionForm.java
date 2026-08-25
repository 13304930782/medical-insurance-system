package com.medical.insurance.model;

import java.math.BigDecimal;

public class PrescriptionForm {
    private String hospitalizationNumber;
    private String chargeableItemsCategory;
    private String projectCoding;
    private String projectName;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String sourceType;
    private String catalogType;

    public String getHospitalizationNumber(){return hospitalizationNumber;} public void setHospitalizationNumber(String v){hospitalizationNumber=v;}
    public String getChargeableItemsCategory(){return chargeableItemsCategory;} public void setChargeableItemsCategory(String v){chargeableItemsCategory=v;}
    public String getProjectCoding(){return projectCoding;} public void setProjectCoding(String v){projectCoding=v;}
    public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
    public String getCatalogType(){return catalogType;} public void setCatalogType(String v){catalogType=v;}
}
