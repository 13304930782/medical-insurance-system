package com.medical.insurance.model;

import java.util.List;
import java.util.Map;

public class BulkDeleteRequest {
    private List<Map<String,Object>> keys;
    private String confirmation;
    public List<Map<String,Object>> getKeys(){return keys;}
    public void setKeys(List<Map<String,Object>> keys){this.keys=keys;}
    public String getConfirmation(){return confirmation;}
    public void setConfirmation(String confirmation){this.confirmation=confirmation;}
}
