package com.tulay.ai

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private val navy = Color.rgb(5, 10, 25)
    private val panel = Color.rgb(15, 25, 47)
    private val blue = Color.rgb(35, 160, 255)
    private val cyan = Color.rgb(70, 220, 255)
    private val serverUrl = "https://tulay-ai-production.up.railway.app/chat"
    private val memoryUrl = "https://tulay-ai-production.up.railway.app/memories"

    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var send: TextView
    private lateinit var mic: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private val requestAudioPermission = 701

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(14))
            setBackgroundColor(navy)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "T"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = shape(blue, 18)
        }

        header.addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        heading.addView(TextView(this).apply {
            text = "تولاي"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })

        heading.addView(TextView(this).apply {
            text = "T U L A Y  •  AI ASSISTANT"
            textSize = 10f
            setTextColor(cyan)
        })

        header.addView(heading)
        root.addView(header)

        val status = TextView(this).apply {
            text = "●  متصلة وجاهزة لمساعدتك"
            textSize = 12f
            setTextColor(Color.rgb(90, 220, 170))
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(8))
        }
        root.addView(status)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(18))
        }

        val orb = TextView(this).apply {
            text = "✦"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(20, 100, 190),
                    Color.rgb(20, 45, 100),
                    Color.rgb(10, 25, 60)
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), cyan)
            }
        }

        hero.addView(orb, LinearLayout.LayoutParams(dp(112), dp(112)))

        hero.addView(TextView(this).apply {
            text = "أنا تولاي"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(3))
        })

        hero.addView(TextView(this).apply {
            text = "مساعدتك الذكية، دائماً قريبة منك"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        })

        root.addView(hero)

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(8), dp(2), dp(12))
        }

        scroll.addView(messages)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        addMessage("هلا بيك 🌙\nآني تولاي، مساعدتك الشخصية الذكية.\nشلون أگدر أساعدك اليوم؟", false)

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = shape(panel, 24)
            setPadding(dp(10), dp(5), dp(8), dp(5))
        }

        input = EditText(this).apply {
            hint = "اكتب رسالة لتولاي..."
            setHintTextColor(Color.rgb(145, 160, 185))
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            background = null
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        composer.addView(
            input,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        send = TextView(this).apply {
            text = "➤"
            textSize = 23f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = shape(blue, 18)
        }

        mic = TextView(this).apply {
            text = "🎙️"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = shape(Color.rgb(20, 110, 130), 18)
        }

        composer.addView(mic, LinearLayout.LayoutParams(dp(48), dp(46)))

        composer.addView(send, LinearLayout.LayoutParams(dp(48), dp(46)))

        mic.setOnClickListener {
            startVoiceInput()
        }

        root.addView(
            composer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        )

        send.setOnClickListener {
            sendMessage()
        }

        setContentView(root)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale("ar", "IQ"))
                ttsReady = result != null &&
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

                if (!ttsReady) {
                    val fallback = textToSpeech?.setLanguage(Locale("ar"))
                    ttsReady = fallback != null &&
                        fallback != TextToSpeech.LANG_MISSING_DATA &&
                        fallback != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        }
    }


    private fun startVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestAudioPermission
            )
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            addMessage("خدمة التعرف على الصوت غير متوفرة بهذا الهاتف.", false)
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    runOnUiThread { mic.text = "🔴" }
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    runOnUiThread { mic.text = "🎙️" }
                }

                override fun onError(error: Int) {
                    runOnUiThread {
                        mic.text = "🎙️"
                        addMessage("ما سمعت الكلام بوضوح. حاول مرة ثانية.", false)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val spokenText = words?.firstOrNull().orEmpty()

                    runOnUiThread {
                        mic.text = "🎙️"
                        if (spokenText.isNotBlank()) {
                            input.setText(spokenText)
                            sendMessage()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-IQ")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            mic.text = "⏳"
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            mic.text = "🎙️"
            addMessage("تعذر تشغيل المايك: ${e.message}", false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestAudioPermission) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput()
            } else {
                addMessage("لازم تسمح لتولاي باستخدام المايك حتى تسمعك.", false)
            }
        }
    }

    private fun speakReply(text: String) {
        if (ttsReady && text.isNotBlank()) {
            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "tulay_reply"
            )
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.START else Gravity.END
            setPadding(dp(2), dp(5), dp(2), dp(5))
        }

        val bubble = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = shape(
                if (isUser) Color.rgb(20, 90, 155) else panel,
                20
            )
        }

        row.addView(
            bubble,
            LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.78f).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        messages.addView(row)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun handlePhoneCommand(message: String): Boolean {
        val text = message.lowercase(Locale.ROOT)
        val hasOpenCommand = listOf("افتح", "افتحي", "شغل", "شغّل", "وديني", "روح الى", "روح لـ").any { text.contains(it) }
        if (!hasOpenCommand) return false

        val target = when {
            text.contains("واتساب") || text.contains("واتس اب") -> "whatsapp"
            text.contains("يوتيوب") -> "youtube"
            text.contains("تيليجرام") || text.contains("تلغرام") || text.contains("تليجرام") -> "telegram"
            text.contains("الكاميرا") || text.contains("كاميرا") -> "camera"
            text.contains("الإعدادات") || text.contains("الاعدادات") || text.contains("إعدادات") -> "settings"
            else -> return false
        }

        val appName = when (target) {
            "whatsapp" -> "واتساب"
            "youtube" -> "يوتيوب"
            "telegram" -> "تيليجرام"
            "camera" -> "الكاميرا"
            else -> "الإعدادات"
        }

        try {
            val launchIntent = when (target) {
                "whatsapp" -> packageManager.getLaunchIntentForPackage("com.whatsapp")
                "youtube" -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                "telegram" -> packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                "camera" -> Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                else -> Intent(android.provider.Settings.ACTION_SETTINGS)
            }

            if (launchIntent == null) {
                val reply = "تطبيق $appName مو مثبت أو ما كدرت أوصله على الموبايل."
                addMessage(reply, false)
                speakReply(reply)
                return true
            }

            startActivity(launchIntent)
            val reply = "حاضر، دا أفتح $appName."
            addMessage(reply, false)
            speakReply(reply)
        } catch (e: Exception) {
            val reply = "ما كدرت أفتح $appName. حاول مرة ثانية."
            addMessage(reply, false)
            speakReply(reply)
        }
        return true
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        addMessage(message, true)
        input.text.clear()

        if (handlePhoneCommand(message)) return
        send.isEnabled = false
        send.text = "..."

        Thread {
            try {
                val connection = URL(serverUrl)
                    .openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 70000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                val requestBody = JSONObject()
                    .put("message", message)
                    .toString()
                    .toByteArray(Charsets.UTF_8)

                connection.outputStream.use {
                    it.write(requestBody)
                }

                val code = connection.responseCode

                val responseText = (
                    if (code in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                )?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                connection.disconnect()

                val response = JSONObject(responseText)

                val reply = if (code in 200..299) {
                    response.optString(
                        "reply",
                        "تولاي استلمت الرد لكن ما كدرت تقراه."
                    )
                } else {
                    response.optString("error", "خطأ بالخادم: $code")
                }

                runOnUiThread {
                    addMessage(reply, false)
                    speakReply(reply)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    addMessage(
                        "ما كدرت أتصل بالخادم. تأكد من الإنترنت وحاول مرة ثانية.",
                        false
                    )
                }
            } finally {
                runOnUiThread {
                    send.isEnabled = true
                    send.text = "➤"
                }
            }
        }.start()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        super.onDestroy()
    }
}
