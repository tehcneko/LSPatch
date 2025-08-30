package org.lsposed.lspatch.ui.page

import android.content.pm.ApplicationInfo
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.parcelize.Parcelize
import org.lsposed.lspatch.R
import org.lsposed.lspatch.ui.component.AppItem
import org.lsposed.lspatch.ui.component.ListCard
import org.lsposed.lspatch.ui.viewmodel.SelectAppsViewModel
import org.lsposed.lspatch.util.LSPPackageManager
import org.lsposed.lspatch.util.LSPPackageManager.AppInfo
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.getWindowSize
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Parcelize
sealed class SelectAppsResult : Parcelable {
    data class SingleApp(val selected: AppInfo) : SelectAppsResult()
    data class MultipleApps(val selected: List<AppInfo>) : SelectAppsResult()
}

@Destination<RootGraph>
@Composable
fun SelectAppsScreen(
    navigator: ResultBackNavigator<SelectAppsResult>,
    multiSelect: Boolean,
    initialSelected: ArrayList<String>? = null
) {
    val viewModel = viewModel<SelectAppsViewModel>()

    var searchPackage by remember { mutableStateOf("") }
    val filter: (AppInfo) -> Boolean = {
        val packageLowerCase = searchPackage.toLowerCase(Locale.current)
        val contains = it.label.toLowerCase(Locale.current)
            .contains(packageLowerCase) || it.app.packageName.contains(packageLowerCase)
        if (multiSelect) contains && it.isXposedModule
        else contains && it.app.flags and ApplicationInfo.FLAG_SYSTEM == 0
    }

    LaunchedEffect(Unit) {
        viewModel.filterAppList(false, filter)
        initialSelected?.let {
            val tmp = initialSelected.toSet()
            viewModel.multiSelected.addAll(LSPPackageManager.appList.filter { tmp.contains(it.app.packageName) })
        }
    }

    BackHandler {
        navigator.navigateBack()
    }

    var expanded by remember { mutableStateOf(false) }

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
                    title = stringResource(R.string.screen_select_apps),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.padding(start = 20.dp),
                            onClick = { navigator.navigateBack() }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Useful.Back,
                                contentDescription = "Back",
                            )
                        }
                    },
                    color = Color.Unspecified
                )
                SearchBar(
                    modifier = Modifier.padding(vertical = 12.dp),
                    inputField = {
                        InputField(
                            query = searchPackage,
                            onQueryChange = {
                                searchPackage = it
                            },
                            onSearch = { viewModel.filterAppList(false, filter) },
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier
                    .padding(bottom = 20.dp, end = 20.dp),
                visible = multiSelect,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MultiSelectFab {
                    navigator.navigateBack(SelectAppsResult.MultipleApps(viewModel.multiSelected))
                }
            }
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        PullToRefresh(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.filterAppList(true, filter) },
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 6.dp,
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection)
            ),
        ) {
            if (multiSelect) MultiSelect(
                scrollBehavior,
                innerPadding,
                hazeState,
            )
            else SingleSelect(
                onSelect = {
                    navigator.navigateBack(SelectAppsResult.SingleApp(it))
                },
                scrollBehavior,
                innerPadding,
                hazeState,
            )
        }
    }
}

@Composable
private fun MultiSelectFab(onClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier
            .border(0.05.dp, colorScheme.outline.copy(alpha = 0.5f), CircleShape),
        shadowElevation = 0.dp,
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.Done,
            contentDescription = stringResource(R.string.add),
            tint = colorScheme.onPrimaryContainer,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SingleSelect(
    onSelect: (AppInfo) -> Unit,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    hazeState: HazeState,
) {
    val viewModel = viewModel<SelectAppsViewModel>()
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier
            .height(getWindowSize().height.dp)
            .scrollEndHaptic()
            .overScrollVertical()
            .scrollEndHaptic()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .hazeSource(hazeState),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 6.dp,
            start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
            end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp
        ),
        overscrollEffect = null,
    ) {
        itemsIndexed(
            items = viewModel.filteredList,
            key = { index, item -> item.app.packageName }
        ) { index, item ->
            ListCard(
                index = index,
                size = viewModel.filteredList.size
            ) {
                AppItem(
                    modifier = Modifier
                        .animateItem(spring(stiffness = Spring.StiffnessLow))
                        .clickable { onSelect(item) },
                    icon = LSPPackageManager.getIcon(item),
                    label = item.label,
                    packageName = item.app.packageName
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiSelect(
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    hazeState: HazeState,
) {
    val viewModel = viewModel<SelectAppsViewModel>()
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier
            .height(getWindowSize().height.dp)
            .scrollEndHaptic()
            .overScrollVertical()
            .scrollEndHaptic()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .hazeSource(hazeState),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 6.dp,
            start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
            end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp
        ),
        overscrollEffect = null,
    ) {
        itemsIndexed(
            items = viewModel.filteredList,
            key = { index, item -> item.app.packageName }
        ) { index, item ->
            ListCard(
                index = index,
                size = viewModel.filteredList.size
            ) {
                val checked = viewModel.multiSelected.contains(item)
                AppItem(
                    modifier = Modifier
                        .animateItem(spring(stiffness = Spring.StiffnessLow))
                        .clickable {
                            if (checked) viewModel.multiSelected.remove(item)
                            else viewModel.multiSelected.add(item)
                        },
                    icon = LSPPackageManager.getIcon(item),
                    label = item.label,
                    packageName = item.app.packageName,
                    checked = checked
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp + 20.dp))
        }
    }
}
