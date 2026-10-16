package app.tagwerk.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.tagwerk.data.ListColors
import app.tagwerk.data.ThemePreset
import app.tagwerk.data.colorToHex
import app.tagwerk.data.parseColor
import app.tagwerk.ui.theme.Hsl
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.readableInk
import app.tagwerk.ui.theme.toHsl
import kotlin.math.roundToInt

/** Weicher Regenbogen für das Farbrad und den Farbton-Regler. */
internal val PickerRainbow: List<Color> = (0..6).map { Hsl(it * 60f, 0.85f, 0.66f).toColor() }

/** Vorschläge: oben die Farbthemen, darunter ihre zarteren Geschwister aus den Listenfarben. */
private val Suggestions: List<Int> =
    ThemePreset.entries.map { it.accent } + listOf(0, 11, 9, 7, 5, 4, 2).map { ListColors[it] }

private val ThumbRadius = 15.dp

/**
 * Farbwähler: Farbe als Farbcode eingeben oder mit drei Reglern
 * (Farbton, Sättigung, Helligkeit) einstellen. Beides bleibt im Gleichklang.
 */
@Composable
fun ColorPickerDialog(
    initial: Color,
    title: String,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
) {
    val p = Tagwerk.palette
    val start = remember { initial.copy(alpha = 1f) }
    // Der Farbcode übersteht auch das Drehen des Handys; die Regler folgen ihm.
    var hex by rememberSaveable { mutableStateOf(colorToHex(start.toArgb())) }
    var hsl by remember { mutableStateOf(parseColor(hex)?.let { Color(it).toHsl() } ?: start.toHsl()) }
    val parsed = parseColor(hex)
    val color = if (parsed != null) Color(parsed) else hsl.toColor()
    val darkInk = if (p.isDark) p.background else p.ink

    fun slide(next: Hsl) {
        hsl = next
        hex = colorToHex(next.toColor().toArgb())
    }

    fun type(text: String) {
        // Leerzeichen (z. B. beim Einfügen) fallen weg; Regler folgen genau dem, was im Feld steht.
        val clean = text.filterNot { it.isWhitespace() }.take(9)
        hex = clean
        val argb = parseColor(clean) ?: return
        val typed = Color(argb).toHsl()
        // Bei Grautönen bleibt der Farbton stehen, damit der Regler nicht springt.
        hsl = if (typed.saturation < 0.002f) typed.copy(hue = hsl.hue) else typed
    }

    PickerDialogFrame(onDismiss = onDismiss) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = p.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))

        val previewShape = RoundedCornerShape(20.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(previewShape)
                .background(color)
                .border(1.dp, p.line, previewShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                colorToHex(color.toArgb()),
                style = MaterialTheme.typography.titleLarge,
                color = color.readableInk(darkInk),
            )
        }

        Spacer(Modifier.height(14.dp))
        SoftInputField(
            value = hex,
            onValueChange = ::type,
            placeholder = "#F4B6A0",
            inlineLabel = "Farbcode",
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            error = parsed == null,
            modifier = Modifier.semantics { contentDescription = "Farbcode" },
        )
        Box(Modifier.fillMaxWidth().height(22.dp).padding(start = 4.dp, top = 5.dp)) {
            if (parsed == null) {
                Text("Bitte als #RRGGBB eingeben", style = MaterialTheme.typography.bodySmall, color = p.danger)
            }
        }

        ColorSlider(
            label = "Farbton",
            valueText = "${hsl.hue.roundToInt()}°",
            value = hsl.hue / 360f,
            colors = PickerRainbow,
            thumb = color,
            onChange = { slide(hsl.copy(hue = it * 360f)) },
        )
        ColorSlider(
            label = "Sättigung",
            valueText = "${(hsl.saturation * 100).roundToInt()} %",
            value = hsl.saturation,
            colors = listOf(hsl.copy(saturation = 0f).toColor(), hsl.copy(saturation = 1f).toColor()),
            thumb = color,
            onChange = { slide(hsl.copy(saturation = it)) },
        )
        ColorSlider(
            label = "Helligkeit",
            valueText = "${(hsl.lightness * 100).roundToInt()} %",
            value = hsl.lightness,
            colors = listOf(
                hsl.copy(lightness = 0f).toColor(),
                hsl.copy(lightness = 0.5f).toColor(),
                hsl.copy(lightness = 1f).toColor(),
            ),
            thumb = color,
            onChange = { slide(hsl.copy(lightness = it)) },
        )

        PickerLabel("Vorschläge")
        Suggestions.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { argb ->
                    Box(Modifier.weight(1f).padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                        SuggestionDot(argb = argb, selected = parsed == argb, onClick = { type(colorToHex(argb)) })
                    }
                }
            }
        }

        PickerDialogActions(
            confirm = "Übernehmen",
            enabled = parsed != null,
            onConfirm = { if (parsed != null) onPick(Color(parsed)) },
            onDismiss = onDismiss,
            confirmColor = color,
            confirmContent = color.readableInk(darkInk),
        )
    }
}

