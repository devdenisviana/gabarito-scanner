package com.devdenisviana.gabaritoscanner.render

import android.content.Context
import android.graphics.pdf.PdfDocument
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import java.io.File
import java.io.FileOutputStream

/** Renders an answer sheet to a one-page A4 PDF stored in the app's cache folder. */
object SheetPdfExporter {

    fun export(context: Context, profile: SheetProfile): File {
        val dir = File(context.cacheDir, "sheets").apply { mkdirs() }
        val file = File(dir, "gabarito_${profile.id}.pdf")

        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(
                SheetRenderer.PAGE_WIDTH,
                SheetRenderer.PAGE_HEIGHT,
                1,
            ).create()

            val page = document.startPage(pageInfo)
            SheetRenderer.draw(page.canvas, profile)
            document.finishPage(page)

            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return file
    }
}