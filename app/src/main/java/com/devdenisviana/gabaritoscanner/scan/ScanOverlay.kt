package com.devdenisviana.gabaritoscanner.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import kotlin.math.roundToInt

/** Converts this bitmap to a [GrayImage] (luminance 0..255) so [BubbleReader] can analyze it. */
fun Bitmap.toGrayImage(): GrayImage {
    val argb = IntArray(width * height)
    getPixels(argb, 0, width, 0, 0, width, height)
    val gray = IntArray(argb.size) { index ->
        val pixel = argb[index]
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        (0.299 * red + 0.587 * green + 0.114 * blue).roundToInt()
    }
    return GrayImage(width, height, gray)
}

/** Draws the reading result on top of the aligned sheet, for visual verification. */
object ScanOverlay {

    fun draw(sheet: Bitmap, layout: SheetLayout, readings: List<QuestionReading>): Bitmap {
        val output = sheet.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val radius = layout.bubbleRadius * output.width

        val green = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 0, 200, 0)
            style = Paint.Style.FILL
        }
        val red = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 230, 0, 0)
            style = Paint.Style.FILL
        }

        for (reading in readings) {
            val (indices, paint) = when (val answer = reading.answer) {
                is Answer.Marked -> listOf(answer.optionIndex) to green
                is Answer.Multiple -> answer.optionIndices to red
                Answer.Blank -> emptyList<Int>() to green
            }
            for (optionIndex in indices) {
                val center = layout.bubbleCenter(reading.question, optionIndex)
                canvas.drawCircle(center.x * output.width, center.y * output.height, radius, paint)
            }
        }
        return output
    }
}