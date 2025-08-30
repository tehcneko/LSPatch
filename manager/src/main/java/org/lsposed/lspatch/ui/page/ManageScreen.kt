package org.lsposed.lspatch.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.ramcosta.composedestinations.generated.destinations.SelectAppsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.lsposed.lspatch.R
import org.lsposed.lspatch.ui.page.manage.AppManageBody
import org.lsposed.lspatch.ui.page.manage.AppManageFab
import org.lsposed.lspatch.ui.page.manage.ModuleManageBody
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ManageScreen(
    navigator: DestinationsNavigator,
    bottomInnerPadding: Dp,
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
        backgroundColor = colorScheme.background,
        tint = HazeTint(
            colorScheme.background.copy(
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
                    color = Color.Unspecified,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TabRow(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
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
                visible = pagerState.currentPage == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AppManageFab(
                    modifier = Modifier
                        .padding(bottom = bottomInnerPadding + 20.dp, end = 20.dp),
                    navigator
                )
            }
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            pageContent = { page ->
                when (page) {
                    0 -> AppManageBody(
                        navigator,
                        bottomInnerPadding,
                        resultRecipient,
                        scrollBehavior,
                        innerPadding,
                        hazeState,
                    )

                    1 -> ModuleManageBody(
                        bottomInnerPadding,
                        scrollBehavior,
                        innerPadding,
                        hazeState,
                    )
                }
            }
        )
    }
}
