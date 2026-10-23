package com.example

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.StudioMainScreen
import com.example.ui.StudioViewModel
import com.example.ui.theme.MotionForgeTheme

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        var currentActivity: MainActivity? = null
            private set
    }

    private val studioViewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before any WebView is created to allow software Canvas drawing
        WebView.enableSlowWholeDocumentDraw()
        super.onCreate(savedInstanceState)
        currentActivity = this
        enableEdgeToEdge()
        setContent {
            MotionForgeTheme {
                StudioMainScreen(viewModel = studioViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        currentActivity = this
    }

    override fun onDestroy() {
        if (currentActivity == this) {
            currentActivity = null
        }
        super.onDestroy()
    }
}
