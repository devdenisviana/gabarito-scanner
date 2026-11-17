package com.devdenisviana.gabaritoscanner.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.domain.student.QrCode
import com.devdenisviana.gabaritoscanner.domain.student.StudentTag

/**
 * Draws a printable answer sheet on an Android [Canvas] (A4, PDF points).
 * Bubble positions come from [SheetLayout], so what is printed matches what the scanner looks for.
 */
object SheetRenderer {

    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    /** Distance from the page edge to the CENTER of each corner marker. */
    const val MARKER_CENTER_INSET = 30f
    private const val MARKER_SIZE = 24f

    // Area between the centers of the four markers (matches SheetLayout's normalized space).
    private const val AREA_LEFT = MARKER_CENTER_INSET
    private const val AREA_TOP = MARKER_CENTER_INSET
    private const val AREA_WIDTH = PAGE_WIDTH - 2 * MARKER_CENTER_INSET
    private const val AREA_HEIGHT = PAGE_HEIGHT - 2 * MARKER_CENTER_INSET

    // Where the student's QR code is printed (page points).
    private const val QR_LEFT = 440f
    private const val QR_TOP = 80f
    private const val QR_SIZE = 90f

    fun draw(
        canvas: Canvas,
        profile: SheetProfile,
        title: String = "Folha de Respostas",
        student: StudentTag? = null,
    ) {
        val layout = SheetLayout(profile)
        val radiusPx = layout.bubbleRadius * AREA_WIDTH

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val dashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }
        val titleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val centerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val leftText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.LEFT
        }
        val numberText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawColor(Color.WHITE)

        // 1) Corner markers (solid black squares)
        val half = MARKER_SIZE / 2f
        for (cx in listOf(MARKER_CENTER_INSET, PAGE_WIDTH - MARKER_CENTER_INSET)) {
            for (cy in listOf(MARKER_CENTER_INSET, PAGE_HEIGHT - MARKER_CENTER_INSET)) {
                canvas.drawRect(cx - half, cy - half, cx + half, cy + half, fill)
            }
        }

        // 2) Header: title, handwritten fields, instructions
        canvas.drawText(title, PAGE_WIDTH / 2f, 62f, titleText)

        val lineLeft = AREA_LEFT + 10f
        val lineRight = lineLeft + 330f
        listOf("Nome:" to 105f, "Turma:" to 135f, "Data:" to 165f).forEach { (label, y) ->
            canvas.drawText(label, lineLeft, y, leftText)
            canvas.drawLine(lineLeft + 40f, y + 2f, lineRight, y + 2f, stroke)
        }
        if (student != null) {
            canvas.drawText(student.name, lineLeft + 45f, 102f, leftText)
        }

        canvas.drawText(
            "Preencha totalmente a bolinha com caneta preta ou azul escura. Não rasure.",
            PAGE_WIDTH / 2f,
            215f,
            centerText,
        )

        // 3) Student QR code, or a dashed placeholder when no student is given
        if (student == null) {
            canvas.drawRect(QR_LEFT, QR_TOP, QR_LEFT + QR_SIZE, QR_TOP + QR_SIZE, dashed)
            canvas.drawText("QR", QR_LEFT + QR_SIZE / 2f, QR_TOP + QR_SIZE / 2f + 3f, centerText)
        } else {
            drawQr(canvas, student.encode(), QR_LEFT, QR_TOP, QR_SIZE)
            canvas.drawText(student.code, QR_LEFT + QR_SIZE / 2f, QR_TOP + QR_SIZE + 12f, centerText)
        }

        // 4) Column headers (A, B, C...) above the first row of each column
        for (col in 0 until profile.columns) {
            val firstQuestion = col * profile.rowsPerColumn + 1
            if (firstQuestion > profile.questionCount) continue
            profile.optionLabels.forEachIndexed { index, label ->
                val c = layout.bubbleCenter(firstQuestion, index)
                canvas.drawText(
                    label.toString(),
                    AREA_LEFT + c.x * AREA_WIDTH,
                    AREA_TOP + c.y * AREA_HEIGHT - radiusPx - 6f,
                    centerText,
                )
            }
        }

        // 5) Question numbers, to the left of each row
        for (question in 1..profile.questionCount) {
            val c = layout.bubbleCenter(question, 0)
            canvas.drawText(
                question.toString(),
                AREA_LEFT + c.x * AREA_WIDTH - radiusPx - 8f,
                AREA_TOP + c.y * AREA_HEIGHT + 3.5f,
                numberText,
            )
        }

        // 6) Bubbles
        layout.allBubbles().forEach { cell ->
            canvas.drawCircle(
                AREA_LEFT + cell.center.x * AREA_WIDTH,
                AREA_TOP + cell.center.y * AREA_HEIGHT,
                radiusPx,
                stroke,
            )
        }
    }

    /** Draws [text] as a QR code inside the square starting at ([left], [top]) with side [size]. */
    private fun drawQr(canvas: Canvas, text: String, left: Float, top: Float, size: Float) {
        // No anti-aliasing, so neighbouring modules don't leave thin light seams.
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val matrix = QrCode.encode(text, 1)
        val module = size / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    canvas.drawRect(
                        left + x * module,
                        top + y * module,
                        left + (x + 1) * module,
                        top + (y + 1) * module,
                        paint,
                    )
                }
            }
        }
    }
}