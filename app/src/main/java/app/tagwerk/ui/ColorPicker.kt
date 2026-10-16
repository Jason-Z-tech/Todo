package app.tagwerk.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Farbwähler: Farbe per Hex-Code eingeben oder mit Reglern einstellen.
 * PLATZHALTER – wird durch die richtige Umsetzung ersetzt.
 */
@Composable
fun ColorPickerDialog(
    initial: Color,
    title: String,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = { TextButton(onClick = { onPick(initial) }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
