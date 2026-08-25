package com.medical.insurance.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReimbursementDashboardMapper {

    @Select("SELECT COUNT(*) FROM people")
    long peopleCount();

    @Select("SELECT COUNT(*) FROM company")
    long companyCount();

    @Select("SELECT COUNT(*) FROM t_personnel_visits_info WHERE settlement_flag IS NULL OR settlement_flag NOT IN ('已结算','SETTLED')")
    long pendingVisitCount();

    @Select("SELECT COUNT(*) FROM ext_reimbursement_settlement "
        + "WHERE transaction_type=1 AND settlement_status='SETTLED' AND YEAR(settled_at)=#{year}")
    long settledVisitCount(@Param("year") int year);

    @Select("SELECT COALESCE(SUM(fund_fee),0) FROM ext_reimbursement_settlement "
        + "WHERE YEAR(settled_at)=#{year}")
    BigDecimal fundPaid(@Param("year") int year);

    @Select("SELECT COALESCE(NULLIF(TRIM(company_type),''),'未分类') AS companyType,COUNT(*) AS companyCount FROM company GROUP BY COALESCE(NULLIF(TRIM(company_type),''),'未分类') ORDER BY companyCount DESC,companyType")
    List<Map<String,Object>> companyTypeDistribution();
}
