package com.aethelsoft.grooveplayer.domain.repository

interface StartupAdQuotaRepository {
    fun canShow(): Boolean
    fun recordShown()
}
