/*
 * DammKeyboard – Copyright (C) 2026 Teyro – GNU GPL v3 oder neuer, siehe LICENSE
 */
package de.oejendorferdamm.dammkeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Die ganze Tastatur in einer einzigen gezeichneten Ansicht – keine Unteransichten, keine Bibliotheken: schnell und sparsam.
 * Optik wie die CTOUCH-Tastatur: hellgraue Fläche, helle Tasten, dunklere Sondertasten, blauer Haken. Schwebend mit Griffen
 * links und rechts zum Verschieben und einem Pfeil oben rechts zum Einklappen.
 */
@SuppressLint("ViewConstructor")
class TastaturAnsicht(context: Context, private val aktion: Aktionen) : View(context) {

    interface Aktionen {
        fun schreiben(text: String)
        fun loeschen()
        fun eingabe()
        fun cursor(schritte: Int)
        fun spracheWechseln()
        fun einklappen()
        fun verschieben(dx: Float, dy: Float)
        fun verschiebenEnde()
        fun seiteGewechselt()
    }

    private val d = resources.displayMetrics.density
    var seite = Seite.BUCHSTABEN; set(v) { field = v; zeilen = Belegung.seite(v); legen(); invalidate() }
    var umschalt = Umschalt.AUS; set(v) { field = v; invalidate() }
    var schwebend = false; set(v) { field = v; legen(); invalidate() }
    var vibration = false
    /** Beschriftung des Eingabe-Knopfs (✓, ⏎, Suche …) – vom Eingabefeld abhängig */
    var eingabeZeichen = "✓"; set(v) { field = v; invalidate() }

    private var zeilen = Belegung.seite(seite)
    private class Platz(val taste: Taste, val r: RectF)
    private val plaetze = ArrayList<Platz>()

