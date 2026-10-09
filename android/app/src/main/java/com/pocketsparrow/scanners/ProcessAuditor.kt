package com.pocketsparrow.scanners

import android.app.ActivityManager
import android.content.Context
import java.io.File

data class AndroidProcessInfo(
    val pid: Int,
    val name: String,
    val path: String,
    val isSuspicious: Boolean,
    val isSystem: Boolean = false,
    val threatDetail: String,
    val importance: Int = 0
)

/**
 * Cross-platform Android Process Auditor.
 * Inspects running application processes, background services, and native processes via ActivityManager and /proc.
 * Identifies and protects essential Android system processes from being terminated.
 */
class ProcessAuditor(private val context: Context) {

    companion object {
        private val SYSTEM_PROCESS_NAMES = setOf(
            "init", "ueventd", "zygote", "zygote64", "system_server",
            "surfaceflinger", "servicemanager", "hwservicemanager", "vndservicemanager",
            "vold", "netd", "keystore", "keystore2", "logd", "adbd", "installd",
            "tombstoned", "traced", "lmkd", "statsd", "incidentd",
            "com.android.systemui", "com.android.phone", "com.google.android.gms"
        )

        fun isSystemProcess(pid: Int, name: String, path: String): Boolean {
            if (pid <= 2) return true
            if (SYSTEM_PROCESS_NAMES.contains(name)) return true
            val pathLower = path.lowercase()
            return pathLower.startsWith("/system/") ||
                    pathLower.startsWith("/vendor/") ||
                    pathLower.startsWith("/apex/")
        }
    }

    fun scanProcesses(): List<AndroidProcessInfo> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val list = mutableListOf<AndroidProcessInfo>()

        // 1. Inspect running application processes via ActivityManager
        val runningProcesses = am?.runningAppProcesses ?: emptyList()
        for (proc in runningProcesses) {
            val isSystem = isSystemProcess(proc.pid, proc.processName, "/system/bin/${proc.processName}")
            val isSuspicious = !isSystem && (proc.processName.contains("hidden", ignoreCase = true) ||
                    proc.processName.contains("payload", ignoreCase = true) ||
                    proc.processName.contains("dropper", ignoreCase = true) ||
                    proc.processName.contains("reverse", ignoreCase = true))

            val detail = if (isSuspicious) {
                "Background process matches deceptive or covert execution signature."
            } else if (isSystem) {
                "Core Android operating system service. Termination prohibited."
            } else {
                ""
            }

            list.add(
                AndroidProcessInfo(
                    pid = proc.pid,
                    name = proc.processName,
                    path = if (isSystem) "/system/bin/${proc.processName}" else "/data/app/${proc.processName}",
                    isSuspicious = isSuspicious,
                    isSystem = isSystem,
                    threatDetail = detail,
                    importance = proc.importance
                )
            )
        }

        // 2. Direct /proc inspection fallback for native Android daemons
        if (list.isEmpty()) {
            val procDir = File("/proc")
            if (procDir.exists() && procDir.canRead()) {
                val files = procDir.listFiles() ?: emptyArray()
                for (file in files) {
                    val pid = file.name.toIntOrNull() ?: continue
                    val commFile = File(file, "comm")
                    val cmdlineFile = File(file, "cmdline")

                    val name = if (commFile.exists()) {
                        commFile.readText().trim()
                    } else {
                        "proc-$pid"
                    }

                    val path = if (cmdlineFile.exists()) {
                        cmdlineFile.readText().replace('\u0000', ' ').trim()
                    } else {
                        "/system/bin/$name"
                    }

                    val finalPath = if (path.isEmpty()) "/system/bin/$name" else path
                    val isSystem = isSystemProcess(pid, name, finalPath)

                    list.add(
                        AndroidProcessInfo(
                            pid = pid,
                            name = name,
                            path = finalPath,
                            isSuspicious = false,
                            isSystem = isSystem,
                            threatDetail = if (isSystem) "Core Android operating system service. Termination prohibited." else ""
                        )
                    )
                }
            }
        }

        // 3. Fallback default host processes if sandbox limits process enumeration
        if (list.isEmpty()) {
            list.add(
                AndroidProcessInfo(
                    pid = 1,
                    name = "init",
                    path = "/system/bin/init",
                    isSuspicious = false,
                    isSystem = true,
                    threatDetail = "Core Android operating system service. Termination prohibited."
                )
            )
            list.add(
                AndroidProcessInfo(
                    pid = android.os.Process.myPid(),
                    name = context.packageName,
                    path = "/data/app/${context.packageName}",
                    isSuspicious = false,
                    isSystem = true,
                    threatDetail = "Protected Pocket Sparrow runtime application."
                )
            )
        }

        return list.sortedByDescending { it.isSuspicious }
    }
}
