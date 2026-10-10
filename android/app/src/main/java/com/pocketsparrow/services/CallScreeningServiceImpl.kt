package com.pocketsparrow.services

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import com.pocketsparrow.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val startTime = System.currentTimeMillis()
        val rawHandle = callDetails.handle?.schemeSpecificPart ?: ""
        val normalizedNumber = rawHandle.replace(Regex("[^0-9+]"), "")

        // Check if number is in user's contacts (fast-path whitelist)
        val isContact = isNumberInContacts(normalizedNumber)

        // Evaluate STIR/SHAKEN attestation on API 30+ (1 = Connection.VERIFICATION_STATUS_PASSED, 2 = Connection.VERIFICATION_STATUS_FAILED)
        val stirShakenStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (callDetails.callerNumberVerificationStatus) {
                1 -> "VERIFIED"
                2 -> "FAILED"
                else -> "NOT_VERIFIED"
            }
        } else {
            "NOT_AVAILABLE"
        }

        // On-device evaluation within strict 50ms SLA budget
        val verdict = runBlocking {
            withTimeoutOrNull(45) {
                evaluateCallLocally(normalizedNumber, isContact, stirShakenStatus)
            }
        } ?: CallEvaluationVerdict(
            shouldBlock = false,
            isSpam = false,
            category = "TIMEOUT_FALLBACK",
            xaiReason = "Fast-path SLA safety fallback executed."
        )

        val responseBuilder = CallResponse.Builder()
        if (verdict.shouldBlock) {
            responseBuilder
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
        } else {
            responseBuilder
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(verdict.isSpam)
        }

        respondToCall(callDetails, responseBuilder.build())

        // Show caller ID overlay with STIR/SHAKEN & verdict info
        if (normalizedNumber.isNotBlank()) {
            val overlayIntent = Intent(this, CallerIdOverlayService::class.java).apply {
                putExtra("PHONE_NUMBER", normalizedNumber)
                putExtra("IS_SPAM", verdict.isSpam)
                putExtra("SHOULD_BLOCK", verdict.shouldBlock)
                putExtra("CATEGORY", verdict.category)
                putExtra("XAI_REASON", verdict.xaiReason)
                putExtra("STIR_SHAKEN", stirShakenStatus)
                putExtra("LATENCY_MS", System.currentTimeMillis() - startTime)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(overlayIntent)
                } else {
                    startService(overlayIntent)
                }
            } catch (_: Exception) {}
        }
    }

    private fun isNumberInContacts(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun evaluateCallLocally(
        phoneNumber: String,
        isContact: Boolean,
        stirShaken: String
    ): CallEvaluationVerdict {
        if (isContact) {
            return CallEvaluationVerdict(
                shouldBlock = false,
                isSpam = false,
                category = "WHITELISTED_CONTACT",
                xaiReason = "Phone number matches an existing local contact in phonebook."
            )
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val hash = hashTarget(phoneNumber)
        val blockedEntry = db.spamDao().getNumberByHash(hash)

        if (blockedEntry != null) {
            return CallEvaluationVerdict(
                shouldBlock = true,
                isSpam = true,
                category = blockedEntry.category,
                xaiReason = "Number found in encrypted local SQLCipher spam blocklist (reported ${blockedEntry.reportCount} times)."
            )
        }

        // Behavioral heuristics: Toll-free spoofing with failed carrier attestation
        val isTollFree = phoneNumber.startsWith("+1800") || phoneNumber.startsWith("+1888") ||
                phoneNumber.startsWith("800") || phoneNumber.startsWith("888")

        if (isTollFree && stirShaken == "FAILED") {
            return CallEvaluationVerdict(
                shouldBlock = true,
                isSpam = true,
                category = "SPOOFED_TOLL_FREE",
                xaiReason = "High-confidence spoof: Toll-free origination failed STIR/SHAKEN carrier cryptographic verification."
            )
        }

        if (stirShaken == "FAILED") {
            return CallEvaluationVerdict(
                shouldBlock = false,
                isSpam = true,
                category = "UNVERIFIED_ROBOCALL",
                xaiReason = "Carrier attestation failed. Likely spoofed robocall origination."
            )
        }

        return CallEvaluationVerdict(
            shouldBlock = false,
            isSpam = false,
            category = "NORMAL_CALL",
            xaiReason = "Clean on-device behavioral analysis."
        )
    }

    private fun hashTarget(target: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("sparrow_salt_$target".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    data class CallEvaluationVerdict(
        val shouldBlock: Boolean,
        val isSpam: Boolean,
        val category: String,
        val xaiReason: String
    )
}
