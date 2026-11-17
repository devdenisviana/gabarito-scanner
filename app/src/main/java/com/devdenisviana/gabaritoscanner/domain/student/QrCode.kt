package com.devdenisviana.gabaritoscanner.domain.student

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Generates and reads QR codes (pure Kotlin/Java, so it can be unit-tested on the JVM). */
object QrCode {

    /** Builds the QR matrix for [text], scaled to [size] x [size] modules/pixels. */
    fun encode(text: String, size: Int): BitMatrix {
        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        return QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    }

    /** Looks for a QR code in ARGB [pixels]. Returns its text, or null if none was found. */
    fun decode(width: Int, height: Int, pixels: IntArray): String? {
        val source = RGBLuminanceSource(width, height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.CHARACTER_SET to "UTF-8",
            DecodeHintType.TRY_HARDER to true,
        )
        return try {
            QRCodeReader().decode(bitmap, hints).text
        } catch (e: ReaderException) {
            null
        }
    }
}