package com.ledger.tracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.ledger.tracker.ui.LedgerScreen
import com.ledger.tracker.ui.LedgerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var accessEnabled by remember { mutableStateOf(isNotificationAccessEnabled()) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LedgerScreen(
                        viewModel = viewModel,
                        notificationAccessEnabled = accessEnabled,
                        onOpenNotificationSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check every time the user comes back (e.g. after granting access in Settings).
        recreate()
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(packageName)
    }
}
