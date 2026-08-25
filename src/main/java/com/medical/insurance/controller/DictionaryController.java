package com.medical.insurance.controller;

import com.medical.insurance.dao.DictionaryMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionaries")
public class DictionaryController {

    private final DictionaryMapper dictionaryMapper;

    DictionaryController(DictionaryMapper dictionaryMapper) {
        this.dictionaryMapper = dictionaryMapper;
    }

    @GetMapping
    Map<String, Object> list(@RequestParam String category) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", dictionaryMapper.findByCategory(category.trim()));
        return response;
    }
}
