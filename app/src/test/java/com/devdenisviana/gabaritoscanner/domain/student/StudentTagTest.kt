package com.devdenisviana.gabaritoscanner.domain.student

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentTagTest {

    @Test
    fun `encode gera o formato GS1`() {
        assertEquals("GS1|2024001|Ana Souza", StudentTag("2024001", "Ana Souza").encode())
    }

    @Test
    fun `decode recupera o que o encode gravou`() {
        val original = StudentTag("2024001", "Ana Souza")
        assertEquals(original, StudentTag.decode(original.encode()))
    }

    @Test
    fun `barras e quebras de linha sao trocadas por espaco`() {
        val tag = StudentTag("20|24", "Ana\nSouza")
        assertEquals("GS1|20 24|Ana Souza", tag.encode())
    }

    @Test
    fun `decode rejeita texto de outro QR`() {
        assertNull(StudentTag.decode("https://exemplo.com"))
    }

    @Test
    fun `decode rejeita formato incompleto`() {
        assertNull(StudentTag.decode("GS1|2024001"))
    }

    @Test
    fun `decode rejeita codigo ou nome vazio`() {
        assertNull(StudentTag.decode("GS1||Ana"))
        assertNull(StudentTag.decode("GS1|2024001| "))
    }
}