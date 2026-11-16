package com.devdenisviana.gabaritoscanner.domain.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SheetProfileTest {

    @Test
    fun `10 questoes usam 1 coluna`() {
        assertEquals(1, SheetProfile.of(10, 4).columns)
    }

    @Test
    fun `20 questoes usam 2 colunas como o perfil padrao`() {
        assertEquals(2, SheetProfile.of(20, 5).columns)
    }

    @Test
    fun `45 questoes usam 3 colunas`() {
        assertEquals(3, SheetProfile.of(45, 5).columns)
    }

    @Test
    fun `100 questoes usam 4 colunas`() {
        assertEquals(4, SheetProfile.of(100, 5).columns)
    }

    @Test
    fun `id e rotulos refletem a configuracao`() {
        val profile = SheetProfile.of(12, 3)
        assertEquals("q12-o3", profile.id)
        assertEquals(listOf('A', 'B', 'C'), profile.optionLabels)
    }

    @Test
    fun `quantidade invalida e rejeitada`() {
        assertThrows(IllegalArgumentException::class.java) { SheetProfile.of(0, 5) }
        assertThrows(IllegalArgumentException::class.java) { SheetProfile.of(10, 6) }
    }
}