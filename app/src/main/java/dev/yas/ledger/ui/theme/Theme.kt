package dev.yas.ledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object LedgerTheme {
    val colors: LedgerColors
        @Composable get() = LocalLedgerColors.current

    val type: LedgerTypography
        @Composable get() = LocalLedgerTypography.current
}

@Composable
fun LedgerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLedgerColors provides if (darkTheme) DarkColors else LightColors,
        LocalLedgerTypography provides LedgerTypography(),
        content = content,
    )
}
