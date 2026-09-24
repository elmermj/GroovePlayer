package com.aethelsoft.grooveplayer.data.ads

import android.content.Context
import com.aethelsoft.grooveplayer.domain.repository.StartupAdQuotaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupAdQuotaStore @Inject constructor(
    @ApplicationContext context: Context,
) : StartupAdQuotaRepository {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun canShow(): Boolean = shownToday() < MAX_PER_DAY

    override fun recordShown() {
        val today = todayKey()
        val count = if (prefs.getString(KEY_DATE, null) == today) {
            prefs.getInt(KEY_COUNT, 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putString(KEY_DATE, today)
            .putInt(KEY_COUNT, count)
            .apply()
    }

    private fun shownToday(): Int {
        val today = todayKey()
        return if (prefs.getString(KEY_DATE, null) == today) prefs.getInt(KEY_COUNT, 0) else 0
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        private const val PREFS = "grooveplayer_startup_ads"
        private const val KEY_DATE = "date"
        private const val KEY_COUNT = "count"
        private const val MAX_PER_DAY = 2
    }
}
