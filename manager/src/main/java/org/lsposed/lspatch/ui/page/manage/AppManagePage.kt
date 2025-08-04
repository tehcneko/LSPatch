package org.lsposed.lspatch.ui.page.manage

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.generated.destinations.NewPatchScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SelectAppsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.lsposed.lspatch.BuildConfig
import org.lsposed.lspatch.R
import org.lsposed.lspatch.config.ConfigManager
import org.lsposed.lspatch.config.Configs
import org.lsposed.lspatch.database.entity.Module
import org.lsposed.lspatch.lspApp
import org.lsposed.lspatch.share.Constants
import org.lsposed.lspatch.share.LSPConfig
import org.lsposed.lspatch.ui.activity.LocalBottomBarHeight
import org.lsposed.lspatch.ui.activity.LocalBottomHazeState
import org.lsposed.lspatch.ui.component.AppItem
import org.lsposed.lspatch.ui.component.ListCard
import org.lsposed.lspatch.ui.component.LoadingDialog
import org.lsposed.lspatch.ui.page.ACTION_APPLIST
import org.lsposed.lspatch.ui.page.ACTION_STORAGE
import org.lsposed.lspatch.ui.page.LocalHazeState
import org.lsposed.lspatch.ui.page.SelectAppsResult
import org.lsposed.lspatch.ui.util.LocalSnackbarHost
import org.lsposed.lspatch.ui.viewmodel.manage.AppManageViewModel
import org.lsposed.lspatch.ui.viewstate.ProcessingState
import org.lsposed.lspatch.util.LSPPackageManager
import org.lsposed.lspatch.util.ShizukuApi
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.DropdownImpl
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.New
import top.yukonga.miuix.kmp.icon.icons.useful.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.IOException

private const val TAG = "AppManagePage"

