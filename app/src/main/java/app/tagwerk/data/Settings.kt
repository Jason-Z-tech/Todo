package app.tagwerk.data

import org.json.JSONArray
import org.json.JSONObject

/** Persönliche Einstellungen: Name in der Begrüßung, Farbthema und die eigenen Listen. */
data class Settings(
    val name: String = DEFAULT_NAME,
    /** Hauptfarbe des Themas als ARGB. */
    val accent: Int = DEFAULT_ACCENT,
    val lists: List<TaskList> = DefaultLists.all,
) {
    /** Liste zu einer ID – unbekannte IDs landen in der ersten Liste. */
    fun list(id: String): TaskList = lists.firstOrNull { it.id == id } ?: lists.first()

    companion object {
        const val DEFAULT_NAME = "Queen"

        /** Pfirsich – das ursprüngliche Tagwerk-Thema. */
        const val DEFAULT_ACCENT = 0xFFF4B6A0.toInt()
    }
}

/** Fertige Farbthemen. Mit „Eigene Farbe“ lässt sich jede andere Farbe wählen. */
enum class ThemePreset(val label: String, val accent: Int) {
    PEACH("Pfirsich", Settings.DEFAULT_ACCENT),
    ROSE("Rosé", 0xFFEFA9BC.toInt()),
    LILAC("Flieder", 0xFFC7B3E8.toInt()),
    SKY("Himmel", 0xFFA4C8EC.toInt()),
    MINT("Minze", 0xFF9BD3BE.toInt()),
    SAGE("Salbei", 0xFFB3CF9F.toInt()),
    HONEY("Honig", 0xFFF1CC7E.toInt()),
}

/** Pastelltöne zur Auswahl für Listen, nach Farbton sortiert. */
val ListColors: List<Int> = listOf(
    0xFFFBD3C2, 0xFFFCE1B6, 0xFFF8E8B0, 0xFFE2EDB9,
    0xFFD4E6CC, 0xFFC8E8DC, 0xFFC6E5EC, 0xFFCFE0F0,
    0xFFD3D6F2, 0xFFE0D6F0, 0xFFEED3EC, 0xFFF6D0D8,
).map { it.toInt() }

internal object SettingsJson {

    fun encode(s: Settings): String = JSONObject().apply {
        put("version", 1)
        put("name", s.name)
        put("accent", colorToHex(s.accent))
        put("lists", JSONArray().apply {
            s.lists.forEach { l ->
                put(JSONObject().put("id", l.id).put("name", l.name).put("color", colorToHex(l.color)))
            }
        })
    }.toString()

    fun decode(text: String): Settings {
        val o = JSONObject(text)
        val array = o.optJSONArray("lists")
        val lists = if (array == null) emptyList() else (0 until array.length()).mapNotNull { i ->
            runCatching {
                val l = array.getJSONObject(i)
                TaskList(
                    id = l.getString("id"),
                    name = l.optString("name"),
                    color = parseColor(l.optString("color")) ?: ListColors.first(),
                )
            }.getOrNull()
        }.distinctBy { it.id }
        return Settings(
            name = if (o.has("name")) o.optString("name") else Settings.DEFAULT_NAME,
            accent = parseColor(o.optString("accent")) ?: Settings.DEFAULT_ACCENT,
            lists = lists.ifEmpty { DefaultLists.all },
        )
    }
}

/** „#RRGGBB“ für eine ARGB-Farbe. */
fun colorToHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

/** Liest „#RRGGBB“, „RRGGBB“ oder die Kurzform „#RGB“ – sonst null. */
fun parseColor(text: String): Int? {
    val hex = text.trim().removePrefix("#")
    val full = when {
        hex.length == 3 && hex.all { it.isHexDigit() } -> hex.map { "$it$it" }.joinToString("")
        hex.length == 6 && hex.all { it.isHexDigit() } -> hex
        else -> return null
    }
    return (0xFF000000.toInt()) or full.toInt(16)
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
