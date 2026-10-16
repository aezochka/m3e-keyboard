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

    class ModeSwitch(val target: Layer, override val weight: Float = 1.4f) : Key()

    class Fill(override val weight: Float) : Key()

    object Shift : Key() { override val weight: Float get() = 1f }
    object Backspace : Key() { override val weight: Float get() = 1f }
    object Enter : Key() { override val weight: Float get() = 1.8f }
    object LayoutSwitch : Key() { override val weight: Float get() = 1.4f }
}

enum class Layer { LETTERS, SYMBOLS, SYMBOLS2 }
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
        "«" to "»",
        "“" to "”",
        "(" to "[",
        ")" to "]",
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

    // EN: strict 10-column grid, letters directly under letters, no stagger.
    // row2 gets an apostrophe, row3 a hyphen to fill the grid.
    private val enRows: List<List<Key>> = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { Key.Char(it) },
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "'").map { Key.Char(it) },
        listOf<Key>(Key.Shift) +
            listOf("z", "x", "c", "v", "b", "n", "m", "-").map { Key.Char(it) } +
            listOf(Key.Backspace),
    )

    // RU compact: ъ -> long-press ь, ё/э -> long-press е.
    // Strict 11-column grid: every key is one unit wide, letters under letters.
    // row2 ends with a hyphen to complete the grid.
    private val ruRows: List<List<Key>> = listOf(
        listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х").map { Key.Char(it) },
        listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "-").map { Key.Char(it) },
        listOf<Key>(Key.Shift) +
            listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю").map { Key.Char(it) } +
            listOf(Key.Backspace),
    )

    private val symbolPage1: List<List<Key>> = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { Key.Char(it) },
        listOf("@", "#", "€", "$", "%", "&", "-", "+", "(", ")").map { Key.Char(it) },
        listOf<Key>(Key.ModeSwitch(Layer.SYMBOLS2, 1f)) +
            listOf("*", "\"", "'", ":", ";", "!", "?", "=").map { Key.Char(it) } +
            listOf(Key.Backspace),
    )

    private val symbolPage2: List<List<Key>> = listOf(
        listOf("~", "`", "^", "|", "×", "÷", "±", "•", "…", "°").map { Key.Char(it) },
        listOf("₽", "¥", "£", "€", "¢", "№", "™", "®", "©", "“").map { Key.Char(it) },
        listOf<Key>(Key.ModeSwitch(Layer.SYMBOLS, 1f)) +
            listOf("<", ">", "[", "]", "{", "}", "_", "«").map { Key.Char(it) } +
            listOf(Key.Backspace),
    )

    fun rows(layout: String, layer: Layer): List<List<Key>> {
        val base = when (layer) {
            Layer.SYMBOLS -> symbolPage1
            Layer.SYMBOLS2 -> symbolPage2
            else -> if (layout == "ru") ruRows else enRows
        }
        return base + listOf(bottomRow(layout, layer))
    }

    private fun bottomRow(layout: String, layer: Layer): List<Key> {
        val total = if (layout == "ru" && layer == Layer.LETTERS) 11f else 10f
        val switch = when (layer) {
            Layer.LETTERS -> Key.ModeSwitch(Layer.SYMBOLS)
            Layer.SYMBOLS -> Key.ModeSwitch(Layer.LETTERS)
            Layer.SYMBOLS2 -> Key.ModeSwitch(Layer.SYMBOLS)
        }
        val spaceWeight = total - (1.4f + 1.4f + 1f + 1f + 1.8f)
        return listOf(
            Key.LayoutSwitch,
            switch,
            Key.Text(","),
            Key.Space(spaceWeight),
            Key.Text("."),
            Key.Enter,
        )
    }
}
