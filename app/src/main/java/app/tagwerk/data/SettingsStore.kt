package app.tagwerk.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Hält die [Settings] und schreibt sie als JSON-Datei. */
class SettingsStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val file = AtomicFile(File(appContext.filesDir, "settings.json"))
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun setName(name: String) = mutate { it.copy(name = name.take(40)) }

    fun setAccent(argb: Int) = mutate { it.copy(accent = argb or 0xFF000000.toInt()) }

    /** Legt eine Liste an oder ändert Name/Farbe einer bestehenden. */
    fun saveList(list: TaskList) = mutate { s ->
        val clean = list.copy(name = list.name.trim().take(40).ifEmpty { "Liste" })
        if (s.lists.any { it.id == clean.id }) s.copy(lists = s.lists.map { if (it.id == clean.id) clean else it })
        else s.copy(lists = s.lists + clean)
    }

    /**
     * Löscht eine Liste. Ihre Aufgaben wandern in die erste verbleibende Liste.
     * Die letzte Liste lässt sich nicht löschen.
     */
    fun deleteList(id: String): Boolean {
        val current = _settings.value
        if (current.lists.size <= 1 || current.lists.none { it.id == id }) return false
        val remaining = current.lists.filterNot { it.id == id }
        mutate { it.copy(lists = remaining) }
        TaskStore.get(appContext).moveToList(fromId = id, toId = remaining.first().id)
        return true
    }

    fun newListId(): String = "L${System.currentTimeMillis()}"

    @Synchronized
    private fun mutate(transform: (Settings) -> Settings) {
        val updated = transform(_settings.value)
        if (updated == _settings.value) return
        _settings.value = updated
        persist(updated)
    }

    private fun persist(settings: Settings) {
        val out = try {
            file.startWrite()
        } catch (e: Exception) {
            Log.e(TAG, "Konnte Datei nicht öffnen", e)
            return
        }
        try {
            out.write(SettingsJson.encode(settings).toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            Log.e(TAG, "Speichern fehlgeschlagen", e)
        }
    }

    private fun load(): Settings {
        if (!file.baseFile.exists()) return Settings()
        return try {
            SettingsJson.decode(String(file.readFully(), Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Laden fehlgeschlagen", e)
            Settings()
        }
    }

    companion object {
        private const val TAG = "SettingsStore"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
