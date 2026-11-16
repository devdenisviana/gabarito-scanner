package com.devdenisviana.gabaritoscanner.domain.student

/** Identificação de um aluno, no formato que vai dentro do QR code. */
data class StudentTag(val code: String, val name: String) {

    /** Texto gravado no QR: "GS1|matricula|nome". */
    fun encode(): String = "$PREFIX|${clean(code)}|${clean(name)}"

    companion object {
        private const val PREFIX = "GS1"

        // Remove os caracteres que quebrariam o formato.
        private fun clean(text: String): String =
            text.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim()

        /** Lê o texto de um QR. Devolve null se não for um QR deste app. */
        fun decode(text: String): StudentTag? {
            val parts = text.trim().split('|')
            if (parts.size != 3 || parts[0] != PREFIX) return null
            val code = parts[1].trim()
            val name = parts[2].trim()
            if (code.isEmpty() || name.isEmpty()) return null
            return StudentTag(code, name)
        }
    }
}