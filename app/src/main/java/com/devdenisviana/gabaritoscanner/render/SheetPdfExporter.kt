package com.devdenisviana.gabaritoscanner.render

import android.content.Context
import android.graphics.pdf.PdfDocument
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.domain.student.StudentTag
import java.io.File
import java.io.FileOutputStream

/** Renders an answer sheet to a one-page A4 PDF stored in the app's cache folder. */
object SheetPdfExporter {

    fun export(context: Context, profile: SheetProfile, student: StudentTag? = null): File {
        val dir = File(context.cacheDir, "sheets").apply { mkdirs() }
        val suffix = student?.let { "_" + it.code.filter { c -> c.isLetterOrDigit() } }.orEmpty()
        val file = File(dir, "gabarito_${profile.id}$suffix.pdf")

        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(
                SheetRenderer.PAGE_WIDTH,
                SheetRenderer.PAGE_HEIGHT,
                1,
            ).create()

            val page = document.startPage(pageInfo)
            SheetRenderer.draw(page.canvas, profile, student = student)
            document.finishPage(page)

            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return file
    }
}