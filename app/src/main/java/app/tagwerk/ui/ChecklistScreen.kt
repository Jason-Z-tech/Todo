package app.tagwerk.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.tagwerk.data.Occurrence
import app.tagwerk.data.Task
import java.time.LocalDate

/**
 * Schlichte Abhakliste eines Tages: nach Uhrzeit sortiert, ohne Uhrzeit ganz unten.
 * PLATZHALTER – wird durch die richtige Umsetzung ersetzt.
 */
@Composable
fun ChecklistScreen(
    tasks: List<Task>,
    today: LocalDate,
    day: LocalDate,
    onDayChange: (LocalDate) -> Unit,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Text("Checkliste", modifier = modifier.padding(contentPadding))
}
