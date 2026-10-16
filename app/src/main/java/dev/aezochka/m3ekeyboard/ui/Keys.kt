package dev.aezochka.m3ekeyboard.ui

sealed class Key {
    abstract val weight: Float

    class Char(val base: String, override val weight: Float = 1f) : Key() {
        val upper: String get() = base.uppercase()
    }

    class Text(val text: String, override val weight: Float = 1f) : Key() {
        val upper: String get() = text.uppercase()
    }

    class Space(override val weight: Float = 4f) : Key()

    class Fill(override val weight: Float) : Key()

    object Shift : Key() { override val weight: Float get() = 1f }
    object Backspace : Key() { override val weight: Float get() = 1f }
    object Enter : Key() { override val weight: Float get() = 1.6f }
    object Symbols : Key() { override val weight: Float get() = 1.5f }
    object Letters : Key() { override val weight: Float get() = 1.5f }
    object LayoutSwitch : Key() { override val weight: Float get() = 1.1f }
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
        "е" to "ёэ",
        "ь" to "ъ",
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

    // EN: 10-column grid; all letters equal width, columns align.
    private val enRows: List<List<Key>> = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { Key.Char(it) },
        listOf(Key.Fill(0.5f)) +
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { Key.Char(it) } +
            listOf(Key.Fill(0.5f)),
        listOf<Key>(Key.Shift) +
            listOf("z", "x", "c", "v", "b", "n", "m").map { Key.Char(it) } +
            listOf(Key.Fill(0.5f), Key.Fill(0.5f), Key.Backspace),
    )

    // RU compact: ъ -> long-press on ь, ё/э -> long-press on е.
    // 11/10/9 letters; every letter key is exactly the same width, row3 columns align with row1.
    private val ruRows: List<List<Key>> = listOf(
        listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х").map { Key.Char(it) },
        listOf(Key.Fill(0.5f)) +
            listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж").map { Key.Char(it) } +
            listOf(Key.Fill(0.5f)),
        listOf<Key>(Key.Shift) +
            listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю").map { Key.Char(it) } +
            listOf(Key.Backspace),
    )

    private val symbolRows: List<List<Key>> = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { Key.Char(it) },
        listOf(Key.Fill(0.5f)) +
            listOf("@", "#", "$", "%", "&", "-", "+", "(", ")").map { Key.Char(it) } +
            listOf(Key.Fill(0.5f)),
        listOf("*", "\"", "'", ":", ";", "!", "?", "=").map { Key.Char(it) } +
            listOf(Key.Fill(0.5f), Key.Fill(0.5f), Key.Backspace),
    )

    fun rows(layout: String, layer: Layer): List<List<Key>> {
        val base = when {
            layer == Layer.SYMBOLS -> symbolRows
            layout == "ru" -> ruRows
            else -> enRows
        }
        return base + listOf(bottomRow(layout, layer))
    }

    private fun bottomRow(layout: String, layer: Layer): List<Key> {
        val total = if (layout == "ru" && layer == Layer.LETTERS) 11f else 10f
        val spaceWeight = total - (1.1f + 1.5f + 1f + 1f + 1.6f)
        return listOf(
            Key.LayoutSwitch,
            if (layer == Layer.SYMBOLS) Key.Letters else Key.Symbols,
            Key.Text(","),
            Key.Space(spaceWeight),
            Key.Text("."),
            Key.Enter,
        )
    }
}
