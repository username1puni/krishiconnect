package com.krishiconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.krishiconnect.entity.CropRule;

public interface CropRuleRepository extends JpaRepository<CropRule, Long> {

    @Query("select distinct r.crop from CropRule r where r.crop <> 'ALL' order by r.crop")
    List<String> findCrops();
}
