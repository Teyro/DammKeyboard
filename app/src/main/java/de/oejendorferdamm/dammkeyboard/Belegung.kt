/*
 * DammKeyboard – Copyright (C) 2026 Teyro – GNU GPL v3 oder neuer, siehe LICENSE
 */
package de.oejendorferdamm.dammkeyboard

/** Was eine Taste tut */
enum class Art { ZEICHEN, LOESCHEN, UMSCHALT, EINGABE, LEER, ZAHLEN, BUCHSTABEN, SYMBOLE, SPRACHE, EMOJI, LUECKE }

/**
 * Eine Taste: [text] wird geschrieben (bei ZEICHEN), [breite] in Tastenbreiten, [hinweis] klein oben rechts
 * (z. B. die Ziffer über „w“), [lang] = Auswahl bei langem Druck (erster Eintrag = Vorwahl).
 */
data class Taste(val text: String, val art: Art = Art.ZEICHEN, val breite: Float = 1f, val hinweis: String? = null, val lang: List<String> = emptyList(),
                 val unten: String? = null)

/** Die Seiten der Tastatur: Buchstaben, Zahlen/Zeichen, weitere Zeichen, Zahlenblock (Telefon/Zahl) und Emoji */
enum class Seite { BUCHSTABEN, ZAHLEN, SYMBOLE, ZIFFERNBLOCK, EMOJI }

object Belegung {
    private val ziffern = "1234567890"

    /** Lange drücken: Umlaute, Akzente, Sonderzeichen */
    val lang: Map<String, List<String>> = mapOf(
        "a" to listOf("ä", "à", "á", "â", "æ", "å"), "o" to listOf("ö", "ó", "ò", "ô", "ø", "œ"), "u" to listOf("ü", "ú", "ù", "û"),
        "s" to listOf("ß", "ś", "š"), "e" to listOf("é", "è", "ê", "ë", "€"), "i" to listOf("í", "ì", "î", "ï"), "c" to listOf("ç", "ć", "č"),
        "n" to listOf("ñ", "ń"), "y" to listOf("ý", "ÿ"), "z" to listOf("ž", "ź", "ż"), "l" to listOf("ł"),
        "!" to listOf("¡"), "?" to listOf("¿"), "." to listOf("…", ":", ";", "·"), "," to listOf(";", "'", "\""),
        "-" to listOf("–", "—", "_"), "€" to listOf("$", "£", "¥", "¢"), "0" to listOf("°", "ⁿ"), "\"" to listOf("„", "“", "”", "«", "»"),
        "'" to listOf("‚", "‘", "’", "‹", "›"), "%" to listOf("‰"), "+" to listOf("±"), "=" to listOf("≠", "≈"), "<" to listOf("≤", "«"), ">" to listOf("≥", "»"),
    )

    private fun buchstabe(z: String, hinweis: String? = null) = Taste(z, hinweis = hinweis, lang = listOfNotNull(hinweis) + (lang[z] ?: emptyList()))

    /** QWERTZ wie die CTOUCH-Tastatur: oben die Ziffern als Hinweis (lange drücken), rechts groß der Eingabe-Knopf */
    val buchstaben: List<List<Taste>> = listOf(
        "qwertzuiop".mapIndexed { i, c -> buchstabe(c.toString(), ziffern[i].toString()) } + Taste("⌫", Art.LOESCHEN, 1.4f),
        listOf(Taste("", Art.LUECKE, 0.5f)) + "asdfghjkl".map { buchstabe(it.toString()) } + Taste("✓", Art.EINGABE, 1.9f),
        listOf(Taste("⇧", Art.UMSCHALT, 1.4f)) + "yxcvbnm".map { buchstabe(it.toString()) } + listOf(Taste("!", lang = lang["!"]!!), Taste("?", lang = lang["?"]!!), Taste("⇧", Art.UMSCHALT, 1.4f)),
        listOf(Taste("?123", Art.ZAHLEN, 1.6f), Taste(",", lang = lang[","]!!), Taste("🌐", Art.SPRACHE), Taste(" ", Art.LEER, 5.2f), Taste(".", lang = lang["."]!!), Taste("😊", Art.EMOJI, 1.4f)),
    )

    val zahlen: List<List<Taste>> = listOf(
        ziffern.map { Taste(it.toString(), lang = lang[it.toString()] ?: emptyList()) } + Taste("⌫", Art.LOESCHEN, 1.4f),
        listOf(Taste("", Art.LUECKE, 0.5f)) + "@#€%&-+()".map { Taste(it.toString(), lang = lang[it.toString()] ?: emptyList()) } + Taste("✓", Art.EINGABE, 1.9f),
        listOf(Taste("=\\<", Art.SYMBOLE, 1.4f)) + "*\"':;!?/".map { Taste(it.toString(), lang = lang[it.toString()] ?: emptyList()) } + Taste("=\\<", Art.SYMBOLE, 1.4f),
        listOf(Taste("ABC", Art.BUCHSTABEN, 1.6f), Taste(","), Taste("🌐", Art.SPRACHE), Taste(" ", Art.LEER, 5.2f), Taste("."), Taste("😊", Art.EMOJI, 1.4f)),
    )

