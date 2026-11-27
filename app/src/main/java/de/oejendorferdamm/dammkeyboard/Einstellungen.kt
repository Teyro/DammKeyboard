/*
 * DammKeyboard – Copyright (C) 2026 Teyro – GNU GPL v3 oder neuer, siehe LICENSE
 */
package de.oejendorferdamm.dammkeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Gespeicherte Einstellungen – nur diese wenigen Werte, keine Eingaben */
object Einstellungen {
    private fun p(c: Context) = c.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)

    fun vibration(c: Context) = p(c).getBoolean("vibration", false)
    fun setzeVibration(c: Context, an: Boolean) = p(c).edit().putBoolean("vibration", an).apply()

    /** „auto“ (schwebend ab Tablet-Größe), „an“, „aus“ */
    fun schwebModus(c: Context) = p(c).getString("schweben", "auto") ?: "auto"
    fun setzeSchwebModus(c: Context, m: String) = p(c).edit().putString("schweben", m).apply()
    fun schwebend(c: Context, kleinsteBreiteDp: Int) = when (schwebModus(c)) { "an" -> true; "aus" -> false; else -> kleinsteBreiteDp >= 600 }

    /** Tastengröße 0,8 … 1,3 */
    fun groesse(c: Context) = p(c).getFloat("groesse", 1f)
    fun setzeGroesse(c: Context, f: Float) = p(c).edit().putFloat("groesse", f.coerceIn(0.8f, 1.3f)).apply()

    /** Lage im Schwebemodus als Anteil (0 = links/oben, 1 = rechts/unten) */
    fun lageX(c: Context) = p(c).getFloat("lageX", 0.5f)
    fun lageY(c: Context) = p(c).getFloat("lageY", 0.92f)
    fun lageMerken(c: Context, x: Float, y: Float) = p(c).edit().putFloat("lageX", x.coerceIn(0f, 1f)).putFloat("lageY", y.coerceIn(0f, 1f)).apply()
}

/** Einrichtung: Tastatur einschalten und auswählen, ein paar Schalter, ein Feld zum Ausprobieren */
class EinstellungenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val spalte = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((24 * d).toInt(), (24 * d).toInt(), (24 * d).toInt(), (24 * d).toInt()) }
        fun text(t: String, groesse: Float = 16f, fett: Boolean = false) = TextView(this).apply {
            text = t; textSize = groesse; if (fett) setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, (8 * d).toInt(), 0, (4 * d).toInt())
        }
        spalte.addView(text("DammKeyboard", 26f, true))
        spalte.addView(text("Eigene, schlanke Tastatur der Schule am Öjendorfer Damm. Kein Internet, keine Wortvorschläge, nichts wird gespeichert oder gesendet. Keine Tastentöne."))

        spalte.addView(text("1. Einschalten", 18f, true))
        spalte.addView(Button(this).apply { text = "DammKeyboard in den Einstellungen aktivieren"; setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } })
        spalte.addView(text("2. Auswählen", 18f, true))
        spalte.addView(Button(this).apply { text = "Tastatur auswählen"; setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } })

        spalte.addView(text("Einstellungen", 18f, true))
        spalte.addView(Switch(this).apply {
            text = "Leichte Vibration beim Tippen"; isChecked = Einstellungen.vibration(this@EinstellungenActivity)
            setOnCheckedChangeListener { _, an -> Einstellungen.setzeVibration(this@EinstellungenActivity, an) }
        })
        spalte.addView(text("Schwebende Tastatur (mit Griffen zum Verschieben)"))
        val gruppe = RadioGroup(this)
        listOf("auto" to "Automatisch – auf großen Bildschirmen", "an" to "Immer schwebend", "aus" to "Immer unten").forEachIndexed { i, (wert, name) ->
            gruppe.addView(RadioButton(this).apply { id = 100 + i; text = name; isChecked = Einstellungen.schwebModus(this@EinstellungenActivity) == wert
                setOnClickListener { Einstellungen.setzeSchwebModus(this@EinstellungenActivity, wert) } })
        }
        spalte.addView(gruppe)
        val groesseText = text("")
        fun groesseZeigen() { groesseText.text = "Tastengröße: ${(Einstellungen.groesse(this) * 100).toInt()} %" }
        groesseZeigen(); spalte.addView(groesseText)
        spalte.addView(SeekBar(this).apply {
            max = 50; progress = ((Einstellungen.groesse(this@EinstellungenActivity) - 0.8f) * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, vomNutzer: Boolean) { if (vomNutzer) { Einstellungen.setzeGroesse(this@EinstellungenActivity, 0.8f + v / 100f); groesseZeigen() } }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        spalte.addView(text("Ausprobieren", 18f, true))
        spalte.addView(EditText(this).apply { hint = "Hier tippen …"; minLines = 2; gravity = Gravity.TOP })
        spalte.addView(EditText(this).apply { hint = "Zahlenfeld (Zahlenblock)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER })
        spalte.addView(text("Tipps: Lange drücken für Umlaute (ä, ö, ü, ß) und die Ziffern der oberen Reihe. Über die Leertaste wischen bewegt den Cursor. " +
            "🌐 wechselt die Tastatur. Im Schwebemodus an den Griffen links und rechts verschieben, ▾ klappt sie ein.", 14f))
        setContentView(ScrollView(this).apply { addView(spalte) })
    }
}
