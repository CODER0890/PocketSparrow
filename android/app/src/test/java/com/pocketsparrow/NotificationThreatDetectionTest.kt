package com.pocketsparrow

import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.services.LiveShieldManager
import com.pocketsparrow.services.NotificationExtractor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NotificationThreatDetectionTest {

    @Before
    fun setUp() {
        LiveShieldManager.resumeNow()
        LiveShieldManager.setProtectionEnabled(true)
        LiveShieldManager.setZeroRetentionEnabled(false)
    }

    @Test
    fun testUrlExtractionPerformanceUnderTwoMillis() {
        // Warmup to avoid JVM classloading/regex compilation timing artifacts
        NotificationExtractor.extractUrls("https://warmup.test")

        val sampleText = "URGENT: Verify your account immediately at https://security-paypal.test and backup at http://bank-update.test/login"
        val start = System.nanoTime()
        val urls = NotificationExtractor.extractUrls(sampleText)
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000f

        assertTrue("Extraction took ${elapsedMillis}ms, must be <2ms", elapsedMillis < 2.0f)
        assertEquals(2, urls.size)
        assertTrue(urls.contains("https://security-paypal.test"))
        assertTrue(urls.contains("http://bank-update.test/login"))
    }

    @Test
    fun testSha256HashingConsistency() {
        val input = "Test Notification Payload"
        val hash1 = NotificationExtractor.sha256(input)
        val hash2 = NotificationExtractor.sha256(input)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun testDebounceWindowDropsDuplicates() {
        val testKey = "com.whatsapp:sample_hash_123"
        val firstCall = LiveShieldManager.shouldDebounce(testKey)
        assertFalse("First notification should not be debounced", firstCall)

        val immediateDuplicate = LiveShieldManager.shouldDebounce(testKey)
        assertTrue("Immediate duplicate should be debounced within 5s window", immediateDuplicate)
    }

    @Test
    fun testVerdictCachingInstantHit() {
        val testHash = "dummy_sha256_hash_456"
        val scanResult = NativeBridge.scan(NativeBridge.CONTENT_TYPE_SMS_TEXT, "Safe message")
        LiveShieldManager.cacheVerdict(testHash, scanResult)

        val start = System.nanoTime()
        val cached = LiveShieldManager.getCachedVerdict(testHash)
        val elapsedMicros = (System.nanoTime() - start) / 1000

        assertNotNull(cached)
        assertEquals(scanResult.threatLevel, cached!!.threatLevel)
        assertTrue("Cache retrieval must be <100µs, took ${elapsedMicros}µs", elapsedMicros < 100)
    }

    @Test
    fun testPauseFor15Minutes() {
        assertFalse(LiveShieldManager.isPaused())
        LiveShieldManager.pauseFor15Minutes()
        assertTrue(LiveShieldManager.isPaused())
        assertTrue(LiveShieldManager.pauseUntilTimestamp.value > System.currentTimeMillis())

        LiveShieldManager.resumeNow()
        assertFalse(LiveShieldManager.isPaused())
    }

    @Test
    fun testZeroRetentionModeToggle() {
        assertFalse(LiveShieldManager.isZeroRetentionEnabled.value)
        LiveShieldManager.setZeroRetentionEnabled(true)
        assertTrue(LiveShieldManager.isZeroRetentionEnabled.value)
        LiveShieldManager.setZeroRetentionEnabled(false)
        assertFalse(LiveShieldManager.isZeroRetentionEnabled.value)
    }

    @Test
    fun testRfc2606SafeTestCases() {
        // 1. PayPal spoof
        val paypalResult = NativeBridge.scan(
            NativeBridge.CONTENT_TYPE_SMS_TEXT,
            "Security alert: Unauthorized login from Moscow. Verify identity at https://security-paypal.test"
        )
        // 2. Chase wire transfer
        val chaseResult = NativeBridge.scan(
            NativeBridge.CONTENT_TYPE_SMS_TEXT,
            "URGENT: wire transfer of $4,850 pending. Account suspended. Confirm at https://chase-login.test"
        )
        assertTrue("PayPal/Wire scam must be classified as threat", paypalResult.threatLevel > 0 || chaseResult.threatLevel > 0)

        // 3. Normal lunch chat
        val lunchResult = NativeBridge.scan(
            NativeBridge.CONTENT_TYPE_SMS_TEXT,
            "Hey! Are we still meeting for lunch at 12:30pm today at the bistro downtown?"
        )
        assertEquals("Casual chat must be classified as SAFE", 0, lunchResult.threatLevel)

        // 4. Safe delivery update
        val deliveryResult = NativeBridge.scan(
            NativeBridge.CONTENT_TYPE_SMS_TEXT,
            "Your book order #84920 has been delivered to your front porch. Have a wonderful weekend!"
        )
        assertEquals("Delivery update must be classified as SAFE", 0, deliveryResult.threatLevel)
    }
}
