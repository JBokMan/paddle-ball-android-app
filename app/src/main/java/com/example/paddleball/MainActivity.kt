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
import com.example.paddleball.ui.di.AppModule
import com.example.paddleball.ui.theme.PaddleballTheme
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

@OptIn(KoinExperimentalAPI::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@MainActivity)
            // Load modules
            modules(AppModule().module)
        }

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
                GameScreen()
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