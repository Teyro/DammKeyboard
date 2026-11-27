/*
 * DammKeyboard – Copyright (C) 2026 Teyro – GNU GPL v3 oder neuer, siehe LICENSE
 */
package de.oejendorferdamm.dammkeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout

/**
 * Die Tastatur als Eingabe-Dienst. Kein Internet, kein Wörterbuch, kein Lernen: es wird nur geschrieben, was getippt wird.
 * Auf großen Bildschirmen schwebt sie (die App darunter wird nicht gestaucht, nur die Tastatur nimmt Berührungen an).
 */
class DammKeyboardDienst : InputMethodService(), TastaturAnsicht.Aktionen {

    private lateinit var wurzel: Wurzel
    private lateinit var tastatur: TastaturAnsicht
    private var info: EditorInfo? = null

    /** Wurzel: im Schwebemodus so hoch wie der Bildschirm (durchsichtig), sonst so hoch wie die Tastatur */
    private inner class Wurzel(c: Context) : FrameLayout(c) {
        override fun onMeasure(wSpec: Int, hSpec: Int) {
            val b = MeasureSpec.getSize(wSpec)
            val breite = tastaturBreite(b)
            val hoehe = tastatur.hoeheFuer(breite, Einstellungen.groesse(context))
            tastatur.measure(MeasureSpec.makeMeasureSpec(breite, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(hoehe, MeasureSpec.EXACTLY))
            val h = if (tastatur.schwebend) resources.displayMetrics.heightPixels else hoehe
            setMeasuredDimension(b, h)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val w = tastatur.measuredWidth; val h = tastatur.measuredHeight
            if (tastatur.schwebend) {
                // gemerkte Lage (Anteile), sonst unten mittig
                val x = ((width - w) * Einstellungen.lageX(context)).toInt()
                val y = ((height - h) * Einstellungen.lageY(context)).toInt()
                tastatur.layout(x, y, x + w, y + h)
            } else tastatur.layout((width - w) / 2, height - h, (width + w) / 2, height)
            tastatur.translationX = 0f; tastatur.translationY = 0f
        }
    }

    private fun tastaturBreite(verfuegbar: Int): Int =
        if (tastatur.schwebend) minOf((verfuegbar * 0.62f).toInt(), (1400 * resources.displayMetrics.density).toInt()).coerceAtLeast(verfuegbar / 2) else verfuegbar

    override fun onCreateInputView(): View {
        tastatur = TastaturAnsicht(this, this)
        wurzel = Wurzel(this)
        wurzel.addView(tastatur)
        window.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return wurzel
    }

    /** Nie Vollbild-Eingabe (sonst verdeckt sie im Querformat das ganze Board) */
    override fun onEvaluateFullscreenMode() = false

    override fun onStartInputView(ei: EditorInfo, restarting: Boolean) {
        super.onStartInputView(ei, restarting)
        info = ei
        tastatur.schwebend = Einstellungen.schwebend(this, resources.configuration.smallestScreenWidthDp)
        tastatur.vibration = Einstellungen.vibration(this)
        tastatur.seite = when (ei.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> Seite.ZIFFERNBLOCK
            else -> Seite.BUCHSTABEN
        }
        tastatur.eingabeZeichen = when (ei.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "🔍"
            EditorInfo.IME_ACTION_SEND -> "➤"
            EditorInfo.IME_ACTION_NEXT -> "→"
            EditorInfo.IME_ACTION_PREVIOUS -> "←"
            else -> if (mehrzeilig(ei)) "⏎" else "✓"
        }
        tastatur.umschalt = Umschalt.AUS
        grossAmAnfang()
        wurzel.requestLayout()
    }

    override fun onComputeInsets(aus: Insets) {
        super.onComputeInsets(aus)
        if (!::tastatur.isInitialized || !tastatur.schwebend || !tastatur.isShown) return
        // App darunter nicht stauchen; berührbar ist nur die Tastatur selbst
        aus.contentTopInsets = wurzel.height
        aus.visibleTopInsets = wurzel.height
        aus.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        aus.touchableRegion.set(tastatur.flaecheImFenster())
    }

    private fun mehrzeilig(ei: EditorInfo) = ei.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
        ei.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0

    /** Satzanfang (laut Eingabefeld): nächster Buchstabe groß */
    private fun grossAmAnfang() {
        val ei = info ?: return
        if (tastatur.seite != Seite.BUCHSTABEN || tastatur.umschalt == Umschalt.FEST) return
        val ic = currentInputConnection ?: return
        val gross = ei.inputType != InputType.TYPE_NULL && ic.getCursorCapsMode(ei.inputType) != 0
        if (gross) tastatur.umschalt = Umschalt.EINMAL else if (tastatur.umschalt == Umschalt.EINMAL) tastatur.umschalt = Umschalt.AUS
    }

    // ---------------------------------------------------------------- Aktionen der Tastatur

    override fun schreiben(text: String) {
        currentInputConnection?.commitText(text, 1)
        if (text == " " || text in listOf(".", "!", "?")) grossAmAnfang()
    }

    override fun loeschen() {
        val ic = currentInputConnection ?: return
        // ausgewählter Text: weg damit; sonst ein Zeichen (Emoji/Umlaute sauber über die Löschtaste)
        if (!ic.getSelectedText(0).isNullOrEmpty()) ic.commitText("", 1) else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        grossAmAnfang()
    }

    override fun eingabe() {
        val ei = info
        val ic = currentInputConnection ?: return
        val aktion = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val keineAktion = ei == null || (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0 || aktion == EditorInfo.IME_ACTION_NONE ||
            aktion == EditorInfo.IME_ACTION_UNSPECIFIED || mehrzeilig(ei)
        if (keineAktion) sendKeyChar('\n') else ic.performEditorAction(aktion)
        grossAmAnfang()
    }

    override fun cursor(schritte: Int) {
        val code = if (schritte < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(schritte)) { sendDownUpKeyEvents(code) }
    }

    override fun spracheWechseln() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    override fun einklappen() = requestHideSelf(0)

    override fun verschieben(dx: Float, dy: Float) {
        val maxX = (wurzel.width - tastatur.width).toFloat(); val maxY = (wurzel.height - tastatur.height).toFloat()
        tastatur.translationX = (tastatur.translationX + dx).coerceIn(-tastatur.left.toFloat(), maxX - tastatur.left)
        tastatur.translationY = (tastatur.translationY + dy).coerceIn(-tastatur.top.toFloat(), maxY - tastatur.top)
    }

    override fun verschiebenEnde() {
        val maxX = (wurzel.width - tastatur.width).coerceAtLeast(1); val maxY = (wurzel.height - tastatur.height).coerceAtLeast(1)
        Einstellungen.lageMerken(this, (tastatur.left + tastatur.translationX) / maxX, (tastatur.top + tastatur.translationY) / maxY)
        wurzel.requestLayout()
    }

    override fun seiteGewechselt() {
        if (tastatur.seite == Seite.BUCHSTABEN) grossAmAnfang()
        wurzel.requestLayout()   // Emoji-Seite hat mehr Zeilen
    }
}
