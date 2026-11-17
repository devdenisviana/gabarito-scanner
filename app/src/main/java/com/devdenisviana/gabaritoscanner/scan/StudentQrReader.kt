package com.devdenisviana.gabaritoscanner.scan

import android.graphics.Bitmap
import com.devdenisviana.gabaritoscanner.domain.student.QrCode
import com.devdenisviana.gabaritoscanner.domain.student.StudentTag

/** Looks for the student's QR code in the top part of an aligned sheet. */
object StudentQrReader {

    // Regions to try, as fractions of the aligned sheet: (left, top, right, bottom).
    private val regions = listOf(
        floatArrayOf(0.72f, 0.03f, 0.97f, 0.22f), // where the QR is printed
        floatArrayOf(0f, 0f, 1f, 0.30f), // fallback: the whole header
    )

    fun read(sheet: Bitmap): StudentTag? {
        for (region in regions) {
            val x = (region[0] * sheet.width).toInt()
            val y = (region[1] * sheet.height).toInt()
            val width = ((region[2] - region[0]) * sheet.width).toInt().coerceAtMost(sheet.width - x)
            val height = ((region[3] - region[1]) * sheet.height).toInt().coerceAtMost(sheet.height - y)
            if (width <= 0 || height <= 0) continue

            val pixels = IntArray(width * height)
            sheet.getPixels(pixels, 0, width, x, y, width, height)

            val text = QrCode.decode(width, height, pixels) ?: continue
            val tag = StudentTag.decode(text)
            if (tag != null) return tag
        }
        return null
    }
}