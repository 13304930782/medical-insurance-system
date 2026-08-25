package com.medical.insurance.service.impl;

import com.medical.insurance.dao.ApprovalMapper;
import com.medical.insurance.exception.ApprovalBusinessException;
import com.medical.insurance.model.InstitutionApprovalForm;
import com.medical.insurance.model.SpecialApprovalForm;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private final ApprovalMapper mapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public ApprovalService(ApprovalMapper mapper, AuthService authService, SystemMapper systemMapper) {
        this.mapper=mapper; this.authService=authService; this.systemMapper=systemMapper;
    }

    public List<Map<String,Object>> institutionApprovals(String keyword) { return mapper.findInstitutionApprovals(normalize(keyword)); }
    public List<Map<String,Object>> specialApprovals(String keyword) { return mapper.findSpecialApprovals(normalize(keyword)); }
    public List<Map<String,Object>> specialProjectOptions(String itemType,String keyword) {
        String normalizedType=required(itemType,"审批项目类别不能为空").toUpperCase();
        if("MEDICINE".equals(normalizedType)) return mapper.findSpecialMedicineProjects(normalize(keyword));
        if("DIAGNOSIS".equals(normalizedType)) return mapper.findSpecialDiagnosisProjects(normalize(keyword));
        throw new ApprovalBusinessException("审批项目类别只能是药品或诊疗项目");
    }

    @Transactional
    public void createInstitution(InstitutionApprovalForm form,HttpServletRequest request) {
        validateInstitution(form,request);
        if(mapper.institutionApprovalExists(form.getApprovalNumber())>0) throw new ApprovalBusinessException("审批编号已存在");
        mapper.insertInstitutionApproval(form); record(request,"人员就诊机构审批","CREATE",form.getApprovalNumber(),"新增人员就诊机构审批");
    }
    @Transactional
    public void updateInstitution(String number,InstitutionApprovalForm form,HttpServletRequest request) {
        form.setApprovalNumber(number); validateInstitution(form,request);
        if(mapper.updateInstitutionApproval(form)==0) throw new ApprovalBusinessException("未找到该审批记录");
        record(request,"人员就诊机构审批","UPDATE",number,"修改人员就诊机构审批");
    }
    @Transactional
    public void deleteInstitution(String number,HttpServletRequest request) {
        if(mapper.deleteInstitutionApproval(required(number,"审批编号不能为空"))==0) throw new ApprovalBusinessException("未找到该审批记录");
        record(request,"人员就诊机构审批","DELETE",number,"删除人员就诊机构审批");
    }

    @Transactional
    public void createSpecial(SpecialApprovalForm form,HttpServletRequest request) {
        validateSpecial(form,request);
        if(mapper.specialApprovalExists(form.getApprovalNumber())>0) throw new ApprovalBusinessException("审批编号已存在");
        mapper.insertSpecialApproval(form); mapper.upsertSpecialItem(form); record(request,"特检特治审批","CREATE",form.getApprovalNumber(),"新增特检特治审批");
    }
    @Transactional
    public void updateSpecial(String number,SpecialApprovalForm form,HttpServletRequest request) {
        form.setApprovalNumber(number); validateSpecial(form,request);
        if(mapper.updateSpecialApproval(form)==0) throw new ApprovalBusinessException("未找到该审批记录");
        mapper.upsertSpecialItem(form); record(request,"特检特治审批","UPDATE",number,"修改特检特治审批");
    }
    @Transactional
    public void deleteSpecial(String number,HttpServletRequest request) {
        if(mapper.deleteSpecialApproval(required(number,"审批编号不能为空"))==0) throw new ApprovalBusinessException("未找到该审批记录");
        record(request,"特检特治审批","DELETE",number,"删除特检特治审批");
    }

    private void validateInstitution(InstitutionApprovalForm form,HttpServletRequest request) {
        normalizeBase(form,request);
        form.setMedicalInstitutionCode(required(form.getMedicalInstitutionCode(),"定点医疗机构编码不能为空"));
        if(mapper.personExists(form.getPersonId())==0) throw new ApprovalBusinessException("个人编号不存在");
        if(mapper.institutionExists(form.getMedicalInstitutionCode())==0) throw new ApprovalBusinessException("定点医疗机构不存在");
    }
    private void validateSpecial(SpecialApprovalForm form,HttpServletRequest request) {
        normalizeBase(form,request);
        form.setItemType(required(form.getItemType(),"审批项目类别不能为空").toUpperCase());
        form.setProjectCode(required(form.getProjectCode(),"项目编码不能为空"));
        if(!"MEDICINE".equals(form.getItemType()) && !"DIAGNOSIS".equals(form.getItemType())) throw new ApprovalBusinessException("审批项目类别只能是药品或诊疗项目");
        if(mapper.personExists(form.getPersonId())==0) throw new ApprovalBusinessException("个人编号不存在");
         if("MEDICINE".equals(form.getItemType())) {
            if(mapper.medicineExists(form.getProjectCode())==0) throw new ApprovalBusinessException("药品编码不存在");
            if(mapper.medicineRequiresSpecialApproval(form.getProjectCode())==0) throw new ApprovalBusinessException("所选药品不需要特检特治审批");
            form.setDrugCode(form.getProjectCode());
        } else {
            if(mapper.diagnosisExists(form.getProjectCode())==0) throw new ApprovalBusinessException("诊疗项目编码不存在");
            if(mapper.diagnosisRequiresSpecialApproval(form.getProjectCode())==0) throw new ApprovalBusinessException("所选诊疗项目不需要特检特治审批");
            form.setDrugCode(null);
        }
    }
    private void normalizeBase(InstitutionApprovalForm form,HttpServletRequest request) {
        form.setApprovalNumber(required(form.getApprovalNumber(),"审批编号不能为空"));
        form.setPersonId(required(form.getPersonId(),"个人编号不能为空"));
        form.setApprovalCategory(trim(form.getApprovalCategory())); form.setApprovalOpinions(trim(form.getApprovalOpinions()));
        form.setApprovalFlag(required(form.getApprovalFlag(),"审批标志不能为空"));
        if(form.getStartDate()!=null && form.getTerminationDate()!=null && form.getTerminationDate().isBefore(form.getStartDate())) throw new ApprovalBusinessException("终止日期不能早于开始日期");
        if(blank(form.getApprover())) form.setApprover(authService.currentRealName(request)); else form.setApprover(form.getApprover().trim());
        if(form.getApprovalDate()==null) form.setApprovalDate(LocalDateTime.now());
    }
    private void record(HttpServletRequest request,String module,String type,String no,String content){systemMapper.recordOperation(authService.currentUserId(request),module,type,no,content,"SUCCESS",request.getRemoteAddr());}
    private String normalize(String value){return blank(value)?null:value.trim();}
    private String required(String value,String message){if(blank(value))throw new ApprovalBusinessException(message);return value.trim();}
    private String trim(String value){return value==null?null:value.trim();}
    private boolean blank(String value){return value==null||value.trim().isEmpty();}
}
