package com.pocketsparrow.scanners

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult

data class ApkAuditReport(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val requestedPermissions: List<String>,
    val scanResult: ScanResult,
    val riskScorePercent: Int
)

class ApkAuditor(private val context: Context) {

    fun auditInstalledPackages(): List<ApkAuditReport> {
        val pm = context.packageManager
        val packages: List<PackageInfo> = try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            emptyList()
        }

        val reports = mutableListOf<ApkAuditReport>()

        for (pkg in packages) {
            val perms = pkg.requestedPermissions ?: emptyArray()
            if (perms.isEmpty()) continue

            val appName = try {
                pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
            } catch (_: Exception) {
                pkg.packageName
            }

            val isSystem = (pkg.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0

            // Run static risk scoring
            val scanResult = NativeBridge.audit(perms)
            val scorePct = (scanResult.confidence * 100).toInt()

            reports.add(
                ApkAuditReport(
                    packageName = pkg.packageName,
                    appName = appName,
                    isSystemApp = isSystem,
                    requestedPermissions = perms.toList(),
                    scanResult = scanResult,
                    riskScorePercent = scorePct
                )
            )
        }

        // Sort descending by risk score
        return reports.sortedByDescending { it.riskScorePercent }
    }

    fun auditSideloadedApkFile(apkPath: String): ApkAuditReport? {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_PERMISSIONS) ?: return null
        val perms = pkgInfo.requestedPermissions ?: emptyArray()

        val scanResult = NativeBridge.audit(perms)
        val scorePct = (scanResult.confidence * 100).toInt()

        return ApkAuditReport(
            packageName = pkgInfo.packageName,
            appName = pkgInfo.packageName,
            isSystemApp = false,
            requestedPermissions = perms.toList(),
            scanResult = scanResult,
            riskScorePercent = scorePct
        )
    }
}
