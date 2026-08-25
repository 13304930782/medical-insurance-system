package com.medical.insurance.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DictionaryMapper {

    List<Map<String, Object>> findByCategory(@Param("category") String category);

    int contains(@Param("category") String category, @Param("value") String value);
}
