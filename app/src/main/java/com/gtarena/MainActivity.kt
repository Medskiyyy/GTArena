package com.gtarena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.gtarena.data.RootShell
import com.gtarena.service.ArenaService
import com.gtarena.ui.AppRoot
import com.gtarena.ui.theme.GTArenaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            RootShell.run(
                "pm grant $packageName android.permission.POST_NOTIFICATIONS; " +
                    "appops set $packageName SYSTEM_ALERT_WINDOW allow"
            )
            ArenaService.start(this@MainActivity)
        }
        val openSidebar = intent?.getStringExtra(EXTRA_SCREEN) == SCREEN_SIDEBAR
        setContent {
            GTArenaTheme {
                AppRoot(openSidebar = openSidebar)
            }
        }
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
        const val SCREEN_SIDEBAR = "sidebar"
    }

    override fun onStart() {
        super.onStart()
        ArenaService.setUiVisible(true)
    }

    override fun onStop() {
        ArenaService.setUiVisible(false)
        super.onStop()
    }
}