    val symbole: List<List<Taste>> = listOf(
        "~`|•√π÷×¶∆".map { Taste(it.toString()) } + Taste("⌫", Art.LOESCHEN, 1.4f),
        listOf(Taste("", Art.LUECKE, 0.5f)) + "£¥\$¢^°={}".map { Taste(it.toString()) } + Taste("✓", Art.EINGABE, 1.9f),
        listOf(Taste("?123", Art.ZAHLEN, 1.4f)) + "\\©®™§[]<>".map { Taste(it.toString(), lang = lang[it.toString()] ?: emptyList()) } + Taste("?123", Art.ZAHLEN, 1.4f),
        listOf(Taste("ABC", Art.BUCHSTABEN, 1.6f), Taste(","), Taste("🌐", Art.SPRACHE), Taste(" ", Art.LEER, 5.2f), Taste("."), Taste("😊", Art.EMOJI, 1.4f)),
    )

    /** Zahlenblock für Zahl- und Telefonfelder (wie auf dem Foto: Buchstaben klein daneben, „+“ unter der 0) */
    val ziffernblock: List<List<Taste>> = listOf(
        listOf(Taste("1"), Taste("2", unten = "ABC"), Taste("3", unten = "DEF")),
        listOf(Taste("4", unten = "GHI"), Taste("5", unten = "JKL"), Taste("6", unten = "MNO")),
        listOf(Taste("7", unten = "PQRS"), Taste("8", unten = "TUV"), Taste("9", unten = "WXYZ")),
        listOf(Taste("⌫", Art.LOESCHEN), Taste("0", unten = "+", lang = listOf("+", ",", ".", "*", "#")), Taste("✓", Art.EINGABE)),
    )

    /** Häufige Emoji – bewusst klein gehalten (kein Verlauf, nichts wird gespeichert) */
    val emojiListe: List<String> = ("😀 😃 😄 😁 😆 😅 😂 🙂 😉 😊 😍 🥰 😘 😎 🤓 🤔 🤗 🤩 😮 😢 😭 😡 🥳 😴 🤒 🤧 😷 🙈 " +
        "👍 👎 👏 🙌 👋 🤝 🙏 💪 ✌️ 👌 ✋ 👀 ❤️ 💙 💚 💛 🧡 💜 ⭐ 🌟 ✨ 🔥 🎉 🎈 🎂 🎁 ✅ ❌ ❗ ❓ 💡 📌 📚 ✏️ 📝 🏫 ⚽ " +
        "🌞 🌧️ ❄️ 🍎 🍌 🍕 🥨 ☕ 🐶 🐱 🐰 🦊 🐻 🐼 🐸 🦄 🚌 🚲 🏠 ⏰ 📅 🔔 💬 ➡️ ⬅️").split(" ")

    fun emoji(spalten: Int = 10): List<List<Taste>> =
        emojiListe.chunked(spalten).map { r -> r.map { Taste(it) } } +
            listOf(listOf(Taste("ABC", Art.BUCHSTABEN, 2f), Taste(" ", Art.LEER, 6f), Taste("⌫", Art.LOESCHEN, 2f)))

    fun seite(s: Seite): List<List<Taste>> = when (s) {
        Seite.BUCHSTABEN -> buchstaben; Seite.ZAHLEN -> zahlen; Seite.SYMBOLE -> symbole; Seite.ZIFFERNBLOCK -> ziffernblock; Seite.EMOJI -> emoji()
    }

    /** Groß-/Kleinschreibung einer Taste (nur Buchstaben; „ß“ bleibt) */
    fun gross(z: String) = if (z == "ß") z else z.uppercase()
}

/** Umschalten: einmal tippen = nächster Buchstabe groß, zweimal schnell = feststellen */
enum class Umschalt { AUS, EINMAL, FEST }

object UmschaltLogik {
    const val DOPPEL_MS = 400L

    fun tippen(jetzt: Umschalt, msSeitLetztem: Long): Umschalt = when (jetzt) {
        Umschalt.AUS -> Umschalt.EINMAL
        Umschalt.EINMAL -> if (msSeitLetztem < DOPPEL_MS) Umschalt.FEST else Umschalt.AUS
        Umschalt.FEST -> Umschalt.AUS
    }

    /** Nach einem geschriebenen Buchstaben: „einmal“ fällt zurück, „fest“ bleibt */
    fun nachZeichen(jetzt: Umschalt) = if (jetzt == Umschalt.EINMAL) Umschalt.AUS else jetzt
}
