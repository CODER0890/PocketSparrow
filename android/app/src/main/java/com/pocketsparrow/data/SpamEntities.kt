package com.pocketsparrow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spam_numbers")
data class SpamNumberEntity(
    @PrimaryKey val hash: String,
    val originalMasked: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val reportCount: Int,
    val category: String
)

@Entity(tableName = "spam_senders")
data class SpamSenderEntity(
    @PrimaryKey val hash: String,
    val originalMasked: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val reportCount: Int,
    val category: String
)

@Entity(tableName = "spam_patterns")
data class SpamPatternEntity(
    @PrimaryKey val patternHash: String,
    val patternSnippet: String,
    val category: String,
    val hits: Int
)

@Entity(tableName = "user_reports")
data class UserReportEntity(
    @PrimaryKey val id: String,
    val targetHash: String,
    val targetMasked: String,
    val category: String,
    val reason: String,
    val timestamp: Long
)

@Entity(tableName = "quarantined_messages")
data class QuarantinedMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val category: String,
    val isRead: Boolean
)
