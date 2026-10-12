package app.tagwerk.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tagwerk.ui.theme.Tagwerk

/** Kleine, runde Auswahl-Pille – ersetzt die Standard-Chips. */
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selectedColor: Color = Tagwerk.palette.accentSoft,
    selectedContent: Color = Tagwerk.palette.ink,
    enabled: Boolean = true,
) {
    val p = Tagwerk.palette
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) selectedColor else p.chip,
        contentColor = if (selected) selectedContent else if (enabled) p.ink else p.muted,
        border = if (selected) null else BorderStroke(1.dp, p.line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Runder Abhak-Kreis. Beim Abhaken füllt er sich mit dem Pastellton
 * und das Häkchen wird nachgezeichnet.
 */
@Composable
fun CheckCircle(
    checked: Boolean,
    fill: Color,
    stroke: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "check",
    )
    val tick = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }

    Box(
        modifier = modifier
            .size(size + 14.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val strokeWidth = 2.dp.toPx()
            drawCircle(color = fill, radius = r * progress)
            drawCircle(
                color = stroke.copy(alpha = 0.55f + 0.45f * progress),
                radius = r - strokeWidth / 2f,
                style = Stroke(width = strokeWidth),
            )
            if (progress > 0f) {
                val w = this.size.width
                tick.reset()
                tick.moveTo(w * 0.28f, w * 0.52f)
                tick.lineTo(w * 0.44f, w * 0.67f)
                tick.lineTo(w * 0.73f, w * 0.36f)
                measure.setPath(tick, false)
                segment.reset()
                measure.getSegment(0f, measure.length * progress, segment, true)
                drawPath(
                    segment,
                    color = stroke,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier, accent: Color = Tagwerk.palette.muted, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = Tagwerk.palette.muted.copy(alpha = 0.7f),
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = Tagwerk.palette
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = p.accent,
        contentColor = p.onAccent,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 22.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Neue Aufgabe", style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Ruhiger Leerzustand mit drei weichen Pastellkreisen. */
@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val p = Tagwerk.palette
    val peach = Color(0xFFFBD3C2)
    val sage = Color(0xFFD4E6CC)
    val lilac = Color(0xFFE0D6F0)
    val alpha = if (p.isDark) 0.45f else 1f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(width = 120.dp, height = 84.dp)) {
            val u = size.width / 120f
            drawCircle(lilac.copy(alpha = alpha), radius = 30 * u, center = Offset(40 * u, 46 * u))
            drawCircle(peach.copy(alpha = alpha), radius = 26 * u, center = Offset(76 * u, 38 * u))
            drawCircle(sage.copy(alpha = alpha), radius = 16 * u, center = Offset(68 * u, 66 * u))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = p.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center)
    }
}
