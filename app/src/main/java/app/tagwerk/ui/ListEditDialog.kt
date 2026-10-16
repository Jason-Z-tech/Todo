package app.tagwerk.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tagwerk.data.ListColors
import app.tagwerk.data.TaskList
import app.tagwerk.data.colorToHex
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.asPastel
import app.tagwerk.ui.theme.pastel
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong

/** Namen der Listenfarben – nur für Bildschirmleser. */
private val ListColorNames = listOf(
    "Pfirsich", "Aprikose", "Vanille", "Limette", "Salbei", "Minze",
    "Lagune", "Himmel", "Lavendel", "Flieder", "Orchidee", "Rosé",
)

/**
 * Dialog zum Anlegen ([initial] = null) oder Bearbeiten einer Liste: Name und Farbe.
 * Die Rückfrage vor dem Löschen zeigt der Aufrufer.
 */
@Composable
fun ListEditDialog(
    initial: TaskList?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Int) -> Unit,
    onDelete: () -> Unit,
) {
    val p = Tagwerk.palette
    val lists = Tagwerk.settings.lists
    // Neue Listen bekommen die erste Farbe, die noch keine andere Liste trägt.
    val defaultColor = remember {
        ListColors.firstOrNull { c -> lists.none { it.color == c } } ?: ListColors.first()
    }
    // Eingaben überstehen das Drehen des Handys, wie der offene Dialog selbst.
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: defaultColor) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    val canSave = name.isNotBlank()
    val preview = TaskList(id = initial?.id ?: "neu", name = name.trim(), color = color)
    val custom = color !in ListColors

    PickerDialogFrame(onDismiss = onDismiss) {
        // Fokus erst im Dialog selbst anfordern – erst dort hängt das Feld am FocusRequester.
        LaunchedEffect(Unit) {
            if (initial == null) runCatching { focus.requestFocus() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (initial == null) "Neue Liste" else "Liste bearbeiten",
                style = MaterialTheme.typography.titleLarge,
                color = p.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (initial != null && canDelete) {
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Löschen", style = MaterialTheme.typography.labelLarge, color = p.danger, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SoftInputField(
            value = name,
            onValueChange = { name = it.take(40) },
            placeholder = "Name der Liste",
            focusRequester = focus,
            onDone = { if (canSave) onSave(name.trim(), color) },
        )

        PickerLabel("Farbe")
        ListColors.chunked(6).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { i, c ->
                    Box(Modifier.weight(1f).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
                        ListSwatch(
                            color = c,
                            name = ListColorNames.getOrElse(rowIndex * 6 + i) { colorToHex(c) },
                            selected = color == c,
                            onClick = { color = c },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        CustomColorChip(
            list = preview,
            custom = custom,
            onClick = { picking = true },
            modifier = Modifier.padding(start = 2.dp),
        )
        if (custom && Color(color).asPastel() != Color(color)) {
            Text(
                "Listen zeigen eigene Farben als zarten Pastellton.",
                style = MaterialTheme.typography.bodySmall,
                color = p.muted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }

        PickerLabel("Vorschau")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
            PreviewPill(
                text = preview.name.ifEmpty { "Deine Liste" },
                color = preview.soft(),
                content = preview.strong(),
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(preview.soft())
                    .border(2.dp, preview.strong(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = preview.strong(), modifier = Modifier.size(16.dp))
            }
        }

        PickerDialogActions(
            confirm = "Speichern",
            enabled = canSave,
            onConfirm = { if (canSave) onSave(name.trim(), color) },
            onDismiss = onDismiss,
        )
    }

    if (picking) {
        ColorPickerDialog(
            initial = Color(color),
            title = "Eigene Farbe",
            onDismiss = { picking = false },
            onPick = {
                color = it.toArgb()
                picking = false
            },
        )
    }
}

/** Vorschau im Stil der ausgewählten [Pill] – nicht antippbar, lange Namen enden einzeilig mit „…“. */
@Composable
private fun PreviewPill(text: String, color: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = color, contentColor = content) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ListSwatch(color: Int, name: String, selected: Boolean, onClick: () -> Unit) {
    val p = Tagwerk.palette
    val list = TaskList(id = "", name = name, color = color)
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(42.dp).border(2.dp, list.strong(), CircleShape))
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(list.pastel()),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (p.isDark) p.background else list.strong(),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** „Eigene“: öffnet den Farbwähler; ist eine eigene Farbe gewählt, steht sie samt Code darin. */
@Composable
private fun CustomColorChip(list: TaskList, custom: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = Tagwerk.palette
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (custom) list.soft() else p.chip,
        contentColor = if (custom) list.strong() else p.ink,
        border = if (custom) BorderStroke(2.dp, list.strong()) else BorderStroke(1.dp, p.line),
    ) {
        Row(
            Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RainbowWheel(Modifier.size(24.dp), inner = if (custom) Color(list.color).asPastel() else null)
            Spacer(Modifier.width(8.dp))
            Text("Eigene", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            if (custom) {
                Spacer(Modifier.width(6.dp))
                Text(colorToHex(list.color), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}
