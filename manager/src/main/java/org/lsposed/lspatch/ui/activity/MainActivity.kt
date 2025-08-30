package org.lsposed.lspatch.ui.activity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.SelectAppsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.lsposed.lspatch.ui.page.BottomBarDestination
import org.lsposed.lspatch.ui.page.HomeScreen
import org.lsposed.lspatch.ui.page.ManageScreen
import org.lsposed.lspatch.ui.page.SelectAppsResult
import org.lsposed.lspatch.ui.page.SettingsScreen
import org.lsposed.lspatch.ui.theme.LSPTheme
import org.lsposed.lspatch.ui.util.LocalSnackbarHost
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { innerPadding ->
                        DestinationsNavHost(
                            modifier = Modifier,
                            navGraph = NavGraphs.root,
                            navController = navController,
                            defaultTransitions = object :
                                NavHostAnimatedDestinationStyle() {
                                override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                    {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }

                                override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                    {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 5 },
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }

                                override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                    {
                                        slideInHorizontally(
                                            initialOffsetX = { -it / 5 },
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }

                                override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                    {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}

val LocalPagerState = compositionLocalOf<PagerState> { error("No pager state") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("No handle page change") }

@Composable
@Destination<RootGraph>(start = true)
fun MainScreen(
    navController: DestinationsNavigator,
    resultRecipient: ResultRecipient<SelectAppsScreenDestination, SelectAppsResult>,
) {
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(MiuixTheme.colorScheme.background.copy(0.8f))
    )
    val handlePageChange: (Int) -> Unit = remember(pagerState, coroutineScope) {
        { page ->
            coroutineScope.launch { pagerState.animateScrollToPage(page) }
        }
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            activity?.finishAndRemoveTask()
        }
    }

    CompositionLocalProvider(
        LocalPagerState provides pagerState,
        LocalHandlePageChange provides handlePageChange
    ) {
        Scaffold(
            bottomBar = {
                BottomBar(hazeState, hazeStyle)
            },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier.hazeSource(state = hazeState),
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = false
            ) {
                when (it) {
                    0 -> HomeScreen(
                        pagerState,
                        navController,
                        innerPadding.calculateBottomPadding()
                    )

                    1 -> ManageScreen(
                        navController,
                        innerPadding.calculateBottomPadding(),
                        resultRecipient
                    )

                    2 -> SettingsScreen(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    hazeState: HazeState,
    hazeStyle: HazeStyle
) {
    val page = LocalPagerState.current.targetPage
    val handlePageChange = LocalHandlePageChange.current

    NavigationBar(
        modifier = Modifier
            .hazeEffect(hazeState) {
                style = hazeStyle
                blurRadius = 30.dp
                noiseFactor = 0f
            },
        items = BottomBarDestination.entries.map { destination ->
            NavigationItem(
                icon = destination.iconSelected,
                label = stringResource(destination.label),
            )
        },
        color = androidx.compose.ui.graphics.Color.Transparent,
        selected = page,
        onClick = handlePageChange
    )
}