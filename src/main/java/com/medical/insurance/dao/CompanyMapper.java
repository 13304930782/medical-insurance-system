package com.medical.insurance.dao;

import com.medical.insurance.model.CompanyForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CompanyMapper {

    long count(@Param("keyword") String keyword);

    List<Map<String, Object>> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    Map<String, Object> findById(@Param("companyId") String companyId);

    int exists(@Param("companyId") String companyId);

    int insert(CompanyForm form);

    int update(@Param("companyId") String companyId, @Param("form") CompanyForm form);

    int delete(@Param("companyId") String companyId);
}
