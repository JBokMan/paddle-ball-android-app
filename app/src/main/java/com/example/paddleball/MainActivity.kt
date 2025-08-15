package com.example.paddleball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.paddleball.ui.GameScreen
import com.example.paddleball.ui.GameViewModel
import com.example.paddleball.ui.theme.PaddleballTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current

            // Get screen dimensions in pixels
            val screenWidthPx: Float
            val screenHeightPx: Float

            with(density) {
                screenWidthPx = configuration.screenWidthDp.dp.toPx()
                screenHeightPx = configuration.screenHeightDp.dp.toPx()
            }

            PaddleballTheme {
                // Pass screen dimensions to the ViewModel
                GameScreen(
                    viewModel = GameViewModel(screenWidthPx, screenHeightPx)
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PaddleballTheme {
        Greeting("Android")
    }
}