package com.devdenisviana.gabaritoscanner.domain.student

import com.google.zxing.common.BitMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeTest {

    private fun toPixels(matrix: BitMatrix): IntArray {
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                pixels[y * matrix.width + x] =
                    if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        return pixels
    }

    private fun roundTrip(text: String): String? {
        val matrix = QrCode.encode(text, 300)
        return QrCode.decode(matrix.width, matrix.height, toPixels(matrix))
    }

    @Test
    fun `texto simples vai e volta`() {
        assertEquals("GS1|2024001|Ana Souza", roundTrip("GS1|2024001|Ana Souza"))
    }

    @Test
    fun `acentos sao preservados`() {
        assertEquals("GS1|42|José da Conceição", roundTrip("GS1|42|José da Conceição"))
    }

    @Test
    fun `StudentTag completo vai e volta`() {
        val tag = StudentTag("2024001", "Ana Souza")
        assertEquals(tag, StudentTag.decode(roundTrip(tag.encode())!!))
    }

    @Test
    fun `imagem em branco nao tem QR`() {
        val white = IntArray(100 * 100) { 0xFFFFFFFF.toInt() }
        assertNull(QrCode.decode(100, 100, white))
    }
}