package org.lsposed.lspatch.ui.activity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.startDestination
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import org.lsposed.lspatch.ui.page.BottomBarDestination
import org.lsposed.lspatch.ui.theme.LSPTheme
import org.lsposed.lspatch.ui.util.LocalSnackbarHost
import soup.compose.material.motion.animation.materialSharedAxisZIn
import soup.compose.material.motion.animation.materialSharedAxisZOut
import soup.compose.material.motion.animation.rememberSlideDistance
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalBottomHazeState = compositionLocalOf { HazeState() }

val LocalBottomBarHeight = compositionLocalOf { mutableStateOf(0.dp) }

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val colorMode = remember { mutableIntStateOf(0) }
            val darkMode =
                colorMode.intValue == 2 || (isSystemInDarkTheme() && colorMode.intValue == 0)
            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkMode },
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced =
                        false // Xiaomi moment, this code must be here
                }

                onDispose {}
            }
            val navController = rememberNavController()
            LSPTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
                    val hazeState = rememberHazeState()
                    CompositionLocalProvider(LocalBottomHazeState provides hazeState) {
                        var bottomBarHeight = remember { mutableStateOf(0.dp) }
                        val density = LocalDensity.current
                        CompositionLocalProvider(LocalBottomBarHeight provides bottomBarHeight) {
                            Scaffold(
                                bottomBar = {
                                    val hazeStyle = HazeStyle(
                                        backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                                        tint = HazeTint(
                                            MiuixTheme.colorScheme.surfaceContainer.copy(
                                                0.67f
                                            )
                                        )
                                    )
                                    BottomBar(
                                        modifier = Modifier
                                            .onGloballyPositioned { layoutCoordinates ->
                                                val heightPx = layoutCoordinates.size.height
                                                bottomBarHeight.value =
                                                    with(density) { heightPx.toDp() }
                                            }
                                            .hazeEffect(hazeState) {
                                                style = hazeStyle
                                                blurRadius = 25.dp
                                                noiseFactor = 0f
                                            },
                                        navController = navController
                                    )
                                },
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                            ) { innerPadding ->
                                DestinationsNavHost(
                                    defaultTransitions = rememberNavAnim(),
                                    navGraph = NavGraphs.root,
                                    navController = navController,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private typealias Ty = AnimatedContentTransitionScope<NavBackStackEntry>

@Composable
fun rememberNavAnim(): NavHostAnimatedDestinationStyle {
    val slideDistance = rememberSlideDistance()
    return remember(slideDistance) {
        object : NavHostAnimatedDestinationStyle() {
            override val enterTransition: Ty.() -> EnterTransition =
                { materialSharedAxisZIn(true, slideDistance) }
            override val exitTransition: Ty.() -> ExitTransition =
                { materialSharedAxisZOut(true, slideDistance) }
            override val popEnterTransition: Ty.() -> EnterTransition =
                { materialSharedAxisZIn(false, slideDistance) }
            override val popExitTransition: Ty.() -> ExitTransition =
                { materialSharedAxisZOut(false, slideDistance) }
        }
    }
}

@Composable
private fun BottomBar(modifier: Modifier, navController: NavHostController) {
    val currentDestination = navController.currentDestinationAsState().value
        ?: NavGraphs.root.startDestination
    var topDestination by rememberSaveable { mutableStateOf(currentDestination.route) }
    LaunchedEffect(currentDestination) {
        val queue = navController.currentBackStack.value
        if (queue.size == 2) topDestination = queue[1].destination.route!!
        else if (queue.size > 2) topDestination = queue[2].destination.route!!
    }

    NavigationBar(
        modifier = modifier,
        items = BottomBarDestination.entries.map { destination ->
            NavigationItem(
                icon = destination.iconSelected,
                label = stringResource(destination.label),
            )
        },
        color = androidx.compose.ui.graphics.Color.Unspecified,
        selected = BottomBarDestination.entries.indexOfFirst { it.direction.route == topDestination },
        onClick = {
            val destination = BottomBarDestination.entries[it]
            navController.navigate(destination.direction.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    )
}