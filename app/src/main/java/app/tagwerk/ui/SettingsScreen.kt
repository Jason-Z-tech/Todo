package app.tagwerk.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.tagwerk.data.Settings
import app.tagwerk.data.Task
import app.tagwerk.data.TaskList

/**
 * Einstellungen: Name für die Begrüßung, Farbthema (fertig oder eigene Farbe) und Listen.
 * PLATZHALTER – wird durch die richtige Umsetzung ersetzt.
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
    Text("Einstellungen", modifier = modifier.padding(contentPadding))
}
