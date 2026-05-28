package com.englishvoicebuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.englishvoicebuddy.ui.ChatScreen
import com.englishvoicebuddy.ui.theme.BgWarm
import com.englishvoicebuddy.viewmodel.ChatViewModel
import java.io.File
import java.io.StringWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全局崩溃捕获
        val logFile = File(filesDir, "debug.log")
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                logFile.appendText("$ts FATAL: ${sw.toString().take(2000)}\n")
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, e)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BgWarm,
            ) {
                val vm: ChatViewModel = viewModel()
                ChatScreen(viewModel = vm)
            }
        }
    }
}
