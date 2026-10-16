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
 * Kalender mit Umschalter Tag / Woche / Monat.
 * PLATZHALTER – wird durch die richtige Umsetzung ersetzt.
 */
@Composable
fun CalendarScreen(
    tasks: List<Task>,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onToggle: (Occurrence) -> Unit,
    onOpen: (Task) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Text("Kalender", modifier = modifier.padding(contentPadding))
}
