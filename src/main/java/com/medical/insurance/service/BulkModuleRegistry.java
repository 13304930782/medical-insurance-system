package com.medical.insurance.service;

import com.medical.insurance.exception.BulkBusinessException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BulkModuleRegistry {
    public record Module(String code,String label,String table,boolean importable,boolean deletable){}

    private static final Map<String,Module> MODULES=new LinkedHashMap<>();
    static {
        add("medicines","药品目录","t_medicine",true,true);
        add("diagnoses","诊疗项目目录","t_diagnosis_project",true,true);
        add("facilities","服务设施目录","t_service_facilities",true,true);
        add("diseases","病种信息","t_disease_",true,true);
        add("institutions-base","医疗机构原表","t_medical_insititution",true,true);
        add("institution-profiles","医疗机构扩展资料","ext_medical_institution_profile",true,true);
        add("companies","参保单位","company",true,true);
        add("people","参保人员","people",true,true);
        add("capping-lines","封顶线","t_capping_line",true,true);
        add("minimum-payment-standards","起付标准","t_minimum_payment_standard",true,true);
        add("segment-ratios","个人分段自费比例","t_individual_segement_self_funded_ratio",true,true);
        add("institution-approvals","人员就诊机构审批","t_application_info",true,true);
        add("special-approvals","特检特治审批","t_special_approval",true,true);
        add("visits","人员就诊资料","t_personnel_visits_info",true,false);
        add("prescriptions","处方明细","t_prescription_details",true,false);
        add("annual-expenses","个人年度累计","personal_annual_expenses",false,false);
        add("settlements","正式结算记录","ext_reimbursement_settlement",false,false);
        add("settlement-items","结算项目明细","ext_settlement_item_result",false,false);
        add("settlement-breakdowns","结算费用构成","ext_settlement_fee_breakdown",false,false);
        add("dictionaries","数据字典","ext_data_dictionary",true,false);
    }

    private static void add(String code,String label,String table,boolean importable,boolean deletable){MODULES.put(code,new Module(code,label,table,importable,deletable));}
    public static Module required(String code){Module module=MODULES.get(code);if(module==null)throw new BulkBusinessException("不支持的批量模块："+code);return module;}
    public static List<Module> all(){return List.copyOf(MODULES.values());}
    private BulkModuleRegistry(){}
}
