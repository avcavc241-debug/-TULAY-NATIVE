package com.tulay.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private val navy = Color.rgb(5, 11, 25)
    private val blue = Color.rgb(35, 145, 255)
    private val serverUrl = "https://tulay-aiiaay.infrlo.com/chat"

    private lateinit var messages: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = navy
        window.navigationBarColor = navy

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(22, 28, 22, 18)
        root.setBackgroundColor(navy)
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL

        val title = TextView(this)
        title.text = "تولاي"
        title.textSize = 34f
        title.setTextColor(Color.WHITE)
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        root.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "TULAY • مساعدتك الشخصية الذكية"
        subtitle.textSize = 14f
        subtitle.setTextColor(blue)
        subtitle.gravity = Gravity.CENTER
        root.addView(subtitle)

        val status = TextView(this)
        status.text = "● تولاي جاهزة"
        status.textSize = 13f
        status.setTextColor(Color.rgb(80, 220, 160))
        status.gravity = Gravity.CENTER
        status.setPadding(0, 22, 0, 22)
        root.addView(status)

        val scroll = ScrollView(this)

        messages = LinearLayout(this)
        messages.orientation = LinearLayout.VERTICAL
        messages.setPadding(8, 12, 8, 12)

        val welcome = TextView(this)
        welcome.text = "هلا بيك 🌙\nآني تولاي، مساعدتك الشخصية.\nشلون أگدر أساعدك؟"
        welcome.textSize = 19f
        welcome.setTextColor(Color.WHITE)
        welcome.setPadding(18, 22, 18, 22)
        welcome.setBackgroundColor(Color.rgb(17, 31, 52))
        messages.addView(welcome)

        scroll.addView(messages)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.HORIZONTAL
        bottom.gravity = Gravity.CENTER_VERTICAL

        input = EditText(this)
        input.hint = "اكتب رسالة لتولاي..."
        input.setHintTextColor(Color.LTGRAY)
        input.setTextColor(Color.WHITE)
        input.textSize = 16f
        input.setSingleLine(true)
        input.setPadding(14, 10, 14, 10)
        input.setBackgroundColor(Color.rgb(22, 36, 57))

        bottom.addView(
            input,
            LinearLayout.LayoutParams(0, 55, 1f)
        )

        val send = Button(this)
        send.text = "إرسال"
        send.setTextColor(Color.WHITE)
        send.setBackgroundColor(blue)
        bottom.addView(send)

        send.setOnClickListener {
            val message = input.text.toString().trim()

            if (message.isNotEmpty()) {
                addMessage("أنت: $message")
                input.text.clear()
                send.isEnabled = false
                send.text = "..."

                scroll.post {
                    scroll.fullScroll(View.FOCUS_DOWN)
                }

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
                            response.optString(
                                "error",
                                "خطأ بالخادم: $code"
                            )
                        }

                        runOnUiThread {
                            addMessage("تولاي: $reply")
                            scroll.post {
                                scroll.fullScroll(View.FOCUS_DOWN)
                            }
                        }

                    } catch (e: Exception) {
                        runOnUiThread {
                            addMessage(
                                "تولاي: ما كدرت أتصل بالخادم. " +
                                "تأكد من الإنترنت وحاول مرة ثانية."
                            )
                            scroll.post {
                                scroll.fullScroll(View.FOCUS_DOWN)
                            }
                        }
                    } finally {
                        runOnUiThread {
                            send.isEnabled = true
                            send.text = "إرسال"
                        }
                    }
                }.start()
            }
        }

        root.addView(bottom)
        setContentView(root)
    }

    private fun addMessage(text: String) {
        val item = TextView(this)
        item.text = text
        item.textSize = 16f
        item.setTextColor(Color.WHITE)
        item.setPadding(16, 16, 16, 16)
        item.setBackgroundColor(Color.rgb(17, 31, 52))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 8, 0, 8)
        messages.addView(item, params)
    }
}
