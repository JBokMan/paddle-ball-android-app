package com.example.paddleball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.paddleball.ui.GameScreen
import com.example.paddleball.ui.MainViewModel
import com.example.paddleball.ui.di.AppModule
import com.example.paddleball.ui.di.NavigationRoutes
import com.example.paddleball.ui.theme.PaddleballTheme
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.compose.koinViewModel
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
            val viewModel: MainViewModel = koinViewModel()
            val backStack by viewModel.backStack.collectAsState()

            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val screenWidthPx: Float
            val screenHeightPx: Float

            with(density) {
                screenWidthPx = configuration.screenWidthDp.dp.toPx()
                screenHeightPx = configuration.screenHeightDp.dp.toPx()
            }

            PaddleballTheme {
                NavExample(
                    backStack = backStack,
                    onBack = viewModel::popBackStack,
                    navigateToGameScreen = {viewModel.navigateToGameScreen(screenWidthPx, screenHeightPx)}
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


@Composable
fun NavExample(
    backStack: List<NavigationRoutes>,
    onBack: (Int) -> Unit,
    navigateToGameScreen: () -> Unit
) {
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryProvider = { key ->
            when (key) {
                is NavigationRoutes.Home -> NavEntry(key) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Home")
                        Button(onClick = {
                            navigateToGameScreen()
                        }) {
                            Text("Go to Game")
                        }
                    }
                }

                is NavigationRoutes.Game -> NavEntry(key) {
                    GameScreen()
                }
            }
        }
    )
}