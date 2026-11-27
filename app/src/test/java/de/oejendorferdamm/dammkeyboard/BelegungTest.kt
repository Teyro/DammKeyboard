package de.oejendorferdamm.dammkeyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BelegungTest {
    @Test fun qwertzWieAufDemFoto() {
        val b = Belegung.buchstaben
        assertEquals("qwertzuiop", b[0].filter { it.art == Art.ZEICHEN }.joinToString("") { it.text })
        assertEquals("1", b[0][0].hinweis)
        assertEquals(Art.LOESCHEN, b[0].last().art)
        assertEquals(Art.EINGABE, b[1].last().art)
        assertEquals("yxcvbnm!?", b[2].filter { it.art == Art.ZEICHEN }.joinToString("") { it.text })
        assertTrue(b[3].any { it.art == Art.LEER } && b[3].any { it.art == Art.EMOJI } && b[3].any { it.art == Art.SPRACHE })
    }

    @Test fun langDrueckenUmlauteUndZiffern() {
        val a = Belegung.buchstaben[1].first { it.text == "a" }
        assertEquals("ä", a.lang.first())
        assertEquals("1", Belegung.buchstaben[0].first { it.text == "q" }.lang.first())   // obere Reihe: zuerst die Ziffer
        assertEquals("ß", Belegung.buchstaben[1].first { it.text == "s" }.lang.first())
        assertEquals("SS".lowercase().length, 2); assertEquals("ß", Belegung.gross("ß"))
        assertEquals("Ä", Belegung.gross("ä"))
    }

    @Test fun zahlenblockWieAufDemFoto() {
        val z = Belegung.ziffernblock
        assertEquals(listOf("1", "2", "3"), z[0].map { it.text })
        assertEquals("ABC", z[0][1].unten)
        assertEquals("+", z[3][1].unten)
        assertEquals(listOf(Art.LOESCHEN, Art.ZEICHEN, Art.EINGABE), z[3].map { it.art })
    }

    @Test fun umschalten() {
        assertEquals(Umschalt.EINMAL, UmschaltLogik.tippen(Umschalt.AUS, 5000))
        assertEquals(Umschalt.FEST, UmschaltLogik.tippen(Umschalt.EINMAL, 200))      // zweimal schnell = feststellen
        assertEquals(Umschalt.AUS, UmschaltLogik.tippen(Umschalt.EINMAL, 900))
        assertEquals(Umschalt.AUS, UmschaltLogik.nachZeichen(Umschalt.EINMAL))
        assertEquals(Umschalt.FEST, UmschaltLogik.nachZeichen(Umschalt.FEST))
    }

    @Test fun alleSeitenGefuellt() {
        Seite.values().forEach { s -> assertTrue(s.name, Belegung.seite(s).isNotEmpty() && Belegung.seite(s).all { it.isNotEmpty() }) }
        assertTrue(Belegung.emojiListe.size >= 80)
    }
}
