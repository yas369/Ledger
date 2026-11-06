package dev.yas.ledger.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The named colours from design.md section 5. Screens use these names, never raw values. */
@Immutable
class LedgerColors(
    val bg: Color,
    val grouped: Color,
    val card: Color,
    val text: Color,
    val textSecondary: Color,
    val separator: Color,
    val accent: Color,
    val accentFill: Color,
    val onAccent: Color,
    val over: Color,
    val near: Color,
    val ok: Color,
    val credit: Color,
    val lent: Color,
)

val LightColors = LedgerColors(
    bg = Color(0xFFFFFFFF),
    grouped = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF000000),
    textSecondary = Color(0xFF636366),
    separator = Color(0xFFC6C6C8),
    accent = Color(0xFF0062CC),
    accentFill = Color(0xFF0062CC),
    onAccent = Color(0xFFFFFFFF),
    over = Color(0xFFD70015),
    near = Color(0xFFC93400),
    ok = Color(0xFF1B7A32),
    credit = Color(0xFF1B7A32),
    lent = Color(0xFF636366),
)

val DarkColors = LedgerColors(
    bg = Color(0xFF000000),
    grouped = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    text = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF98989D),
    separator = Color(0xFF38383A),
    accent = Color(0xFF4DA3FF),
    accentFill = Color(0xFF0A6FDB),
    onAccent = Color(0xFFFFFFFF),
    over = Color(0xFFFF453A),
    near = Color(0xFFFF9F0A),
    ok = Color(0xFF30D158),
    credit = Color(0xFF30D158),
    lent = Color(0xFF98989D),
)

val LocalLedgerColors = staticCompositionLocalOf { LightColors }
