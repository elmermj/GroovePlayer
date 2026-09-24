package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

/**
 * Leave the process after a committed database swap so Room opens the new file.
 * The live file is not closed in a way that overwrites it; the process exits
 * immediately so the old connection cannot write a WAL onto the new database.
 */
object ProcessRestarter {
    fun restart(context: Context): Nothing {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (launch != null) {
            runCatching { context.startActivity(launch) }
        }
        exitProcess(0)
    }
}
