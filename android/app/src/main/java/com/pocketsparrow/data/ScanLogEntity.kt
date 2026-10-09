package com.pocketsparrow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_logs")
data class ScanLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val contentType: String,
    val payloadSnippet: String,
    val threatLevel: Int,
    val category: String,
    val latencyMicros: Long,
    val xaiReason: String,
    val shouldBlock: Boolean
)