@Composable
fun AppManageBody(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<SelectAppsScreenDestination, SelectAppsResult>,
    scrollBehavior: ScrollBehavior,
    padding: PaddingValues,
) {
    val viewModel = viewModel<AppManageViewModel>()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    if (viewModel.appList.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = run {
                    if (LSPPackageManager.appList.isEmpty()) stringResource(R.string.manage_loading)
                    else stringResource(R.string.manage_no_apps)
                },
                style = MiuixTheme.textStyles.headline2
            )
        }
    } else {
        var scopeApp by rememberSaveable { mutableStateOf("") }
        resultRecipient.onNavResult {
            if (it is NavResult.Value) {
                scope.launch {
                    val result = it.value as SelectAppsResult.MultipleApps
                    ConfigManager.getModulesForApp(scopeApp).forEach {
                        ConfigManager.deactivateModule(scopeApp, it)
                    }
                    result.selected.forEach {
                        Log.d(TAG, "Activate ${it.app.packageName} for $scopeApp")
                        ConfigManager.activateModule(
                            scopeApp,
                            Module(it.app.packageName, it.app.sourceDir)
                        )
                    }
                }
            }
        }

        when (viewModel.updateLoaderState) {
            is ProcessingState.Idle -> Unit
            is ProcessingState.Processing -> LoadingDialog()
            is ProcessingState.Done -> {
                val it = viewModel.updateLoaderState as ProcessingState.Done
                val updateSuccessfully = stringResource(R.string.manage_update_loader_successfully)
                val updateFailed = stringResource(R.string.manage_update_loader_failed)
                val copyError = stringResource(R.string.copy_error)
                LaunchedEffect(Unit) {
                    it.result.onSuccess {
                        snackbarHost.showSnackbar(updateSuccessfully)
                    }.onFailure {
                        val result = snackbarHost.showSnackbar(updateFailed, copyError)
                        if (result == SnackbarResult.ActionPerformed) {
                            val cm =
                                lspApp.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("LSPatch", it.toString()))
                        }
                    }
                    viewModel.dispatch(AppManageViewModel.ViewAction.ClearUpdateLoaderResult)
                }
            }
        }
        when (viewModel.optimizeState) {
            is ProcessingState.Idle -> Unit
            is ProcessingState.Processing -> LoadingDialog()
            is ProcessingState.Done -> {
                val it = viewModel.optimizeState as ProcessingState.Done
                val optimizeSucceed = stringResource(R.string.manage_optimize_successfully)
                val optimizeFailed = stringResource(R.string.manage_optimize_failed)
                LaunchedEffect(Unit) {
                    snackbarHost.showSnackbar(if (it.result) optimizeSucceed else optimizeFailed)
                    viewModel.dispatch(AppManageViewModel.ViewAction.ClearOptimizeResult)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .hazeSource(state = LocalHazeState.current)
                .hazeSource(state = LocalBottomHazeState.current),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = LocalBottomBarHeight.current.value + 12.dp,
                start = 12.dp,
                end = 12.dp
            ),
        ) {
            itemsIndexed(
                items = viewModel.appList,
                key = { index, item -> item.first.app.packageName }
            ) { index, item ->
                val isRolling =
                    item.second.useManager && item.second.lspConfig.VERSION_CODE >= Constants.MIN_ROLLING_VERSION_CODE
                val canUpdateLoader =
                    !isRolling && item.second.lspConfig.VERSION_CODE < LSPConfig.instance.VERSION_CODE
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = buildAnnotatedString {
                                        val (text, color) =
                                            if (item.second.useManager) stringResource(R.string.patch_local) to MiuixTheme.colorScheme.primary
                                            else stringResource(R.string.patch_integrated) to MiuixTheme.colorScheme.primary
                                        append(AnnotatedString(text, SpanStyle(color = color)))
                                        append("  ")
                                        if (isRolling) append(stringResource(R.string.manage_rolling))
                                        else append(item.second.lspConfig.VERSION_CODE.toString())
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    style = MiuixTheme.textStyles.body2
                                )
                                if (canUpdateLoader) {
                                    with(LocalDensity.current) {
                                        val size =
                                            MiuixTheme.textStyles.body2.fontSize * 1.2
                                        Icon(
                                            MiuixIcons.Useful.Update,
                                            null,
                                            Modifier.size(size.toDp())
                                        )
                                    }
                                }
                            }
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
                            val shizukuUnavailable = stringResource(R.string.shizuku_unavailable)
                            if (canUpdateLoader || BuildConfig.DEBUG) {
                                DropdownImpl(
                                    text = stringResource(R.string.manage_update_loader),
                                    optionSize = if (item.second.useManager) 4 else 3,
                                    isSelected = false,
                                    index = 0,
                                    onSelectedIndexChange = {
                                        showPopup.value = false
                                        scope.launch {
                                            if (!ShizukuApi.isPermissionGranted) {
                                                snackbarHost.showSnackbar(shizukuUnavailable)
                                            } else {
                                                viewModel.dispatch(
                                                    AppManageViewModel.ViewAction.UpdateLoader(
                                                        item.first,
                                                        item.second
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            if (item.second.useManager) {
                                DropdownImpl(
                                    text = stringResource(R.string.manage_module_scope),
                                    optionSize = if (canUpdateLoader || BuildConfig.DEBUG) 4 else 3,
                                    isSelected = false,
                                    index = if (canUpdateLoader || BuildConfig.DEBUG) 1 else 0,
                                    onSelectedIndexChange = {
                                        showPopup.value = false
                                        scope.launch {
                                            scopeApp = item.first.app.packageName
                                            val activated =
                                                ConfigManager.getModulesForApp(scopeApp)
                                                    .map { it.pkgName }
                                                    .toSet()
                                            val initialSelected =
                                                LSPPackageManager.appList.mapNotNullTo(ArrayList()) {
                                                    if (activated.contains(it.app.packageName)) it.app.packageName else null
                                                }
                                            navigator.navigate(
                                                SelectAppsScreenDestination(
                                                    true,
                                                    initialSelected
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                            DropdownImpl(
                                text = stringResource(R.string.manage_optimize),
                                optionSize = 3,
                                isSelected = false,
                                index = 1,
                                onSelectedIndexChange = {
                                    showPopup.value = false
                                    scope.launch {
                                        if (!ShizukuApi.isPermissionGranted) {
                                            snackbarHost.showSnackbar(shizukuUnavailable)
                                        } else {
                                            viewModel.dispatch(
                                                AppManageViewModel.ViewAction.PerformOptimize(
                                                    item.first
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                            val uninstallSuccessfully =
                                stringResource(R.string.manage_uninstall_successfully)
                            val launcher =
                                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                                    if (result.resultCode == Activity.RESULT_OK) {
                                        scope.launch {
                                            snackbarHost.showSnackbar(uninstallSuccessfully)
                                        }
                                    }
                                }
                            DropdownImpl(
                                text = stringResource(R.string.uninstall),
                                optionSize = 3,
                                isSelected = false,
                                index = 2,
                                onSelectedIndexChange = {
                                    showPopup.value = false
                                    val intent = Intent(Intent.ACTION_DELETE).apply {
                                        data = "package:${item.first.app.packageName}".toUri()
                                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                    }
                                    launcher.launch(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppManageFab(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var shouldSelectDirectory = remember { mutableStateOf(false) }
    var showNewPatchDialog = remember { mutableStateOf(false) }

    val errorText = stringResource(R.string.patch_select_dir_error)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                if (it.resultCode == Activity.RESULT_CANCELED) return@rememberLauncherForActivityResult
                val uri = it.data?.data ?: throw IOException("No data")
                val takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                Configs.storageDirectory = uri.toString()
                Log.i(TAG, "Storage directory: ${uri.path}")
                showNewPatchDialog.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error when requesting saving directory", e)
                scope.launch { snackbarHost.showSnackbar(errorText) }
            }
        }

    if (shouldSelectDirectory.value) {
        SuperDialog(
            show = shouldSelectDirectory,
            onDismissRequest = { shouldSelectDirectory.value = false },
            title = stringResource(R.string.patch_select_dir_title),
            summary = stringResource(R.string.patch_select_dir_text)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(android.R.string.cancel),
                    onClick = { shouldSelectDirectory.value = false }
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(android.R.string.ok),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        launcher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                        shouldSelectDirectory.value = false
                    }
                )
            }
        }
    }

    if (showNewPatchDialog.value) {
        SuperDialog(
            show = showNewPatchDialog,
            onDismissRequest = { showNewPatchDialog.value = false },
            title = stringResource(R.string.screen_new_patch),
            insideMargin = DpSize(0.dp, 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicComponent(
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.patch_from_storage),
                    onClick = {
                        navigator.navigate(NewPatchScreenDestination(id = ACTION_STORAGE))
                        showNewPatchDialog.value = false
                    }
                )
                BasicComponent(
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.patch_from_applist),
                    onClick = {
                        navigator.navigate(NewPatchScreenDestination(id = ACTION_APPLIST))
                        showNewPatchDialog.value = false
                    }
                )
            }
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                text = stringResource(android.R.string.cancel),
                onClick = { showNewPatchDialog.value = false }
            )
        }
    }

    FloatingActionButton(
        onClick = {
            val uri = Configs.storageDirectory?.toUri()
            if (uri == null) {
                shouldSelectDirectory.value = true
            } else {
                runCatching {
                    val takeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    if (DocumentFile.fromTreeUri(context, uri)
                            ?.exists() == false
                    ) throw IOException("Storage directory was deleted")
                }.onSuccess {
                    showNewPatchDialog.value = true
                }.onFailure {
                    Log.w(TAG, "Failed to take persistable permission for saved uri", it)
                    Configs.storageDirectory = null
                    shouldSelectDirectory.value = true
                }
            }
        }
    ) {
        Icon(
            imageVector = MiuixIcons.Useful.New,
            contentDescription = stringResource(R.string.screen_new_patch),
            tint = MiuixTheme.colorScheme.onPrimaryContainer,
        )
    }
}
