package org.lsposed.lspatch.ui.page

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SelectAppsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lsposed.lspatch.R
import org.lsposed.lspatch.lspApp
import org.lsposed.lspatch.ui.activity.LocalBottomBarHeight
import org.lsposed.lspatch.ui.activity.LocalBottomHazeState
import org.lsposed.lspatch.ui.component.LoadingDialog
import org.lsposed.lspatch.ui.component.ShimmerAnimation
import org.lsposed.lspatch.ui.util.LocalSnackbarHost
import org.lsposed.lspatch.ui.util.isScrolledToEnd
import org.lsposed.lspatch.ui.util.lastItemIndex
import org.lsposed.lspatch.ui.viewmodel.NewPatchViewModel
import org.lsposed.lspatch.ui.viewmodel.NewPatchViewModel.PatchState
import org.lsposed.lspatch.ui.viewmodel.NewPatchViewModel.ViewAction
import org.lsposed.lspatch.util.LSPPackageManager
import org.lsposed.lspatch.util.LSPPackageManager.AppInfo
import org.lsposed.lspatch.util.ShizukuApi
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SpinnerEntry
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSpinner
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SmoothRoundedCornerShape
import top.yukonga.miuix.kmp.utils.overScrollVertical

private const val TAG = "NewPatchPage"

const val ACTION_STORAGE = 0
const val ACTION_APPLIST = 1
const val ACTION_INTENT_INSTALL = 2