    // ---------------------------------------------------------------- Farben (wie auf den Fotos)
    private val flaeche = 0xFFE8EAED.toInt()
    private val tasteHell = 0xFFF8F9FA.toInt()
    private val tasteDunkel = 0xFFD6D9DD.toInt()
    private val tasteGedrueckt = 0xFFC4C8CD.toInt()
    private val blau = 0xFF4479E8.toInt()
    private val schrift = 0xFF3C4043.toInt()
    private val schrift2 = 0xFF5F6368.toInt()

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = schrift; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
    private val klein = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT; color = schrift2; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val linie = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB0B4B9.toInt(); strokeWidth = 2f * resources.displayMetrics.density; strokeCap = Paint.Cap.ROUND }

    private val griff = 30f * d
    private val rand = 8f * d
    private val luecke = 5f * d
    private val ecke = 6f * d
    private val oben get() = if (schwebend) 30f * d else 6f * d   // im Schwebemodus Platz für den Einklapp-Pfeil
    private val pfeil = RectF()

    // ---------------------------------------------------------------- Aufteilen

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { legen() }

    private fun legen() {
        plaetze.clear()
        if (width == 0 || height == 0) return
        val links = if (schwebend) griff else rand
        val rechts = width - if (schwebend) griff else rand
        val zeilenH = (height - oben - rand) / zeilen.size
        zeilen.forEachIndexed { zi, z ->
            val summe = z.sumOf { it.breite.toDouble() }.toFloat()
            // Zahlenblock: schmal und mittig wie auf dem Foto
            val breite = if (seite == Seite.ZIFFERNBLOCK) minOf(rechts - links, (rechts - links) * 0.55f) else rechts - links
            var x = links + (rechts - links - breite) / 2
            val einheit = breite / summe
            val y = oben + zi * zeilenH
            z.forEach { k ->
                val w = k.breite * einheit
                if (k.art != Art.LUECKE) plaetze.add(Platz(k, RectF(x + luecke / 2, y + luecke / 2, x + w - luecke / 2, y + zeilenH - luecke / 2)))
                x += w
            }
        }
        pfeil.set(width - griff - 44f * d, 0f, width - griff, oben)
    }

    /** Höhe für eine Breite: Tasten etwa so hoch wie breit × 1,1 (Zahlenblock höher) */
    fun hoeheFuer(breite: Int, faktor: Float): Int {
        val z = zeilen.size.coerceAtLeast(4)
        val taste = (breite - 2 * (if (schwebend) griff else rand)) / 11.5f
        val zeile = (taste * 1.05f).coerceIn(40f * d, 74f * d) * faktor
        return (oben + rand + zeile * z).toInt()
    }

    // ---------------------------------------------------------------- Zeichnen

    override fun onDraw(c: Canvas) {
        p.color = flaeche
        c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), if (schwebend) 6f * d else 0f, if (schwebend) 6f * d else 0f, p)
        if (schwebend) {
            griffZeichnen(c, griff / 2)
            griffZeichnen(c, width - griff / 2)
            // Pfeil zum Einklappen
            val cx = pfeil.centerX(); val cy = pfeil.centerY()
            linie.color = schrift2
            c.drawLine(cx - 9 * d, cy - 4 * d, cx, cy + 5 * d, linie); c.drawLine(cx, cy + 5 * d, cx + 9 * d, cy - 4 * d, linie)
            linie.color = 0xFFB0B4B9.toInt()
        }
        val zeilenH = plaetze.firstOrNull()?.r?.height() ?: 0f
        for (pl in plaetze) tasteZeichnen(c, pl, zeilenH)
        auswahl?.let { auswahlZeichnen(c, it) }
    }

    private fun griffZeichnen(c: Canvas, x: Float) {
        val mitte = height / 2f
        for (i in -1..1) c.drawLine(x + i * 5 * d, mitte - 16 * d, x + i * 5 * d, mitte + 16 * d, linie)
    }

    private fun tasteZeichnen(c: Canvas, pl: Platz, zeilenH: Float) {
        val k = pl.taste
        val sonder = k.art in setOf(Art.LOESCHEN, Art.UMSCHALT, Art.ZAHLEN, Art.BUCHSTABEN, Art.SYMBOLE) || (k.art == Art.UMSCHALT)
        val gedrueckt = pl === gedrueckt
        p.color = when {
            k.art == Art.EINGABE -> if (gedrueckt) 0xFF3563C9.toInt() else blau
            gedrueckt -> tasteGedrueckt
            k.art == Art.UMSCHALT && umschalt != Umschalt.AUS -> 0xFFC9D7F5.toInt()
            sonder -> tasteDunkel
            else -> tasteHell
        }
        c.drawRoundRect(pl.r, ecke, ecke, p)
        val beschriftung = when {
            k.art == Art.EINGABE -> eingabeZeichen
            k.art == Art.ZEICHEN && umschalt != Umschalt.AUS && seite == Seite.BUCHSTABEN -> Belegung.gross(k.text)
            k.art == Art.UMSCHALT && umschalt == Umschalt.FEST -> "⇪"
            else -> k.text
        }
        val gross = when (k.art) {
            Art.ZEICHEN -> if (seite == Seite.EMOJI) 0.5f else 0.46f
            Art.ZAHLEN, Art.BUCHSTABEN, Art.SYMBOLE -> 0.3f
            Art.LEER -> 0f
            else -> 0.42f
        }
        t.textSize = zeilenH * gross
        t.color = if (k.art == Art.EINGABE) 0xFFFFFFFF.toInt() else schrift
        t.typeface = if (k.art == Art.ZAHLEN || k.art == Art.BUCHSTABEN || k.art == Art.SYMBOLE) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        if (k.unten != null && seite == Seite.ZIFFERNBLOCK) {
            // Zahlenblock: Ziffer groß, Buchstaben klein daneben
            val bx = pl.r.centerX() - zeilenH * 0.12f
            c.drawText(beschriftung, bx, pl.r.centerY() + t.textSize * 0.36f, t)
            klein.textSize = zeilenH * 0.2f; klein.textAlign = Paint.Align.LEFT
            c.drawText(k.unten, bx + t.textSize * 0.42f, pl.r.centerY() + t.textSize * 0.36f, klein)
            klein.textAlign = Paint.Align.RIGHT
        } else if (beschriftung.isNotEmpty()) {
            c.drawText(beschriftung, pl.r.centerX(), pl.r.centerY() + t.textSize * 0.36f, t)
        }
        k.hinweis?.let { klein.textSize = zeilenH * 0.2f; c.drawText(it, pl.r.right - 6 * d, pl.r.top + klein.textSize + 3 * d, klein) }
    }

    // ---------------------------------------------------------------- Auswahl bei langem Druck

    private class Auswahl(val von: Platz, val zeichen: List<String>, val r: RectF, var wahl: Int = 0)
    private var auswahl: Auswahl? = null

    private fun auswahlZeigen(pl: Platz) {
        val z = pl.taste.lang.map { if (umschalt != Umschalt.AUS && seite == Seite.BUCHSTABEN) Belegung.gross(it) else it }
        if (z.isEmpty()) return
        val w = pl.r.width() * 0.95f
        val gesamt = w * z.size
        var x = (pl.r.centerX() - w / 2).coerceAtMost(width - (if (schwebend) griff else rand) - gesamt).coerceAtLeast(if (schwebend) griff else rand)
        val h = pl.r.height()
        val y = (pl.r.top - h - 4 * d).coerceAtLeast(0f)
        auswahl = Auswahl(pl, z, RectF(x, y, x + gesamt, y + h))
        if (vibration) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        invalidate()
    }

    private fun auswahlZeichnen(c: Canvas, a: Auswahl) {
        p.color = 0xFFFFFFFF.toInt(); p.setShadowLayer(8 * d, 0f, 2 * d, 0x55000000)
        c.drawRoundRect(a.r, ecke * 1.5f, ecke * 1.5f, p)
        p.clearShadowLayer()
        val w = a.r.width() / a.zeichen.size
        a.zeichen.forEachIndexed { i, z ->
            val r = RectF(a.r.left + i * w, a.r.top, a.r.left + (i + 1) * w, a.r.bottom)
            if (i == a.wahl) { p.color = blau; c.drawRoundRect(r.apply { inset(3 * d, 3 * d) }, ecke, ecke, p) }
            t.textSize = a.r.height() * 0.45f; t.color = if (i == a.wahl) 0xFFFFFFFF.toInt() else schrift; t.typeface = Typeface.DEFAULT
            c.drawText(z, r.centerX(), r.centerY() + t.textSize * 0.36f, t)
        }
    }

    // ---------------------------------------------------------------- Berührung

    private val handler = Handler(Looper.getMainLooper())
    private var gedrueckt: Platz? = null
    private var ziehen = false            // Griff gefasst
    private var letztX = 0f; private var letztY = 0f
    private var startX = 0f
    private var leerSchritte = 0          // Wischen auf der Leertaste = Cursor bewegen
    private var langGelaufen = false

    private val langDruck = Runnable { gedrueckt?.let { pl -> langGelaufen = true; if (pl.taste.art == Art.ZEICHEN) auswahlZeigen(pl) } }
    private val loeschWiederholung = object : Runnable {
        override fun run() { aktion.loeschen(); langGelaufen = true; handler.postDelayed(this, 55) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                letztX = e.rawX; letztY = e.rawY; startX = e.x; leerSchritte = 0; langGelaufen = false
                if (schwebend && pfeil.contains(e.x, e.y)) { aktion.einklappen(); return true }
                if (schwebend && (e.x < griff || e.x > width - griff)) { ziehen = true; return true }
                val pl = plaetze.firstOrNull { it.r.contains(e.x, e.y) } ?: plaetze.minByOrNull { abstand(it.r, e.x, e.y) } ?: return true
                gedrueckt = pl
                if (vibration) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (pl.taste.art) {
                    Art.LOESCHEN -> { aktion.loeschen(); handler.postDelayed(loeschWiederholung, 420) }
                    Art.ZEICHEN -> if (pl.taste.lang.isNotEmpty()) handler.postDelayed(langDruck, 380)
                    else -> {}
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (ziehen) { aktion.verschieben(e.rawX - letztX, e.rawY - letztY); letztX = e.rawX; letztY = e.rawY; return true }
                val pl = gedrueckt ?: return true
                val a = auswahl
                if (a != null) {
                    val w = a.r.width() / a.zeichen.size
                    val neu = ((e.x - a.r.left) / w).toInt().coerceIn(0, a.zeichen.size - 1)
                    if (neu != a.wahl) { a.wahl = neu; invalidate() }
                } else if (pl.taste.art == Art.LEER) {
                    // je 14 dp Wischen ein Zeichen weiter
                    val schritte = ((e.x - startX) / (14 * d)).toInt()
                    if (schritte != leerSchritte) { aktion.cursor(schritte - leerSchritte); leerSchritte = schritte }
                } else if (!pl.r.contains(e.x, e.y) && pl.taste.art == Art.ZEICHEN) {
                    // auf eine andere Taste gerutscht: die zählt (wie bei der Stock-Tastatur)
                    plaetze.firstOrNull { it.r.contains(e.x, e.y) && it.taste.art == Art.ZEICHEN }?.let {
                        handler.removeCallbacks(langDruck); gedrueckt = it
                        if (it.taste.lang.isNotEmpty()) handler.postDelayed(langDruck, 380)
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(langDruck); handler.removeCallbacks(loeschWiederholung)
                if (ziehen) { ziehen = false; aktion.verschiebenEnde(); return true }
                val pl = gedrueckt
                val a = auswahl
                if (a != null) aktion.schreiben(a.zeichen[a.wahl])
                else if (pl != null && !(pl.taste.art == Art.LEER && leerSchritte != 0)) tippen(pl.taste)
                gedrueckt = null; auswahl = null
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(langDruck); handler.removeCallbacks(loeschWiederholung)
                gedrueckt = null; auswahl = null; ziehen = false; invalidate()
            }
        }
        return true
    }

    private fun abstand(r: RectF, x: Float, y: Float) = abs(r.centerX() - x) + abs(r.centerY() - y)

    private var letzteUmschaltung = 0L

    private fun tippen(k: Taste) {
        when (k.art) {
            Art.ZEICHEN -> {
                val z = if (umschalt != Umschalt.AUS && seite == Seite.BUCHSTABEN) Belegung.gross(k.text) else k.text
                aktion.schreiben(z)
                if (seite == Seite.BUCHSTABEN) umschalt = UmschaltLogik.nachZeichen(umschalt)
            }
            Art.LEER -> aktion.schreiben(" ")
            Art.LOESCHEN -> {}   // schon beim Drücken
            Art.EINGABE -> aktion.eingabe()
            Art.UMSCHALT -> {
                val jetzt = System.currentTimeMillis()
                umschalt = UmschaltLogik.tippen(umschalt, jetzt - letzteUmschaltung)
                letzteUmschaltung = jetzt
            }
            Art.ZAHLEN -> { seite = Seite.ZAHLEN; aktion.seiteGewechselt() }
            Art.SYMBOLE -> { seite = Seite.SYMBOLE; aktion.seiteGewechselt() }
            Art.BUCHSTABEN -> { seite = Seite.BUCHSTABEN; aktion.seiteGewechselt() }
            Art.EMOJI -> { seite = Seite.EMOJI; aktion.seiteGewechselt() }
            Art.SPRACHE -> aktion.spracheWechseln()
            Art.LUECKE -> {}
        }
    }

    /** Bereich der Tastatur in Fensterkoordinaten (für die berührbare Fläche im Schwebemodus) */
    fun flaecheImFenster(aus: IntArray = IntArray(2)): android.graphics.Rect {
        getLocationInWindow(aus)
        return android.graphics.Rect(aus[0], aus[1], aus[0] + width, aus[1] + height)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}
