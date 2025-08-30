package org.lsposed.lspatch.ui.page.manage

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.lsposed.lspatch.R
import org.lsposed.lspatch.ui.component.AppItem
import org.lsposed.lspatch.ui.component.ListCard
import org.lsposed.lspatch.ui.component.StatusTag
import org.lsposed.lspatch.ui.viewmodel.manage.ModuleManageViewModel
import org.lsposed.lspatch.util.LSPPackageManager
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.DropdownImpl
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.getWindowSize
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ModuleManageBody(
    bottomInnerPadding: Dp,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    hazeState: HazeState,
) {
    val context = LocalContext.current
    val viewModel = viewModel<ModuleManageViewModel>()
    if (viewModel.appList.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = run {
                    if (LSPPackageManager.appList.isEmpty()) stringResource(R.string.manage_loading)
                    else stringResource(R.string.manage_no_modules)
                },
                style = MiuixTheme.textStyles.headline2
            )
        }
    } else {
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
                items = viewModel.appList,
                key = { index, item -> item.first.app.packageName }
            ) { index, item ->
                val settingsIntent =
                    remember { LSPPackageManager.getSettingsIntent(item.first.app.packageName) }
                val showPopup = remember { mutableStateOf(false) }
                ListCard(
                    index = index,
                    size = viewModel.appList.size
                ) {
                    AppItem(
                        holdDownState = showPopup.value,
                        onClick = {
                            showPopup.value = !showPopup.value
                        },
                        icon = LSPPackageManager.getIcon(item.first),
                        label = item.first.label,
                        packageName = item.first.app.packageName,
                        additionalContent = {
                            Text(
                                text = item.second.description,
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                            StatusTag(
                                label = "API " + item.second.api.toString(),
                                backgroundColor = colorScheme.tertiaryContainer,
                                contentColor = colorScheme.onTertiaryContainer
                            )
                        }
                    )
                    val DropdownPositionProvider = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowBounds: IntRect,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize,
                            popupMargin: IntRect,
                            alignment: PopupPositionProvider.Align
                        ): IntOffset {
                            return ListPopupDefaults.DropdownPositionProvider.calculatePosition(
                                anchorBounds = anchorBounds,
                                windowBounds = windowBounds,
                                layoutDirection = layoutDirection,
                                popupContentSize = popupContentSize,
                                popupMargin = popupMargin,
                                alignment = alignment
                            )
                        }

                        override fun getMargins(): PaddingValues {
                            return PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        }
                    }
                    ListPopup(
                        show = showPopup,
                        onDismissRequest = { showPopup.value = false },
                        popupPositionProvider = DropdownPositionProvider
                    ) {
                        ListPopupColumn {
                            if (settingsIntent != null) {
                                DropdownImpl(
                                    text = stringResource(R.string.manage_module_settings),
                                    optionSize = 2,
                                    isSelected = false,
                                    index = 0,
                                    onSelectedIndexChange = {
                                        showPopup.value = false
                                        context.startActivity(settingsIntent)
                                    }
                                )
                            }
                            DropdownImpl(
                                text = stringResource(R.string.manage_app_info),
                                optionSize = if (settingsIntent != null) 2 else 1,
                                isSelected = false,
                                index = if (settingsIntent != null) 1 else 0,
                                onSelectedIndexChange = {
                                    showPopup.value = false
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", item.first.app.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(bottomInnerPadding + 12.dp))
            }
        }
    }
}
