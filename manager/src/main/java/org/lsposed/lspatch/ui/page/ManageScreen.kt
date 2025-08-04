package org.lsposed.lspatch.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SelectAppsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.lsposed.lspatch.R
import org.lsposed.lspatch.ui.activity.LocalBottomBarHeight
import org.lsposed.lspatch.ui.page.manage.AppManageBody
import org.lsposed.lspatch.ui.page.manage.AppManageFab
import org.lsposed.lspatch.ui.page.manage.ModuleManageBody
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalHazeState = compositionLocalOf { HazeState() }

@Destination<RootGraph>
@Composable
fun ManageScreen(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<SelectAppsScreenDestination, SelectAppsResult>,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val tabs = listOf(
        stringResource(R.string.apps),
        stringResource(R.string.modules)
    )
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = rememberHazeState()
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(
            MiuixTheme.colorScheme.background.copy(
                if (scrollBehavior.state.collapsedFraction <= 0f) 1f
                else lerp(1f, 0.67f, (scrollBehavior.state.collapsedFraction))
            )
        )
    )
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = 25.dp
                        noiseFactor = 0f
                    }
            ) {
                TopAppBar(
                    title = stringResource(R.string.screen_manage),
                    scrollBehavior = scrollBehavior,
                    color = Color.Unspecified
                )
                TabRow(
                    modifier = Modifier.padding(12.dp),
                    tabs = tabs,
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = {
                        scope.launch { pagerState.animateScrollToPage(it) }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier
                    .padding(bottom = LocalBottomBarHeight.current.value),
                visible = pagerState.currentPage == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AppManageFab(navigator)
            }
        },
        popupHost = {},
    ) { innerPadding ->
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            HorizontalPager(
                state = pagerState,
                pageContent = { page ->
                    when (page) {
                        0 -> AppManageBody(
                            navigator,
                            resultRecipient,
                            scrollBehavior,
                            innerPadding
                        )

                        1 -> ModuleManageBody(
                            scrollBehavior,
                            innerPadding
                        )
                    }
                }
            )
        }
    }
}
