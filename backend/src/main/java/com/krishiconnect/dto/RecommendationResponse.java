package com.krishiconnect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.CropRule;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.Skill;

/** "Paddy harvest is on: you'll need a combine harvester. 4 available within 30 km." */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationResponse(Long ruleId, String crop, String season, String activity, int monthFrom,
                                     int monthTo, EquipmentCategory equipmentCategory, Skill skill, String tip,
                                     boolean yourCrop, Long availableNearby) {

    public static RecommendationResponse from(CropRule r, boolean yourCrop, Long availableNearby) {
        return new RecommendationResponse(r.getId(), r.getCrop(), r.getSeason(), r.getActivity(), r.getMonthFrom(),
                r.getMonthTo(), r.getEquipmentCategory(), r.getSkill(), r.getTip(), yourCrop, availableNearby);
    }
}
