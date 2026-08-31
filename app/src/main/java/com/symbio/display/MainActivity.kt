package com.symbio.display

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

/**
 * SymbioDisplay
 *
 * WebView de pantalla completa para Android TV que SIEMPRE renderiza
 * el contenido web como si la pantalla fuera Full HD (1920x1080),
 * sin importar si el panel físico es 4K u otra resolución.
 *
 * Cómo funciona:
 * En vez de dejar que el WebView mida el ancho/alto real del panel (lo que
 * en una TV 4K hace que el navegador use un viewport CSS mucho más grande
 * y el contenido se vea "chico" en proporción), fijamos el layout del
 * WebView a exactamente 1920x1080 px y luego usamos scaleX/scaleY para
 * estirar esa superficie hasta cubrir la pantalla real. Es el mismo
 * principio que usa un reproductor de TV box al hacer upscaling de una
 * señal 1080p a un panel 4K: el contenido se dibuja a 1080p y el hardware
 * lo agranda, así que el tamaño relativo de los elementos en pantalla
 * queda siempre igual, sin achicarse en TVs de mayor resolución.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TARGET_URL = "https://symbio2.vercel.app/client"
        private const val TARGET_WIDTH_PX = 1920
        private const val TARGET_HEIGHT_PX = 1080
        private const val RELOAD_ON_ERROR_DELAY_MS = 5000L
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private val reloadHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla completa, sin barras de sistema (modo kiosco/señalética)
        applyFullscreen()

        // Mantener la pantalla encendida: es una app de cartelería/visualización
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        forceFullHdViewport()

        webView.loadUrl(TARGET_URL)
    }

    private fun applyFullscreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullscreen()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        // Forzamos densidad 1 (mdpi) para que el WebView calcule el viewport
        // CSS a partir de píxeles "reales" de 1920x1080, en vez de aplicar
        // el factor de densidad propio de la TV (que en un panel 4K suele
        // ser xhdpi/xxhdpi y termina achicando el contenido).
        settings.defaultZoom = WebSettings.ZoomDensity.FAR

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    scheduleReload()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Fija el WebView a un lienzo de 1920x1080 px reales y lo escala para
     * cubrir toda la pantalla física, sin importar su resolución real.
     */
    private fun forceFullHdViewport() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
        } else {
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }

        val realWidth = metrics.widthPixels
        val realHeight = metrics.heightPixels

        webView.post {
            val params = webView.layoutParams as ViewGroup.LayoutParams
            params.width = TARGET_WIDTH_PX
            params.height = TARGET_HEIGHT_PX
            webView.layoutParams = params

            webView.pivotX = 0f
            webView.pivotY = 0f
            webView.scaleX = realWidth / TARGET_WIDTH_PX.toFloat()
            webView.scaleY = realHeight / TARGET_HEIGHT_PX.toFloat()
        }
    }

    private fun scheduleReload() {
        reloadHandler.removeCallbacksAndMessages(null)
        reloadHandler.postDelayed({
            webView.loadUrl(TARGET_URL)
        }, RELOAD_ON_ERROR_DELAY_MS)
    }

    override fun onDestroy() {
        reloadHandler.removeCallbacksAndMessages(null)
        webView.destroy()
        super.onDestroy()
    }
}
