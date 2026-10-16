package app.tagwerk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.tagwerk.data.Settings
import app.tagwerk.data.Task
import app.tagwerk.data.TaskList
import app.tagwerk.data.ThemePreset
import app.tagwerk.ui.theme.Tagwerk
import app.tagwerk.ui.theme.readableInk
import app.tagwerk.ui.theme.soft
import app.tagwerk.ui.theme.strong
import kotlinx.coroutines.delay

/**
 * Einstellungen: Name für die Begrüßung, Farbthema (fertig oder eigene Farbe) und Listen.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    tasks: List<Task>,
    onNameChange: (String) -> Unit,
    onAccentChange: (Int) -> Unit,
    onSaveList: (TaskList) -> Unit,
    onDeleteList: (TaskList) -> Unit,
    newListId: () -> String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val p = Tagwerk.palette
    // Eigener Textzustand: das Feld folgt dem Tippen sofort, gespeichert wird nebenbei.
    var name by rememberSaveable { mutableStateOf(settings.name) }
    var pickingAccent by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }

    // Offene Aufgaben je Liste; unbekannte Listen zählen wie in Settings.list() zur ersten.
    val openCounts = remember(tasks, settings.lists) {
        tasks.filter { !it.done }.groupingBy { settings.list(it.listId).id }.eachCount()
    }
    val customAccent = ThemePreset.entries.none { it.accent == settings.accent }

    // Den Namen erst nach einer kurzen Tipp-Pause speichern (nicht bei jedem Buchstaben
    // die Datei schreiben) – beim Verlassen des Bereichs auf jeden Fall.
    val latestName by rememberUpdatedState(name)
    val saveName by rememberUpdatedState(onNameChange)
    LaunchedEffect(name) {
        delay(400)
        saveName(name)
    }
    DisposableEffect(Unit) {
        onDispose { saveName(latestName) }
    }

    LazyColumn(
        // Tastatur im Querformat: Liste endet über der Tastatur, das Feld bleibt sichtbar.
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = contentPadding,
    ) {
        item(key = "title") {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 2.dp)) {
                Text(
                    "Einstellungen",
                    style = MaterialTheme.typography.displaySmall,
                    color = p.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Name, Farben und Listen – ganz wie du magst.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.muted,
                )
            }
        }

        item(key = "h-greeting") { SettingsLabel("Begrüßung") }
        item(key = "greeting") {
            SettingsCard {
                Column(Modifier.padding(14.dp)) {
                    SoftInputField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        placeholder = "Dein Name",
                        label = "Wie soll ich dich nennen?",
                        capitalization = KeyboardCapitalization.Words,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        buildAnnotatedString {
                            append("So begrüßt dich die App: ")
                            withStyle(SpanStyle(color = p.ink, fontWeight = MaterialTheme.typography.labelLarge.fontWeight)) {
                                append(greeting(name))
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.muted,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }

        item(key = "h-theme") { SettingsLabel("Farbthema") }
        item(key = "theme") {
            SettingsCard {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp).selectableGroup()) {
                    val swatches = ThemePreset.entries.map { it as ThemePreset? } + null
                    swatches.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { preset ->
                                if (preset != null) {
                                    ThemeSwatch(
                                        label = preset.label,
                                        selected = settings.accent == preset.accent,
                                        onClick = { onAccentChange(preset.accent) },
                                        modifier = Modifier.weight(1f),
                                    ) { PresetCircle(Color(preset.accent), selected = settings.accent == preset.accent) }
                                } else {
                                    ThemeSwatch(
                                        label = "Eigene",
                                        selected = customAccent,
                                        onClick = { pickingAccent = true },
                                        modifier = Modifier.weight(1f),
                                    ) { CustomCircle(if (customAccent) Color(settings.accent) else null) }
                                }
                            }
                            // Unvollständige Reihe auffüllen, damit alle Kreise in ihren Spalten bleiben.
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Text(
                        "Tippe auf „Eigene“, um jede Farbe selbst einzugeben – z. B. als Farbcode #F4B6A0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.muted,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                    )
                }
            }
        }

        item(key = "h-lists") { SettingsLabel("Listen") }
        item(key = "lists") {
            SettingsCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    settings.lists.forEach { list ->
                        ListRow(list = list, open = openCounts[list.id] ?: 0, onClick = { editingId = list.id })
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = p.line,
                            modifier = Modifier.padding(start = 60.dp, end = 18.dp),
                        )
                    }
                    NewListRow(onClick = { creating = true })
                }
            }
        }

        item(key = "footer") {
            Text(
                "Alles bleibt nur auf diesem Gerät gespeichert.",
                style = MaterialTheme.typography.bodySmall,
                color = p.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
            )
        }
    }

    if (pickingAccent) {
        ColorPickerDialog(
            initial = Color(settings.accent),
            title = "Eigene Farbe",
            onDismiss = { pickingAccent = false },
            onPick = {
                onAccentChange(it.toArgb())
                pickingAccent = false
            },
        )
    }

    settings.lists.firstOrNull { it.id == editingId }?.let { list ->
        ListEditDialog(
            initial = list,
            canDelete = settings.lists.size > 1,
            onDismiss = { editingId = null },
            onSave = { newName, color ->
                onSaveList(list.copy(name = newName, color = color))
                editingId = null
            },
            onDelete = {
                editingId = null
                deletingId = list.id
            },
        )
    }

    if (creating) {
        ListEditDialog(
            initial = null,
            canDelete = false,
            onDismiss = { creating = false },
            onSave = { newName, color ->
                onSaveList(TaskList(newListId(), newName, color))
                creating = false
            },
            onDelete = {},
        )
    }

    settings.lists.firstOrNull { it.id == deletingId }?.let { list ->
        // Ziel wie in SettingsStore.deleteList: die erste verbleibende Liste.
        val target = settings.lists.firstOrNull { it.id != list.id }
        if (target != null) {
            DeleteListDialog(
                list = list,
                target = target,
                open = openCounts[list.id] ?: 0,
                total = tasks.count { settings.list(it.listId).id == list.id },
                onDismiss = { deletingId = null },
                onConfirm = {
                    deletingId = null
                    onDeleteList(list)
                },
            )
        }
    }
}

@Composable
private fun SettingsLabel(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Tagwerk.palette.muted,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Tagwerk.palette.card,
        contentColor = Tagwerk.palette.ink,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun ThemeSwatch(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circle: @Composable () -> Unit,
) {
    val p = Tagwerk.palette
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(54.dp).border(2.dp, p.accentStrong, CircleShape))
            circle()
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) p.ink else p.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PresetCircle(color: Color, selected: Boolean) {
    val p = Tagwerk.palette
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = color.readableInk(if (p.isDark) p.background else p.ink),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Farbrad für „Eigene“ – mit der gewählten Farbe in der Mitte und einem Stift, falls aktiv. */
@Composable
private fun CustomCircle(custom: Color?) {
    val p = Tagwerk.palette
    Box(Modifier.size(44.dp)) {
        RainbowWheel(Modifier.size(44.dp), inner = custom)
        if (custom != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(p.card)
                    .border(1.dp, p.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = p.ink, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun ListRow(list: TaskList, open: Int, onClick: () -> Unit) {
    val p = Tagwerk.palette
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Bearbeiten", onClick = onClick)
            .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(list.soft())
                .border(2.dp, list.strong().copy(alpha = 0.8f), CircleShape),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            list.name,
            style = MaterialTheme.typography.bodyLarge,
            color = p.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (open == 0) "nichts offen" else "$open offen",
            style = MaterialTheme.typography.labelMedium,
            color = p.muted,
            maxLines = 1,
        )
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Rounded.Edit, contentDescription = null, tint = p.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun NewListRow(onClick: () -> Unit) {
    val p = Tagwerk.palette
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(p.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = p.accentStrong, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text("Neue Liste", style = MaterialTheme.typography.bodyLarge, color = p.accentStrong, maxLines = 1)
    }
}

@Composable
private fun DeleteListDialog(
    list: TaskList,
    target: TaskList,
    open: Int,
    total: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = Tagwerk.palette
    PickerDialogFrame(onDismiss = onDismiss) {
        Text(
            "„${list.name}“ löschen?",
            style = MaterialTheme.typography.titleLarge,
            color = p.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                total == 0 -> "Die Liste ist leer – es gehen keine Aufgaben verloren."
                open == 0 && total == 1 -> "Die erledigte Aufgabe wandert in die Liste „${target.name}“."
                open == 0 -> "Die erledigten Aufgaben wandern in die Liste „${target.name}“."
                open == 1 -> "Die offene Aufgabe wandert in die Liste „${target.name}“."
                else -> "Die $open offenen Aufgaben wandern in die Liste „${target.name}“."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = p.muted,
        )
        PickerDialogActions(
            confirm = "Löschen",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmColor = p.dangerSoft,
            confirmContent = p.danger,
        )
    }
}
