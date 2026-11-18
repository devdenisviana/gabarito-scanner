package com.devdenisviana.gabaritoscanner.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreBadDark
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreBadLight
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreGoodDark
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreGoodLight

@Composable
fun goodColor(): Color = if (isSystemInDarkTheme()) ScoreGoodDark else ScoreGoodLight

@Composable
fun badColor(): Color = if (isSystemInDarkTheme()) ScoreBadDark else ScoreBadLight

/** Amber, used for blank answers and double marks. */
@Composable
fun warnColor(): Color = if (isSystemInDarkTheme()) Color(0xFFF59E0B) else Color(0xFFD97706)

/** Green with 60% of correct answers or more, red below that. */
@Composable
fun scoreColor(ratio: Double): Color = if (ratio >= 0.6) goodColor() else badColor()