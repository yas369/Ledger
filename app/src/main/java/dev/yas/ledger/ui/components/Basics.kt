package dev.yas.ledger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import dev.yas.ledger.ledger.RUPEE_SIGN
import dev.yas.ledger.ui.theme.LedgerTheme

@Composable
fun LText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A tap target with no ripple: the pressed item swells in slightly and settles back. */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, label = "press")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = LedgerTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.accentFill else colors.separator),
        contentAlignment = Alignment.Center,
    ) {
        LText(label, LedgerTheme.type.headline, colors.onAccent)
    }
}

@Composable
fun PlainTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LedgerTheme.colors.accent,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .pressable(enabled = enabled, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        LText(
            text = label,
            style = LedgerTheme.type.body,
            color = if (enabled) color else LedgerTheme.colors.textSecondary,
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(colors.grouped)
            .padding(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) colors.card else Color.Transparent)
                    .selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                LText(
                    text = label,
                    style = if (selected) LedgerTheme.type.headline else LedgerTheme.type.body,
                    color = colors.text,
                )
            }
        }
    }
}

/** An inset grouped section: a rounded block on the grouped background. */
@Composable
fun GroupedSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LedgerTheme.colors.card),
        content = content,
    )
}

@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(0.5.dp)
            .background(LedgerTheme.colors.separator),
    )
}

/** Amount entry with a rupee prefix and the decimal keypad. Accepts digits, one-style commas and a dot. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = true,
) {
    val colors = LedgerTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) focusRequester.requestFocus()
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LText(RUPEE_SIGN, LedgerTheme.type.largeTitleAmount, colors.textSecondary)
        BasicTextField(
            value = value,
            onValueChange = { next ->
                if (next.all { it.isDigit() || it == '.' || it == ',' }) onValueChange(next)
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .focusRequester(focusRequester),
            textStyle = LedgerTheme.type.largeTitleAmount.copy(color = colors.text),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            cursorBrush = SolidColor(colors.accent),
        )
    }
}

@Composable
fun EyeIcon(open: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val outline = Path().apply {
            moveTo(w * 0.08f, h * 0.5f)
            cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.92f, h * 0.5f)
            cubicTo(w * 0.7f, h * 0.8f, w * 0.3f, h * 0.8f, w * 0.08f, h * 0.5f)
            close()
        }
        drawPath(outline, tint, style = stroke)
        drawCircle(tint, radius = w * 0.12f, center = center, style = stroke)
        if (!open) {
            drawLine(
                color = tint,
                start = Offset(w * 0.18f, h * 0.84f),
                end = Offset(w * 0.82f, h * 0.16f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun PlusIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val width = 2.5.dp.toPx()
        drawLine(tint, Offset(size.width * 0.2f, center.y), Offset(size.width * 0.8f, center.y), width, StrokeCap.Round)
        drawLine(tint, Offset(center.x, size.height * 0.2f), Offset(center.x, size.height * 0.8f), width, StrokeCap.Round)
    }
}

/**
 * A bottom sheet: Cancel at the top left, the confirming action at the top right. It is a
 * solid surface because it holds a form; there is no backdrop blur in this version.
 */
@Composable
fun LedgerSheet(
    title: String,
    cancelLabel: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LedgerTheme.colors
    BackHandler(onBack = onCancel)
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel,
                ),
        )
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(colors.card)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PlainTextButton(label = cancelLabel, onClick = onCancel)
                    LText(title, LedgerTheme.type.headline, colors.text)
                    PlainTextButton(label = confirmLabel, onClick = onConfirm, enabled = confirmEnabled)
                }
                content()
                Box(modifier = Modifier.height(16.dp))
            }
        }
    }
}
