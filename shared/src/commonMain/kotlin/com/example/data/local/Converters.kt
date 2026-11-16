package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CareActionType
import com.example.data.model.GrowthStage

class Converters {
    @TypeConverter
    fun fromGrowthStage(stage: GrowthStage?): String? = stage?.name

    @TypeConverter
    fun toGrowthStage(value: String?): GrowthStage? = value?.let {
        try {
            GrowthStage.valueOf(it)
        } catch (_: Exception) {
            GrowthStage.VEGETATIVE
        }
    }

    @TypeConverter
    fun fromCareActionType(action: CareActionType?): String? = action?.name

    @TypeConverter
    fun toCareActionType(value: String?): CareActionType? = value?.let {
        try {
            CareActionType.valueOf(it)
        } catch (_: Exception) {
            CareActionType.NOTE
        }
    }
}
