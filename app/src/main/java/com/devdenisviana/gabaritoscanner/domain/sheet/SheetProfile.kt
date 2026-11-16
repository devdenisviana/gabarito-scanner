package com.devdenisviana.gabaritoscanner.domain.sheet

/**
 * Describes the layout of an answer sheet (gabarito).
 * Keeping this configurable lets the app support different sheet models.
 */
data class SheetProfile(
    val id: String,
    val name: String,
    val questionCount: Int,
    val optionsPerQuestion: Int,
    val columns: Int,
) {
    init {
        require(questionCount in 1..100) { "questionCount must be between 1 and 100" }
        require(optionsPerQuestion in 2..5) { "optionsPerQuestion must be between 2 and 5" }
        require(columns in 1..4) { "columns must be between 1 and 4" }
    }

    /** Labels of the alternatives, e.g. [A, B, C, D, E]. */
    val optionLabels: List<Char>
        get() = ('A'..'E').take(optionsPerQuestion)

    /** How many question rows each column holds (last column may have fewer). */
    val rowsPerColumn: Int
        get() = (questionCount + columns - 1) / columns

    companion object {
        val DEFAULT = SheetProfile(
            id = "default-20q-5opt",
            name = "Padrão: 20 questões (A–E)",
            questionCount = 20,
            optionsPerQuestion = 5,
            columns = 2,
        )

        /** Cria um perfil de folha escolhendo as colunas automaticamente (até 15 linhas por coluna). */
        fun of(questionCount: Int, optionsPerQuestion: Int): SheetProfile {
            val columns = ((questionCount + 14) / 15).coerceIn(1, 4)
            return SheetProfile(
                id = "q$questionCount-o$optionsPerQuestion",
                name = "$questionCount questões, $optionsPerQuestion alternativas",
                questionCount = questionCount,
                optionsPerQuestion = optionsPerQuestion,
                columns = columns,
            )
        }
    }
}