/** Waagrechter Farbregler zum Tippen und Ziehen, mit großem Griff. */
@Composable
private fun ColorSlider(
    label: String,
    valueText: String,
    value: Float,
    colors: List<Color>,
    thumb: Color,
    onChange: (Float) -> Unit,
) {
    val p = Tagwerk.palette
    val change by rememberUpdatedState(onChange)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = p.ink, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = p.muted)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
                    setProgress { target ->
                        change(target.coerceIn(0f, 1f))
                        true
                    }
                }
                .pointerInput(Unit) {
                    val r = ThumbRadius.toPx()
                    fun at(x: Float) = ((x - r) / (size.width - 2 * r).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        // Gezogen wird erst nach einer waagrechten Bewegung –
                        // senkrechtes Wischen scrollt den Dialog und lässt die Farbe in Ruhe.
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { move, _ -> move.consume() }
                        if (drag != null) {
                            change(at(drag.position.x))
                            horizontalDrag(drag.id) { move ->
                                move.consume()
                                change(at(move.position.x))
                            }
                        } else if (currentEvent.changes.any { it.id == down.id && it.changedToUp() }) {
                            // Einfaches Tippen springt an die Stelle.
                            change(at(down.position.x))
                        }
                    }
                },
        ) {
            val r = ThumbRadius.toPx()
            val trackHeight = 14.dp.toPx()
            val top = (size.height - trackHeight) / 2f
            val corner = CornerRadius(trackHeight / 2f)
            drawRoundRect(
                brush = Brush.horizontalGradient(colors, startX = r, endX = size.width - r),
                topLeft = Offset(0f, top),
                size = Size(size.width, trackHeight),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = p.line,
                topLeft = Offset(0f, top),
                size = Size(size.width, trackHeight),
                cornerRadius = corner,
                style = Stroke(1.dp.toPx()),
            )
            val center = Offset(r + value.coerceIn(0f, 1f) * (size.width - 2 * r), size.height / 2f)
            drawCircle(p.ink.copy(alpha = 0.14f), radius = r + 0.5.dp.toPx(), center = center + Offset(0f, 1.dp.toPx()))
            drawCircle(p.card, radius = r, center = center)
            drawCircle(thumb, radius = r - 4.dp.toPx(), center = center)
            drawCircle(p.line, radius = r - 4.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
private fun SuggestionDot(argb: Int, selected: Boolean, onClick: () -> Unit) {
    val p = Tagwerk.palette
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Vorschlag ${colorToHex(argb)}" },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(36.dp).border(2.dp, p.accentStrong, CircleShape))
        Box(
            Modifier
                .size(if (selected) 26.dp else 28.dp)
                .clip(CircleShape)
                .background(Color(argb)),
        )
    }
}

/** Rundes Farbrad als Zeichen für „eigene Farbe“; [inner] zeigt die gewählte Farbe in der Mitte. */
@Composable
internal fun RainbowWheel(modifier: Modifier = Modifier, inner: Color? = null) {
    val p = Tagwerk.palette
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(PickerRainbow, center), radius = r)
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0f)), center, r),
            radius = r,
        )
        if (inner != null) {
            drawCircle(p.card, radius = r * 0.62f)
            drawCircle(inner, radius = r * 0.62f - 2.dp.toPx())
        }
    }
}

/** Rahmen für die Dialoge in den Einstellungen: Karte mit runden Ecken, scrollbar. */
@Composable
internal fun PickerDialogFrame(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = Tagwerk.palette
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = p.card,
            contentColor = p.ink,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp),
                content = content,
            )
        }
    }
}

/**
 * „Abbrechen“ und ein gefüllter Bestätigen-Knopf, rechtsbündig. Reicht der Platz
 * nicht (große Schrift), rutscht der Knopf in die nächste Zeile statt abgeschnitten zu werden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PickerDialogActions(
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    enabled: Boolean = true,
    confirmColor: Color = Tagwerk.palette.accent,
    confirmContent: Color = Tagwerk.palette.onAccent,
) {
    val p = Tagwerk.palette
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterVertically)) {
            Text("Abbrechen", style = MaterialTheme.typography.labelLarge, color = p.muted, maxLines = 1)
        }
        Surface(
            onClick = onConfirm,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            color = if (enabled) confirmColor else p.chip,
            contentColor = if (enabled) confirmContent else p.muted,
            border = BorderStroke(1.dp, if (enabled) p.ink.copy(alpha = 0.06f) else p.line),
        ) {
            Text(
                confirm,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

/** Kleine Abschnittsüberschrift in Dialogen (wie im Aufgaben-Editor). */
@Composable
internal fun PickerLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Tagwerk.palette.muted,
        modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * Einzeiliges Eingabefeld in weicher Pastellbox. [label] steht klein darüber,
 * [inlineLabel] links in derselben Zeile – beide gehören zum Feld selbst.
 */
@Composable
internal fun SoftInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    inlineLabel: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: Boolean = false,
    focusRequester: FocusRequester? = null,
    onDone: (() -> Unit)? = null,
) {
    val p = Tagwerk.palette
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val borderColor = when {
        error -> p.danger.copy(alpha = 0.55f)
        focused -> p.accent
        else -> p.chip
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused },
        textStyle = textStyle.copy(color = p.ink),
        singleLine = true,
        cursorBrush = SolidColor(p.accentStrong),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            defaultKeyboardAction(ImeAction.Done)
            onDone?.invoke()
        }),
        decorationBox = { inner ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(p.chip)
                    .border(1.5.dp, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = if (label != null) 11.dp else 13.dp),
            ) {
                if (label != null) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (inlineLabel != null) {
                        Text(inlineLabel, style = MaterialTheme.typography.labelLarge, color = p.muted, maxLines = 1)
                        Spacer(Modifier.width(14.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = textStyle.copy(color = p.muted.copy(alpha = 0.75f)), maxLines = 1)
                        }
                        inner()
                    }
                }
            }
        },
    )
}
