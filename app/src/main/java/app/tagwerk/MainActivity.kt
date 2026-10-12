package app.tagwerk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.tagwerk.data.TaskStore
import app.tagwerk.reminders.Notifications
import app.tagwerk.ui.TagwerkApp
import app.tagwerk.ui.theme.TagwerkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.createChannel(this)
        TaskStore.get(this).rescheduleAll()
        setContent {
            TagwerkTheme {
                TagwerkApp()
            }
        }
    }
}
