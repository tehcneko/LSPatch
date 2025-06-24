package org.lsposed.lspatch.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.startDestination
import org.lsposed.lspatch.ui.page.BottomBarDestination
import org.lsposed.lspatch.ui.theme.LSPTheme
import org.lsposed.lspatch.ui.util.LocalSnackbarHost

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val navController = rememberNavController()
            LSPTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
                    Scaffold(
                        bottomBar = { BottomBar(navController) },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { innerPadding ->
                        DestinationsNavHost(
                            modifier = Modifier.consumeWindowInsets(innerPadding)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(innerPadding),
                            navGraph = NavGraphs.root,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val currentDestination = navController.currentDestinationAsState().value
        ?: NavGraphs.root.startDestination
    var topDestination by rememberSaveable { mutableStateOf(currentDestination.route) }
    LaunchedEffect(currentDestination) {
        val queue = navController.currentBackStack.value
        if (queue.size == 2) topDestination = queue[1].destination.route!!
        else if (queue.size > 2) topDestination = queue[2].destination.route!!
    }

    NavigationBar(tonalElevation = 8.dp) {
        BottomBarDestination.values().forEach { destination ->
            NavigationBarItem(
                selected = topDestination == destination.direction.route,
                onClick = {
                    navController.navigate(destination.direction.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (topDestination == destination.direction.route) Icon(destination.iconSelected, stringResource(destination.label))
                    else Icon(destination.iconNotSelected, stringResource(destination.label))
                },
                label = { Text(stringResource(destination.label)) },
                alwaysShowLabel = false
            )
        }
    }
}
