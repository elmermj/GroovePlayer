package com.aethelsoft.grooveplayer.utils

object TimeframeUtils {
    fun getAllTimeTimestamp(): Long = 0L
    
    fun getLast30DaysTimestamp(): Long {
        val now = System.currentTimeMillis()
        val thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000
        return now - thirtyDaysInMs
    }
    
    fun getThisMonthTimestamp(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getThisWeekTimestamp(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getLast7DaysTimestamp(): Long {
        val now = System.currentTimeMillis()
        val sevenDaysInMs = 7L * 24 * 60 * 60 * 1000
        return now - sevenDaysInMs
    }
    
    fun getLast3MonthsTimestamp(): Long {
        val now = System.currentTimeMillis()
        val threeMonthsInMs = 90L * 24 * 60 * 60 * 1000
        return now - threeMonthsInMs
    }
}

