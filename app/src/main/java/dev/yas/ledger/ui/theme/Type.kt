package dev.yas.ledger.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.yas.ledger.R

private val Inter = FontFamily(
    Font(
        resId = R.font.inter,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
)

/** Fixed-width numerals so amounts line up in columns. */
private fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")

/** The only text sizes the app uses (design.md section 4). */
@Immutable
class LedgerTypography(
    val largeTitle: TextStyle = style(34, 41),
    val title2: TextStyle = style(22, 28),
    val title3: TextStyle = style(20, 25),
    val headline: TextStyle = style(17, 22, FontWeight.SemiBold),
    val body: TextStyle = style(17, 22),
    val subheadline: TextStyle = style(15, 20),
    val footnote: TextStyle = style(13, 18),
    val caption2: TextStyle = style(11, 13),
) {
    val largeTitleAmount: TextStyle = largeTitle.tabular()
    val headlineAmount: TextStyle = headline.tabular()
    val bodyAmount: TextStyle = body.tabular()
    val subheadlineAmount: TextStyle = subheadline.tabular()
}

val LocalLedgerTypography = staticCompositionLocalOf { LedgerTypography() }
