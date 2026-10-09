package com.pocketsparrow.scanners

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import java.nio.ByteBuffer

class QrCodeAnalyzer(
    private val onQrScanned: (rawPayload: String, result: ScanResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()
    private var isThrottled = false

    override fun analyze(image: ImageProxy) {
        if (isThrottled) {
            image.close()
            return
        }

        val buffer: ByteBuffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = image.width
        val height = image.height

        val source = PlanarYUVLuminanceSource(
            data, width, height, 0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decodeWithState(bitmap)
            val rawText = result.text
            if (!rawText.isNullOrBlank()) {
                isThrottled = true
                // Evaluate QR payload on-device through NativeBridge
                val scanRes = NativeBridge.scan(NativeBridge.CONTENT_TYPE_QR_PAYLOAD, rawText)
                onQrScanned(rawText, scanRes)
            }
        } catch (_: Exception) {
            // No QR found in current frame
        } finally {
            image.close()
        }
    }

    fun resetThrottle() {
        isThrottled = false
    }
}
