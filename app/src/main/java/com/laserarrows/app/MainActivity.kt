package com.laserarrows.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import com.laserarrows.app.ui.game.GameScreen
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.LaserArrowsTheme
import com.laserarrows.app.ui.update.AppUpdateOverlay
import com.laserarrows.app.ui.update.AppUpdateUiState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var availableUpdate: AppUpdateResult.Available? = null
    private var downloadedUpdate: AppUpdateResult.Downloaded? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appUpdateManager = AppUpdateManagerFactory.create(this)
        updateResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

        var updateUiState by mutableStateOf<AppUpdateUiState>(AppUpdateUiState.None)

        lifecycleScope.launch {
            runCatching {
                appUpdateManager.requestUpdateFlow()
                    .catch { /* 更新チェックに失敗しても本編には影響させない(Play未経由のインストール時など) */ }
                    .collect { result ->
                        when (result) {
                            is AppUpdateResult.Available -> {
                                availableUpdate = result
                                updateUiState = AppUpdateUiState.Available
                            }
                            is AppUpdateResult.Downloaded -> {
                                downloadedUpdate = result
                                updateUiState = AppUpdateUiState.ReadyToInstall
                            }
                            else -> Unit
                        }
                    }
            }
        }

        setContent {
            LaserArrowsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg1) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GameScreen()
                        AppUpdateOverlay(
                            state = updateUiState,
                            onUpdateClick = {
                                runCatching { availableUpdate?.startFlexibleUpdate(updateResultLauncher) }
                                updateUiState = AppUpdateUiState.None
                            },
                            onInstallClick = {
                                lifecycleScope.launch { runCatching { downloadedUpdate?.completeUpdate() } }
                            },
                            onDismiss = { updateUiState = AppUpdateUiState.None },
                        )
                    }
                }
            }
        }
    }
}
