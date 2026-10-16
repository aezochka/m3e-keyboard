package dev.aezochka.m3ekeyboard.ui

sealed class Key {
    abstract val weight: Float

    class Char(val base: String, override val weight: Float = 1f) : Key() {
        val upper: String get() = base.uppercase()
    }

    class Text(val text: String, override val weight: Float = 1f) : Key() {
        val upper: String get() = text.uppercase()
    }

    object Shift : Key() { override val weight: Float get() = 1.6f }
    object Backspace : Key() { override val weight: Float get() = 1.6f }
    object Space : Key() { override val weight: Float get() = 4f }
    object Enter : Key() { override val weight: Float get() = 1.6f }
    object Symbols : Key() { override val weight: Float get() = 1.6f }
    object Letters : Key() { override val weight: Float get() = 1.6f }
    object LayoutSwitch : Key() { override val weight: Float get() = 1.2f }
}

enum class Layer { LETTERS, SYMBOLS }
enum class ShiftState { OFF, ON, LOCKED }

object Diacritics {
    private val en = mapOf(
        "a" to "áàâäãå",
        "e" to "éèêë",
        "i" to "íìîï",
        "o" to "óòôöõ",
        "u" to "úùûü",
        "c" to "ç",
        "n" to "ñ",
        "s" to "ß",
        "y" to "ýÿ",
    )
    private val ru = mapOf(
        "е" to "ё",
    )
    private val punct = mapOf(
        "," to "…;",
        "." to "…",
        "!" to "¡",
        "?" to "¿",
        "-" to "—–",
        "'" to "\"«»",
    )

    fun variants(layout: String, layer: Layer, base: String): List<String>? {
        if (layer == Layer.LETTERS) {
            (if (layout == "ru") ru else en)[base]?.let { raw ->
                return raw.map { it.toString() }
            }
        }
        return punct[base]?.let { raw -> raw.map { it.toString() } }
    }
}

object Layouts {

    private val enRows: List<List<Key.Char>> = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    ).map { row -> row.map { Key.Char(it) } }

    private val ruRows: List<List<Key.Char>> = listOf(
        listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"),
        listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
        listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю"),
    ).map { row -> row.map { Key.Char(it) } }

    private val symbolRows: List<List<Key>> = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        listOf("*", "\"", "'", ":", ";", "!", "?", "=", "<", ">"),
    ).map { row -> row.map { Key.Char(it) } }

    fun rows(layout: String, layer: Layer): List<List<Key>> {
        val letterRows = if (layout == "ru") ruRows else enRows
        val top = if (layer == Layer.SYMBOLS) symbolRows.subList(0, 2) else letterRows.subList(0, 2)
        val third: List<Key> = if (layer == Layer.SYMBOLS) {
            symbolRows[2] + listOf(Key.Backspace)
        } else {
            listOf<Key>(Key.Shift) + letterRows[2] + listOf<Key>(Key.Backspace)
        }
        return top + listOf(third, bottomRow(layer))
    }

    private fun bottomRow(layer: Layer): List<Key> = listOf(
        Key.LayoutSwitch,
        if (layer == Layer.SYMBOLS) Key.Letters else Key.Symbols,
        Key.Text(","),
        Key.Space,
        Key.Text("."),
        Key.Enter,
    )
}
