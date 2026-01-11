package com.aethelsoft.grooveplayer.data.local.db

import androidx.room.TypeConverter
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier

/**
 * Type converters for Room database.
 * Converts custom types to/from database-supported types.
 */
class Converters {
    
    @TypeConverter
    fun fromPrivilegeTier(tier: PrivilegeTier): String {
        return tier.name
    }
    
    @TypeConverter
    fun toPrivilegeTier(value: String): PrivilegeTier {
        return try {
            PrivilegeTier.valueOf(value)
        } catch (e: IllegalArgumentException) {
            PrivilegeTier.FREE  // Default to FREE if invalid
        }
    }
}
