package com.miraj.nexa

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private var tts: TextToSpeech? = null
    private val bg = Color.rgb(12, 17, 29)
    private val panel = Color.rgb(24, 32, 49)
    private val accent = Color.rgb(89, 166, 255)
    private val white = Color.rgb(241, 245, 249)
    private val muted = Color.rgb(164, 178, 198)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "NEXA AI\nYour personal assistant"
            textSize = 23f
            setTextColor(accent)
            setPadding(dp(4), dp(8), dp(4), dp(16))
        }
        root.addView(title)
        scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply {
            hint = "Nexa-কে কিছু জিজ্ঞেস করো..."
            setHintTextColor(muted); setTextColor(white); textSize = 15f
            maxLines = 3; setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(panel)
        }
        bar.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))
        val mic = Button(this).apply {
            text = "🎙"; setOnClickListener { voice() }
        }
        bar.addView(mic, LinearLayout.LayoutParams(dp(58), dp(52)))
        val send = Button(this).apply {
            text = "➤"; setOnClickListener { send() }
        }
        bar.addView(send, LinearLayout.LayoutParams(dp(58), dp(52)))
        root.addView(bar)
        setContentView(root)
        addMessage("Nexa", "আসসালামু আলাইকুম! আমি Nexa। লিখে বা মাইক্রোফোনে কথা বলে চেষ্টা করো। এই সংস্করণে এখনো অনলাইন AI যুক্ত নেই।", false)
    }

    private fun send() {
        val q = input.text.toString().trim()
        if (q.isEmpty()) return
        input.setText("")
        addMessage("তুমি", q, true)
        val lower = q.lowercase(Locale.ROOT)
        val answer = when {
            lower.contains("নাম") || lower.contains("name") -> "আমার নাম Nexa। আমি তোমার ব্যক্তিগত সহকারী হওয়ার জন্য তৈরি হচ্ছি।"
            lower.contains("হ্যালো") || lower.contains("hello") || lower == "hi" -> "হ্যালো! 😊 কীভাবে সাহায্য করতে পারি?"
            lower.contains("পড়") || lower.contains("study") || lower.contains("রুটিন") -> "চলো কাজটা ছোট ধাপে ভাগ করি। ২৫ মিনিট মনোযোগ দিয়ে শুরু করো।"
            else -> "তোমার প্রশ্নটি পেয়েছি: “$q”। এই প্রাথমিক সংস্করণে আসল AI সার্ভার যুক্ত নেই; তাই এটি এখনো ChatGPT-এর মতো উত্তর দিতে পারবে না।"
        }
        addMessage("Nexa", answer, false)
    }

    private fun addMessage(who: String, messageText: String, user: Boolean) {
        val bubble = TextView(this).apply {
            this.text = "$who\n$messageText"
            textSize = 15f; setTextColor(white)
            setPadding(dp(13), dp(11), dp(13), dp(11))
            setBackgroundColor(if (user) Color.rgb(31, 75, 120) else panel)
        }
        val row = LinearLayout(this).apply {
            gravity = if (user) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
            addView(bubble, LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * .82f).toInt(), -2))
        }
        messages.addView(row)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        if (!user) {
            val speak = TextView(this).apply {
                text = "▶ উত্তর শুনুন"; setTextColor(muted); setPadding(dp(5), dp(4), 0, dp(8))
                setOnClickListener { tts?.speak(messageText, TextToSpeech.QUEUE_FLUSH, null, "nexa") }
            }
            messages.addView(speak)
        }
    }

    private fun voice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nexa-কে বলো...")
        }
        try { startActivityForResult(intent, 701) }
        catch (_: ActivityNotFoundException) { Toast.makeText(this, "Speech recognition পাওয়া যাচ্ছে না", Toast.LENGTH_LONG).show() }
    }

    @Deprecated("Deprecated by Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 701 && resultCode == RESULT_OK) {
            input.setText(data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: "")
            send()
        }
    }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale("bn", "BD") }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}
