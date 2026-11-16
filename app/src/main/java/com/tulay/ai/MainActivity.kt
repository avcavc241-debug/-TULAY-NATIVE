package com.tulay.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val navy = Color.rgb(5, 11, 25)
    private val blue = Color.rgb(35, 145, 255)
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
                addMessage("تولاي: واجهتي جاهزة، وسنفعّل الاتصال بالذكاء الاصطناعي بالخطوة القادمة.")
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
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