@Destination<RootGraph>
@Composable
fun NewPatchScreen(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<SelectAppsScreenDestination, SelectAppsResult>,
    id: Int,
    data: Uri? = null,
) {
    val viewModel = viewModel<NewPatchViewModel>()
    val snackbarHost = LocalSnackbarHost.current
    val errorUnknown = stringResource(R.string.error_unknown)
    val storageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { apks ->
            if (apks.isEmpty()) {
                navigator.navigateUp()
                return@rememberLauncherForActivityResult
            }
            runBlocking {
                LSPPackageManager.getAppInfoFromApks(apks)
                    .onSuccess {
                        viewModel.dispatch(ViewAction.ConfigurePatch(it.first()))
                    }
                    .onFailure {
                        lspApp.globalScope.launch {
                            snackbarHost.showSnackbar(
                                it.message ?: errorUnknown
                            )
                        }
                        navigator.navigateUp()
                    }
            }
        }

    var showSelectModuleDialog = remember { mutableStateOf(false) }
    val noXposedModules = stringResource(R.string.patch_no_xposed_module)
    val storageModuleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { apks ->
            if (apks.isEmpty()) {
                return@rememberLauncherForActivityResult
            }
            runBlocking {
                LSPPackageManager.getAppInfoFromApks(apks).onSuccess { it ->
                    viewModel.embeddedModules = it.filter { it.isXposedModule }.ifEmpty {
                        lspApp.globalScope.launch {
                            snackbarHost.showSnackbar(noXposedModules)
                        }
                        return@onSuccess
                    }
                }.onFailure {
                    lspApp.globalScope.launch {
                        snackbarHost.showSnackbar(
                            it.message ?: errorUnknown
                        )
                    }
                }
            }
        }

    Log.d(TAG, "PatchState: ${viewModel.patchState}")
    when (viewModel.patchState) {
        PatchState.INIT -> {
            LaunchedEffect(Unit) {
                LSPPackageManager.cleanTmpApkDir()
                when (id) {
                    ACTION_STORAGE -> {
                        storageLauncher.launch(arrayOf("application/vnd.android.package-archive"))
                        viewModel.dispatch(ViewAction.DoneInit)
                    }

                    ACTION_APPLIST -> {
                        navigator.navigate(SelectAppsScreenDestination(false))
                        viewModel.dispatch(ViewAction.DoneInit)
                    }

                    ACTION_INTENT_INSTALL -> {
                        runBlocking {
                            data?.let { uri ->
                                LSPPackageManager.getAppInfoFromApks(listOf(uri)).onSuccess {
                                    viewModel.dispatch(ViewAction.ConfigurePatch(it.first()))
                                }.onFailure {
                                    lspApp.globalScope.launch {
                                        snackbarHost.showSnackbar(
                                            it.message ?: errorUnknown
                                        )
                                    }
                                    navigator.navigateUp()
                                }
                            }
                        }
                    }
                }
            }
        }

        PatchState.SELECTING -> {
            resultRecipient.onNavResult {
                Log.d(TAG, "onNavResult: $it")
                when (it) {
                    is NavResult.Canceled -> navigator.navigateUp()
                    is NavResult.Value -> {
                        val result = it.value as SelectAppsResult.SingleApp
                        viewModel.dispatch(ViewAction.ConfigurePatch(result.selected))
                    }
                }
            }
        }

        else -> {
            if (viewModel.patchState == PatchState.CONFIGURING) {
                PatchOptionsBody(
                    navigator,
                    onAddEmbed = {
                        showSelectModuleDialog.value = true
                    }
                )
                resultRecipient.onNavResult {
                    if (it is NavResult.Value) {
                        val result = it.value as SelectAppsResult.MultipleApps
                        viewModel.embeddedModules = result.selected
                    }
                }
            } else {
                DoPatchBody(navigator)
            }

            if (showSelectModuleDialog.value) {
                SuperDialog(
                    show = showSelectModuleDialog,
                    onDismissRequest = { showSelectModuleDialog.value = false },
                    title = stringResource(R.string.patch_embed_modules),
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
                                storageModuleLauncher.launch(arrayOf("application/vnd.android.package-archive"))
                                showSelectModuleDialog.value = false
                            }
                        )
                        BasicComponent(
                            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.patch_from_applist),
                            onClick = {
                                navigator.navigate(
                                    SelectAppsScreenDestination(
                                        true,
                                        viewModel.embeddedModules.mapTo(ArrayList()) { it.app.packageName })
                                )
                                showSelectModuleDialog.value = false
                            }
                        )
                    }
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        text = stringResource(android.R.string.cancel),
                        onClick = { showSelectModuleDialog.value = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfiguringFab() {
    val viewModel = viewModel<NewPatchViewModel>()
    FloatingActionButton(
        modifier = Modifier.padding(bottom = LocalBottomBarHeight.current.value),
        minWidth = 120.dp,
        shape = RoundedCornerShape(16.dp),
        onClick = { viewModel.dispatch(ViewAction.SubmitPatch) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = MiuixIcons.Useful.Play,
                contentDescription = "Add",
                tint = MiuixTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.patch_start),
                color = MiuixTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun sigBypassLvStr(level: Int) = when (level) {
    0 -> stringResource(R.string.patch_sigbypasslv0)
    1 -> stringResource(R.string.patch_sigbypasslv1)
    2 -> stringResource(R.string.patch_sigbypasslv2)
    else -> throw IllegalArgumentException("Invalid sigBypassLv: $level")
}

@Composable
private fun PatchOptionsBody(
    navigator: DestinationsNavigator,
    onAddEmbed: () -> Unit
) {
    val viewModel = viewModel<NewPatchViewModel>()

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
            TopAppBar(
                title = stringResource(R.string.screen_new_patch),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(start = 20.dp),
                        onClick = { navigator.navigateUp() }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Back,
                            contentDescription = "Back",
                        )
                    }
                },
                modifier = Modifier
                    .hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = 25.dp
                        noiseFactor = 0f
                    },
            )
        },
        floatingActionButton = {
            ConfiguringFab()
        },
        popupHost = {},
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .hazeSource(state = hazeState)
                .hazeSource(state = LocalBottomHazeState.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = LocalBottomBarHeight.current.value + 12.dp,
                start = 12.dp,
                end = 12.dp
            ),
        ) {
            item {
                Card {
                    BasicComponent(
                        title = viewModel.patchApp.label,
                        summary = viewModel.patchApp.app.packageName,
                    )
                }
            }

            item {
                Card {
                    val options = listOf(
                        SpinnerEntry(
                            title = stringResource(R.string.patch_local),
                            summary = stringResource(R.string.patch_local_desc)
                        ),
                        SpinnerEntry(
                            title = stringResource(R.string.patch_integrated),
                            summary = stringResource(R.string.patch_integrated_desc),
                        ),
                    )
                    SuperSpinner(
                        title = stringResource(R.string.patch_mode),
                        items = options,
                        selectedIndex = if (viewModel.useManager) 0 else 1,
                        onSelectedIndexChange = {
                            viewModel.useManager = it == 0
                        }
                    )
                    AnimatedVisibility(
                        visible = !viewModel.useManager,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SuperArrow(
                            title = stringResource(R.string.patch_embed_modules),
                            onClick = onAddEmbed,
                        )
                    }
                }
            }

            item {
                Card {
                    SuperSwitch(
                        checked = viewModel.debuggable,
                        title = stringResource(R.string.patch_debuggable),
                        onCheckedChange = { viewModel.debuggable = it },
                    )

                    SuperSwitch(
                        checked = viewModel.overrideVersionCode,
                        title = stringResource(R.string.patch_override_version_code),
                        summary = stringResource(R.string.patch_override_version_code_desc),
                        onCheckedChange = { viewModel.overrideVersionCode = it }
                    )

                    SuperSwitch(
                        checked = viewModel.injectDex,
                        title = stringResource(R.string.patch_inject_dex),
                        summary = stringResource(R.string.patch_inject_dex_desc),
                        onCheckedChange = { viewModel.injectDex = it }
                    )

                    val options =
                        listOf(sigBypassLvStr(0), sigBypassLvStr(1), sigBypassLvStr(2))
                    SuperDropdown(
                        title = stringResource(R.string.patch_sigbypass),
                        summary = sigBypassLvStr(viewModel.sigBypassLevel),
                        items = options,
                        selectedIndex = viewModel.sigBypassLevel,
                        onSelectedIndexChange = { viewModel.sigBypassLevel = it }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DoPatchBody(
    navigator: DestinationsNavigator,
) {
    val viewModel = viewModel<NewPatchViewModel>()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (viewModel.logs.isEmpty()) {
            viewModel.dispatch(ViewAction.LaunchPatch)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = viewModel.patchApp.label,
                navigationIcon = {
                    if (viewModel.patchState != PatchState.PATCHING) {
                        IconButton(
                            modifier = Modifier.padding(start = 20.dp),
                            onClick = { navigator.navigateUp() }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Useful.Back,
                                contentDescription = "Back",
                            )

                        }
                    }
                },
            )
        },
        popupHost = {},
    ) { innerPadding ->
        @SuppressLint("UnusedBoxWithConstraintsScope")
        BoxWithConstraints(
            Modifier
                .padding(
                    bottom = LocalBottomBarHeight.current.value + 12.dp,
                    start = 12.dp,
                    end = 12.dp
                )
                .padding(innerPadding)
                .hazeSource(state = LocalBottomHazeState.current)
        ) {
            val shellBoxMaxHeight =
                if (viewModel.patchState == PatchState.PATCHING) maxHeight
                else maxHeight - ButtonDefaults.MinHeight - 24.dp
            Column(
                Modifier
                    .fillMaxSize()
                    .wrapContentHeight()
                    .animateContentSize(spring(stiffness = Spring.StiffnessLow))
            ) {
                ShimmerAnimation(enabled = viewModel.patchState == PatchState.PATCHING) {
                    val scrollState = rememberLazyListState()
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = shellBoxMaxHeight)
                            .clip(SmoothRoundedCornerShape(CardDefaults.CornerRadius))
                            .background(brush)
                            .overScrollVertical(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(viewModel.logs) {
                            when (it.first) {
                                Log.DEBUG -> Text(
                                    text = it.second,
                                    style = MiuixTheme.textStyles.body2,
                                    fontFamily = FontFamily.Monospace,
                                )

                                Log.INFO -> Text(
                                    text = it.second,
                                    style = MiuixTheme.textStyles.body2,
                                    fontFamily = FontFamily.Monospace,
                                )

                                Log.ERROR -> Text(
                                    text = it.second,
                                    style = MiuixTheme.textStyles.body2,
                                    color = Color.Red.copy(0.3f),
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }

                    LaunchedEffect(scrollState.lastItemIndex) {
                        if (!scrollState.isScrolledToEnd) {
                            scrollState.animateScrollToItem(scrollState.lastItemIndex!!)
                        }
                    }
                }

                when (viewModel.patchState) {
                    PatchState.PATCHING -> BackHandler {}
                    PatchState.FINISHED -> {
                        val shizukuUnavailable = stringResource(R.string.shizuku_unavailable)
                        val installSuccessfully =
                            stringResource(R.string.patch_install_successfully)
                        val installFailed = stringResource(R.string.patch_install_failed)
                        val copyError = stringResource(R.string.copy_error)
                        var installing by remember { mutableStateOf(false) }
                        if (installing) InstallDialog(viewModel.patchApp) { status, message ->
                            scope.launch {
                                installing = false
                                if (status == PackageInstaller.STATUS_SUCCESS) {
                                    lspApp.globalScope.launch {
                                        snackbarHost.showSnackbar(
                                            installSuccessfully
                                        )
                                    }
                                    navigator.navigateUp()
                                } else if (status != LSPPackageManager.STATUS_USER_CANCELLED) {
                                    val result = snackbarHost.showSnackbar(installFailed, copyError)
                                    if (result == SnackbarResult.ActionPerformed) {
                                        val cm =
                                            lspApp.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("LSPatch", message))
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = { navigator.navigateUp() },
                                text = stringResource(R.string.patch_return)
                            )
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (!ShizukuApi.isPermissionGranted) {
                                        scope.launch {
                                            snackbarHost.showSnackbar(shizukuUnavailable)
                                        }
                                    } else {
                                        installing = true
                                    }
                                },
                                text = stringResource(R.string.install),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }

                    PatchState.ERROR -> {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = { navigator.navigateUp() },
                                text = stringResource(R.string.patch_return)
                            )
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val cm =
                                        lspApp.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "LSPatch",
                                            viewModel.logs.joinToString { it.second + "\n" })
                                    )
                                },
                                text = stringResource(R.string.copy_error),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun InstallDialog(patchApp: AppInfo, onFinish: (Int, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var uninstallFirst = remember {
        mutableStateOf(
            ShizukuApi.isPackageInstalledWithoutPatch(
                patchApp.app.packageName
            )
        )
    }
    val showInstalling = remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(0) }
    suspend fun doInstall() {
        Log.i(TAG, "Installing app ${patchApp.app.packageName}")
        installing = 1
        showInstalling.value = true
        val (status, message) = LSPPackageManager.install()
        installing = 0
        showInstalling.value = false
        Log.i(TAG, "Installation end: $status, $message")
        onFinish(status, message)
    }

    LaunchedEffect(Unit) {
        if (!uninstallFirst.value) {
            doInstall()
        }
    }

    if (uninstallFirst.value) {
        SuperDialog(
            show = uninstallFirst,
            onDismissRequest = {
                onFinish(
                    LSPPackageManager.STATUS_USER_CANCELLED,
                    "User cancelled"
                )
            },
            title = stringResource(R.string.uninstall),
            summary = stringResource(R.string.patch_uninstall_text)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(android.R.string.cancel),
                    onClick = {
                        onFinish(
                            LSPPackageManager.STATUS_USER_CANCELLED,
                            "User cancelled"
                        )
                    }
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(android.R.string.ok),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        scope.launch {
                            Log.i(TAG, "Uninstalling app ${patchApp.app.packageName}")
                            uninstallFirst.value = false
                            installing = 2
                            showInstalling.value = true
                            val (status, message) = LSPPackageManager.uninstall(patchApp.app.packageName)
                            installing = 0
                            showInstalling.value = false
                            Log.i(TAG, "Uninstallation end: $status, $message")
                            if (status == PackageInstaller.STATUS_SUCCESS) {
                                doInstall()
                            } else {
                                onFinish(status, message)
                            }
                        }
                    }
                )
            }
        }
    }

    if (installing != 0) {
        LoadingDialog()
    }
}
