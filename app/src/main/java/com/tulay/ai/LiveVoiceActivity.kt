package com.tulay.ai

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.media.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.*
import android.graphics.drawable.GradientDrawable
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class LiveVoiceActivity : Activity() {

    private val navy = Color.rgb(5, 10, 25)
    private val blue = Color.rgb(35, 120, 230)
    private val baseUrl = "https://tulay-ai-production.up.railway.app"

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile private var connected = false
    @Volatile private var recording = false

    private var socket: WebSocket? = null
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var audioThread: Thread? = null
    private var playbackThread: Thread? = null

    private val audioQueue = LinkedBlockingQueue<ByteArray>()

    private lateinit var status: TextView
    private lateinit var button: Button

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = navy
        window.navigationBarColor = navy

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(navy)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val avatar = ImageView(this@LiveVoiceActivity).apply {
            setImageResource(R.drawable.tulay_avatar)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        root.addView(
            avatar,
            LinearLayout.LayoutParams(dp(260), dp(260)).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(24)
            }
        )

        root.addView(TextView(this).apply {
            text = "TULAY LIVE"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "تولاي تسمعك وتحچي وياك مباشرة"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(35))
        })

        root.addView(TextView(this).apply {
            text = "✦"
            textSize = 85f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(30, 90, 190),
                    Color.rgb(15, 25, 75)
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(3), Color.CYAN)
            }
        }, LinearLayout.LayoutParams(dp(180), dp(180)))

        status = TextView(this).apply {
            text = "جاهزة للاتصال"
            textSize = 17f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(0, dp(35), 0, dp(20))
        }
        root.addView(status)

        button = Button(this).apply {
            text = "ابدأ المحادثة"
            textSize = 17f
            setTextColor(Color.WHITE)
            setBackgroundColor(blue)
            setOnClickListener {
                if (connected || recording) stopLive()
                else startLive()
            }
        }

        root.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        setContentView(root)
    }

    private fun updateStatus(message: String) {
        runOnUiThread { status.text = message }
    }

    private fun startLive() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO), 701
            )
            return
        }

        button.isEnabled = false
        updateStatus("دا أتصل بتولاي...")

        Thread {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/live-token")
                    .post(
                        "{}".toRequestBody(
                            "application/json; charset=utf-8".toMediaType()
                        )
                    )
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    throw Exception("خطأ الخادم: ${response.code}")
                }

                val data = JSONObject(body)
                val token = data.getString("token")
                val model = data.optString(
                    "model", "models/gemini-3.8-live"
                )

                connectGemini(token, model)

            } catch (e: Exception) {
                updateStatus("تعذر الاتصال: ${e.message}")
                runOnUiThread { button.isEnabled = true }
            }
        }.start()
    }

    private fun connectGemini(token: String, model: String) {

        val url = (
            "wss://generativelanguage.googleapis.com/" +
            "ws/google.ai.generativelanguage.v1alpha." +
            "GenerativeService.BidiGenerateContentConstrained"
        ).toHttpUrl().newBuilder()
            .addQueryParameter("access_token", token)
            .build()

        val request = Request.Builder().url(url).build()

        socket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                val setup = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", model)

                        put("generationConfig", JSONObject().apply {
                            put("responseModalities",
                                org.json.JSONArray().put("AUDIO"))

                            put("speechConfig", JSONObject().apply {
                                put("voiceConfig", JSONObject().apply {
                                    put("prebuiltVoiceConfig",
                                        JSONObject().put(
                                            "voiceName", "Aoede"
                                        )
                                    )
                                })
                            })
                        })

                        put("systemInstruction", JSONObject().apply {
                            put("parts", org.json.JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    "اسمك تولاي. تحدث باللهجة العراقية " +
                                    "بطريقة طبيعية وودودة. أنت مساعد " +
                                    "شخصي ذكي. أجب بصوت عربي واضح."
                                )
                            ))
                        })

                        put("inputAudioTranscription", JSONObject())
                        put("outputAudioTranscription", JSONObject())
                    })
                }

                webSocket.send(setup.toString())
                updateStatus("اتصلنا، ننتظر تجهيز الصوت...")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)

                    if (json.has("setupComplete")) {
                        connected = true
                        runOnUiThread {
                            button.isEnabled = true
                            button.text = "إنهاء المحادثة"
                        }
                        startAudio()
                        updateStatus("تولاي تسمعك الآن 🎙️")
                    }

                    val content = json.optJSONObject("serverContent")
                    if (content != null) {

                        if (content.optBoolean("interrupted")) {
                            audioQueue.clear()
                            player?.pause()
                            player?.flush()
                            player?.play()
                            updateStatus("تولاي تسمعك...")
                        }

                        val parts = content.optJSONObject("modelTurn")
                            ?.optJSONArray("parts")

                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val inline = parts.optJSONObject(i)
                                    ?.optJSONObject("inlineData")

                                val audio = inline?.optString("data")

                                if (!audio.isNullOrBlank()) {
                                    val bytes = Base64.decode(
                                        audio, Base64.DEFAULT
                                    )
                                    audioQueue.offer(bytes)
                                    updateStatus("تولاي تحچي وياك 🔊")
                                }
                            }
                        }

                        if (content.optBoolean("turnComplete")) {
                            updateStatus("تولاي تسمعك الآن 🎙️")
                        }
                    }

                } catch (e: Exception) {
                    updateStatus("مشكلة بمعالجة الصوت")
                }
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                connected = false
                recording = false
                updateStatus("انقطع الاتصال: ${t.message}")
                runOnUiThread {
                    button.isEnabled = true
                    button.text = "إعادة الاتصال"
                }
                releaseAudio()
            }

            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                connected = false
                recording = false
                updateStatus("انتهت المحادثة")
                releaseAudio()
            }
        })
    }

    private fun startAudio() {
        try {
            val minBuffer = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (minBuffer <= 0) {
                throw Exception("تعذر تجهيز المايك")
            }

            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, 6400)
            )

            val outputSize = AudioTrack.getMinBufferSize(
                24000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            player = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SPEECH
                        ).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(24000)
                        .setChannelMask(
                            AudioFormat.CHANNEL_OUT_MONO
                        ).build()
                )
                .setBufferSizeInBytes(maxOf(outputSize, 12000))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioQueue.clear()
            player?.play()
            recorder?.startRecording()
            recording = true

            playbackThread = Thread {
                while (recording || audioQueue.isNotEmpty()) {
                    try {
                        val bytes = audioQueue.poll(
                            200,
                            TimeUnit.MILLISECONDS
                        ) ?: continue

                        player?.write(
                            bytes, 0, bytes.size,
                            AudioTrack.WRITE_BLOCKING
                        )
                    } catch (_: Exception) {
                        break
                    }
                }
            }.also { it.start() }

            audioThread = Thread {
                val buffer = ByteArray(3200)

                while (recording && connected) {
                    val count = recorder?.read(
                        buffer, 0, buffer.size
                    ) ?: -1

                    if (count > 0) {
                        val encoded = Base64.encodeToString(
                            buffer.copyOf(count),
                            Base64.NO_WRAP
                        )

                        val message = JSONObject().apply {
                            put("realtimeInput", JSONObject().apply {
                                put("audio", JSONObject().apply {
                                    put("mimeType", "audio/pcm;rate=16000")
                                    put("data", encoded)
                                })
                            })
                        }

                        socket?.send(message.toString())
                    }
                }
            }.also { it.start() }

        } catch (e: Exception) {
            updateStatus("تعذر تشغيل الصوت: ${e.message}")
            stopLive()
        }
    }

    private fun releaseAudio() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null

        try {
            player?.pause()
            player?.flush()
            player?.release()
        } catch (_: Exception) {}
        player = null

        audioQueue.clear()
    }

    private fun stopLive() {
        recording = false
        connected = false

        socket?.close(1000, "User ended session")
        socket = null

        releaseAudio()

        runOnUiThread {
            button.text = "ابدأ المحادثة"
            button.isEnabled = true
            updateStatus("انتهت المحادثة")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 701 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLive()
        }
    }

    override fun onDestroy() {
        stopLive()
        super.onDestroy()
    }
}
