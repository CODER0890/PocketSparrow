package com.pocketsparrow.services

import com.pocketsparrow.core.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class LiveShieldEvent(
    val id: String,
    val notificationKey: String = "",
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val snippet: String,
    val threatLevel: Int, // 0: Safe, 1: Suspicious, 2: Malicious
    val category: String,
    val confidence: Float = 0.98f,
    val xaiReason: String,
    val latencyMicros: Long,
    val actionTaken: String, // "PASSED", "BLOCKED", "ALLOWED_ONCE"
    val urlsFound: List<String>
)

object LiveShieldManager {

    private val _isProtectionEnabled = MutableStateFlow(true)
    val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()

    private val _isZeroRetentionEnabled = MutableStateFlow(false)
    val isZeroRetentionEnabled: StateFlow<Boolean> = _isZeroRetentionEnabled.asStateFlow()

    private val _pauseUntilTimestamp = MutableStateFlow(0L)
    val pauseUntilTimestamp: StateFlow<Long> = _pauseUntilTimestamp.asStateFlow()

    private val _sensitivityLevel = MutableStateFlow(0.75f)
    val sensitivityLevel: StateFlow<Float> = _sensitivityLevel.asStateFlow()

    // Default monitored messaging & email packages
    private val defaultMonitored = mapOf(
        "com.whatsapp" to true,
        "org.telegram.messenger" to true,
        "org.thoughtcrime.securesms" to true,
        "com.google.android.apps.messaging" to true,
        "com.google.android.gm" to true,
        "com.instagram.android" to true,
        "com.facebook.orca" to true,
        "com.discord" to true,
        "com.Slack" to true
    )

    private val _monitoredApps = MutableStateFlow<Map<String, Boolean>>(defaultMonitored)
    val monitoredApps: StateFlow<Map<String, Boolean>> = _monitoredApps.asStateFlow()

    private val _liveEvents = MutableStateFlow<List<LiveShieldEvent>>(emptyList())
    val liveEvents: StateFlow<List<LiveShieldEvent>> = _liveEvents.asStateFlow()

    // 500-item LRU Verdict cache keyed by SHA-256
    private val verdictCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, ScanResult>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ScanResult>?): Boolean {
                return size > 500
            }
        }
    )

    // 5-second circular debounce map
    private val debounceMap = ConcurrentHashMap<String, Long>()
    private const val DEBOUNCE_WINDOW_MS = 5000L

    // Temporary Allow-Once set
    private val temporaryAllowlist = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun setProtectionEnabled(enabled: Boolean) {
        _isProtectionEnabled.value = enabled
    }

    fun setZeroRetentionEnabled(enabled: Boolean) {
        _isZeroRetentionEnabled.value = enabled
    }

    fun setSensitivity(level: Float) {
        _sensitivityLevel.value = level.coerceIn(0.1f, 1.0f)
    }

    fun pauseFor15Minutes() {
        _pauseUntilTimestamp.value = System.currentTimeMillis() + (15 * 60 * 1000L)
    }

    fun resumeNow() {
        _pauseUntilTimestamp.value = 0L
    }

    fun isPaused(): Boolean {
        val pauseUntil = _pauseUntilTimestamp.value
        return pauseUntil > 0L && System.currentTimeMillis() < pauseUntil
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        val current = _monitoredApps.value.toMutableMap()
        current[packageName] = enabled
        _monitoredApps.value = current
    }

    fun isAppMonitored(packageName: String): Boolean {
        // If explicitly set, use value. If unspecified third-party chat/sms, default true.
        return _monitoredApps.value[packageName] ?: true
    }

    /**
     * Checks if notification should be dropped due to debouncing within 5s window.
     */
    fun shouldDebounce(debounceKey: String): Boolean {
        val now = System.currentTimeMillis()
        val lastSeen = debounceMap[debounceKey]
        if (lastSeen != null && (now - lastSeen) < DEBOUNCE_WINDOW_MS) {
            return true
        }
        debounceMap[debounceKey] = now

        // Cleanup old keys periodically if size grows
        if (debounceMap.size > 200) {
            val cutoff = now - DEBOUNCE_WINDOW_MS
            debounceMap.entries.removeIf { it.value < cutoff }
        }
        return false
    }

    // 3-second in-memory LRU cache for content hashes (packageName + "|" + title + "|" + text)
    private const val CONTENT_DEBOUNCE_WINDOW_MS = 3000L
    private val contentDebounceCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 500
            }
        }
    )

    fun shouldDebounceContent(contentKey: String): Boolean {
        val now = System.currentTimeMillis()
        val lastSeen = contentDebounceCache[contentKey]
        if (lastSeen != null && (now - lastSeen) < CONTENT_DEBOUNCE_WINDOW_MS) {
            return true
        }
        contentDebounceCache[contentKey] = now
        return false
    }

    fun getCachedVerdict(contentHash: String): ScanResult? {
        return verdictCache[contentHash]
    }

    fun cacheVerdict(contentHash: String, result: ScanResult) {
        verdictCache[contentHash] = result
    }

    fun isAllowedOnce(identifier: String): Boolean {
        return temporaryAllowlist.contains(identifier)
    }

    fun addAllowOnce(identifier: String) {
        temporaryAllowlist.add(identifier)
    }

    private fun normalizeString(s: String): String {
        return s.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
    }

    fun recordEvent(event: LiveShieldEvent) {
        val current = _liveEvents.value.toMutableList()
        val now = System.currentTimeMillis()

        // Deduplicate updates within 5s window
        val existingIndex = current.indexOfFirst { existing ->
            (now - existing.timestamp < 5000L) && (
                (event.notificationKey.isNotEmpty() && existing.notificationKey.isNotEmpty() && existing.notificationKey == event.notificationKey) ||
                (existing.packageName == event.packageName && (
                    normalizeString(existing.snippet) == normalizeString(event.snippet) ||
                    normalizeString(existing.title) == normalizeString(event.title)
                ))
            )
        }

        if (existingIndex >= 0) {
            current[existingIndex] = event.copy(id = current[existingIndex].id)
        } else {
            current.add(0, event)
        }

        if (current.size > 50) {
            _liveEvents.value = current.take(50)
        } else {
            _liveEvents.value = current
        }
    }

    fun clearEvents() {
        _liveEvents.value = emptyList()
    }
}
