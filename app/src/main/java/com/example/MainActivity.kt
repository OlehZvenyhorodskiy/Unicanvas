package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.CanvasRepository
import com.example.ui.editor.CanvasEditorScreen
import com.example.ui.editor.CanvasEditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeCanvasApp()
                }
            }
        }
    }
}

@Composable
fun MeCanvasApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { CanvasRepository(context) }
    val homeViewModel = remember { HomeViewModel(repository) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onCanvasClick = { canvasId ->
                    navController.navigate("editor/$canvasId")
                }
            )
        }

        composable(
            route = "editor/{canvasId}",
            arguments = listOf(navArgument("canvasId") { type = NavType.StringType })
        ) { backStackEntry ->
            val canvasId = backStackEntry.arguments?.getString("canvasId") ?: return@composable
            val editorViewModel = remember(canvasId) {
                CanvasEditorViewModel(repository, canvasId, context)
            }
            CanvasEditorScreen(
                viewModel = editorViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

