package app.tagwerk.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.tagwerk.data.ListColors
import app.tagwerk.data.TaskList

/**
 * Dialog zum Anlegen ([initial] = null) oder Bearbeiten einer Liste: Name und Farbe.
 * PLATZHALTER – wird durch die richtige Umsetzung ersetzt.
 */
@Composable
fun ListEditDialog(
    initial: TaskList?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Int) -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neue Liste" else "Liste bearbeiten") },
        confirmButton = {
            TextButton(onClick = { onSave(initial?.name ?: "Neue Liste", initial?.color ?: ListColors.first()) }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
