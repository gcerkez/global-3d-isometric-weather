package com.antigravity.yorktownweather

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * MainActivity
 * ============
 * Interactive 3D Isometric Weather App & Homescreen/Lockscreen Widget Configurator
 * for OnePlus 13 (OxygenOS 15) and Android 8.0+ devices.
 */
class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var webView: WebView
    private lateinit var statusLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val curName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0d1b2a"))
        }

        // 1. Top Interactive 3D Isometric WebView Canvas
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()
            setBackgroundColor(Color.parseColor("#0d1b2a"))
            loadUrl("file:///android_asset/web/index.html")
        }
        rootLayout.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        )

        // 2. Bottom Native Widget Configuration Panel
        val configCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#182838"))
            setPadding(36, 28, 36, 36)
        }

        val titleView = TextView(this).apply {
            text = "Widget Location & Settings (Active: $curName)"
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        configCard.addView(titleView)

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 14, 0, 10)
        }

        val cityInput = EditText(this).apply {
            hint = "Search any city (e.g., Yorktown, San Francisco, Tokyo)..."
            setHintTextColor(Color.parseColor("#8aa4bc"))
            setTextColor(Color.WHITE)
            setSingleLine(true)
        }
        searchRow.addView(
            cityInput,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        )

        val searchBtn = Button(this).apply {
            text = "Set City"
            setOnClickListener {
                val query = cityInput.text.toString().trim()
                if (query.length >= 2) {
                    geocodeAndApplyCity(query, titleView)
                }
            }
        }
        searchRow.addView(searchBtn)
        configCard.addView(searchRow)

        // Quick Presets Row
        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 10)
        }
        val presets = listOf(
            Triple("Yorktown, VA", 37.2388f, -76.5097f),
            Triple("Williamsburg, VA", 37.2707f, -76.7075f),
            Triple("San Francisco, CA", 37.7749f, -122.4194f)
        )
        for ((name, lat, lon) in presets) {
            val btn = Button(this).apply {
                text = name
                textSize = 11f
                setOnClickListener {
                    saveLocationAndRefreshWidgets(name, lat, lon, titleView)
                }
            }
            presetRow.addView(
                btn,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            )
        }
        configCard.addView(presetRow)

        statusLabel = TextView(this).apply {
            text = "Long-press your OnePlus 13 Homescreen -> Widgets -> Global 3D Isometric Weather to place on Homescreen or Lockscreen."
            setTextColor(Color.parseColor("#a8dbf8"))
            textSize = 12f
        }
        configCard.addView(statusLabel)

        rootLayout.addView(
            configCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(rootLayout)
    }

    /**
     * Geocodes a city name via Open-Meteo Geocoding API and updates all active widgets.
     */
    private fun geocodeAndApplyCity(query: String, titleView: TextView) {
        statusLabel.text = "Searching '$query'..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val results = JSONObject(body).optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val r = results.getJSONObject(0)
                        val name = r.getString("name")
                        val admin = r.optString("admin1", "")
                        val display = if (admin.isNotEmpty()) "$name, $admin" else name
                        Triple(display, r.getDouble("latitude").toFloat(), r.getDouble("longitude").toFloat())
                    } else null
                }.getOrNull()
            }
            if (result != null) {
                saveLocationAndRefreshWidgets(result.first, result.second, result.third, titleView)
            } else {
                statusLabel.text = "Could not find matching city for '$query'."
            }
        }
    }

    /**
     * Persists selected city coordinates and triggers an immediate Homescreen/Lockscreen widget redraw.
     */
    private fun saveLocationAndRefreshWidgets(name: String, lat: Float, lon: Float, titleView: TextView) {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("locationName", name)
            .putFloat("latitude", lat)
            .putFloat("longitude", lon)
            .apply()

        titleView.text = "Widget Location & Settings (Active: $name)"
        statusLabel.text = "Updated widget to $name [$lat, $lon]!"

        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, IsometricWeatherWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val updateIntent = Intent(this, IsometricWeatherWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(updateIntent)
        }

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
