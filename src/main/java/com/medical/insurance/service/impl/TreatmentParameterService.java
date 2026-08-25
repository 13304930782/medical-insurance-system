package com.medical.insurance.service.impl;

import com.medical.insurance.dao.TreatmentParameterMapper;
import com.medical.insurance.exception.TreatmentParameterBusinessException;
import com.medical.insurance.model.CappingLineForm;
import com.medical.insurance.model.MinimumPaymentStandardForm;
import com.medical.insurance.model.SegmentRatioForm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import com.medical.insurance.dao.DictionaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreatmentParameterService {

    private final TreatmentParameterMapper mapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;
    private final DictionaryMapper dictionaryMapper;

    public TreatmentParameterService(TreatmentParameterMapper mapper, AuthService authService, SystemMapper systemMapper, DictionaryMapper dictionaryMapper) {
        this.mapper = mapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
        this.dictionaryMapper = dictionaryMapper;
    }

    public List<Map<String, Object>> cappingLines(String keyword) { return mapper.findCappingLines(normalizeKeyword(keyword)); }
    public List<Map<String, Object>> minimumStandards(String keyword) { return mapper.findMinimumStandards(normalizeKeyword(keyword)); }
    public List<Map<String, Object>> segmentRatios(String keyword) { return mapper.findSegmentRatios(normalizeKeyword(keyword)); }

    @Transactional
    public void createCapping(CappingLineForm form, HttpServletRequest request) {
        validateCapping(form);
        if (mapper.cappingExists(form.getMedicalPersonnelCategory()) > 0) throw new TreatmentParameterBusinessException("该医疗人员类别的封顶线已存在");
        mapper.insertCapping(form);
        record(request, "CREATE", form.getMedicalPersonnelCategory(), "新增基金封顶线：" + form.getCappingLineFee());
    }

    @Transactional
    public void updateCapping(String category, CappingLineForm form, HttpServletRequest request) {
        form.setMedicalPersonnelCategory(trim(category));
        validateCapping(form);
        if (mapper.updateCapping(form.getMedicalPersonnelCategory(), form.getCappingLineFee()) == 0) throw new TreatmentParameterBusinessException("未找到该封顶线参数");
        record(request, "UPDATE", form.getMedicalPersonnelCategory(), "修改基金封顶线：" + form.getCappingLineFee());
    }

    @Transactional
    public void deleteCapping(String category, HttpServletRequest request) {
        String value = required(category, "医疗人员类别不能为空");
        if (mapper.deleteCapping(value) == 0) throw new TreatmentParameterBusinessException("未找到该封顶线参数");
        record(request, "DELETE", value, "删除基金封顶线");
    }

    @Transactional
    public void createMinimum(MinimumPaymentStandardForm form, HttpServletRequest request) {
        validateMinimum(form);
        if (mapper.minimumExists(form) > 0) throw new TreatmentParameterBusinessException("相同医疗类别、人员类别和医院等级的起付标准已存在");
        mapper.insertMinimum(form);
        record(request, "CREATE", minimumKey(form), "新增起付标准：" + form.getMinimumPaymentStandard());
    }

    @Transactional
    public void updateMinimum(MinimumPaymentStandardForm form, HttpServletRequest request) {
        validateMinimum(form);
        if (mapper.updateMinimum(form) == 0) throw new TreatmentParameterBusinessException("未找到该起付标准参数");
        record(request, "UPDATE", minimumKey(form), "修改起付标准：" + form.getMinimumPaymentStandard());
    }

    @Transactional
    public void deleteMinimum(String medicalCategory, String medicalPersonnelCategory, String hospitalLevel, HttpServletRequest request) {
        medicalCategory = required(medicalCategory, "医疗类别不能为空");
        medicalPersonnelCategory = required(medicalPersonnelCategory, "医疗人员类别不能为空");
        hospitalLevel = required(hospitalLevel, "医院等级不能为空");
        if (mapper.deleteMinimum(medicalCategory, medicalPersonnelCategory, hospitalLevel) == 0) throw new TreatmentParameterBusinessException("未找到该起付标准参数");
        record(request, "DELETE", medicalCategory + "/" + medicalPersonnelCategory + "/" + hospitalLevel, "删除起付标准");
    }

    @Transactional
    public void createSegment(SegmentRatioForm form, HttpServletRequest request) {
        validateSegment(form);
        if (mapper.segmentExists(form) > 0) throw new TreatmentParameterBusinessException("相同金额区间的分段比例已存在");
        if (mapper.overlappingSegments(form) > 0) throw new TreatmentParameterBusinessException("该金额区间与已有分段重叠");
        mapper.insertSegment(form);
        record(request, "CREATE", segmentKey(form), "新增分段报销比例：" + form.getReimbursementProportion());
    }

    @Transactional
    public void updateSegment(SegmentRatioForm form, HttpServletRequest request) {
        validateSegment(form);
        if (mapper.updateSegment(form) == 0) throw new TreatmentParameterBusinessException("未找到该分段比例参数");
        record(request, "UPDATE", segmentKey(form), "修改分段报销比例：" + form.getReimbursementProportion());
    }

    @Transactional
    public void deleteSegment(SegmentRatioForm form, HttpServletRequest request) {
        validateSegment(form);
         if (mapper.deleteSegment(form.getMedicalCategory(), form.getMedicalPersonnelCategory(), form.getHospitalLevel(), form.getMinimumAmount(), form.getMaximumAmount()) == 0) {
            throw new TreatmentParameterBusinessException("未找到该分段比例参数");
        }
        record(request, "DELETE", segmentKey(form), "删除分段报销比例");
    }

    private void validateCapping(CappingLineForm form) {
        form.setMedicalPersonnelCategory(required(form.getMedicalPersonnelCategory(), "医疗人员类别不能为空"));
        dictionary("医疗人员类别", form.getMedicalPersonnelCategory());
        nonNegative(form.getCappingLineFee(), "封顶线");
    }

    private void validateMinimum(MinimumPaymentStandardForm form) {
        form.setMedicalCategory(required(form.getMedicalCategory(), "医疗类别不能为空"));
        form.setMedicalPersonnelCategory(required(form.getMedicalPersonnelCategory(), "医疗人员类别不能为空"));
        form.setHospitalLevel(required(form.getHospitalLevel(), "医院等级不能为空"));
        dictionary("医疗类别", form.getMedicalCategory());
        dictionary("医疗人员类别", form.getMedicalPersonnelCategory());
        dictionary("医院等级", form.getHospitalLevel());
        nonNegative(form.getMinimumPaymentStandard(), "起付标准");
    }

    private void validateSegment(SegmentRatioForm form) {
        form.setMedicalCategory(required(form.getMedicalCategory(), "医疗类别不能为空"));
        form.setMedicalPersonnelCategory(required(form.getMedicalPersonnelCategory(), "医疗人员类别不能为空"));
        form.setHospitalLevel(required(form.getHospitalLevel(), "医院等级不能为空"));
        dictionary("医疗类别", form.getMedicalCategory());
        dictionary("医疗人员类别", form.getMedicalPersonnelCategory());
        dictionary("医院等级", form.getHospitalLevel());
        nonNegative(form.getMinimumAmount(), "下限金额");
        nonNegative(form.getMaximumAmount(), "上限金额");
        if (form.getMaximumAmount().compareTo(form.getMinimumAmount()) <= 0) throw new TreatmentParameterBusinessException("上限金额必须大于下限金额");
         if (form.getReimbursementProportion() == null || form.getReimbursementProportion().compareTo(BigDecimal.ZERO) < 0 || form.getReimbursementProportion().compareTo(BigDecimal.ONE) > 0) {
            throw new TreatmentParameterBusinessException("报销比例必须在0到1之间");
        }
    }

    private void nonNegative(BigDecimal value, String label) {
        if (value == null) throw new TreatmentParameterBusinessException(label + "不能为空");
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new TreatmentParameterBusinessException(label + "不能小于0");
    }

    private void dictionary(String category, String value) {
        if (dictionaryMapper.contains(category, value) == 0) throw new TreatmentParameterBusinessException(category + "不在参数字典中：" + value);
    }

    private void record(HttpServletRequest request, String type, String businessNo, String content) {
        systemMapper.recordOperation(authService.currentUserId(request), "医疗待遇计算参数维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String minimumKey(MinimumPaymentStandardForm form) { return form.getMedicalCategory() + "/" + form.getMedicalPersonnelCategory() + "/" + form.getHospitalLevel(); }
    private String segmentKey(SegmentRatioForm form) { return minimumKey(toMinimum(form)) + "/" + form.getMinimumAmount() + "-" + form.getMaximumAmount(); }
    private MinimumPaymentStandardForm toMinimum(SegmentRatioForm form) {
        MinimumPaymentStandardForm value = new MinimumPaymentStandardForm();
        value.setMedicalCategory(form.getMedicalCategory()); value.setMedicalPersonnelCategory(form.getMedicalPersonnelCategory()); value.setHospitalLevel(form.getHospitalLevel());
        return value;
    }
    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private String required(String value, String message) { if (isBlank(value)) throw new TreatmentParameterBusinessException(message); return value.trim(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
