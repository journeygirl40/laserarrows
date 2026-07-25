package com.laserarrows.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.laserarrows.app.ui.game.GameScreen
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.LaserArrowsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaserArrowsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg1) {
                    GameScreen()
                }
            }
        }
    }
}
