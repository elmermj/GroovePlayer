package com.aethelworks.grooveplayer.domain.repository

interface StartupAdQuotaRepository {
    fun canShow(): Boolean
    fun recordShown()
}
