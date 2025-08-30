package org.lsposed.lspatch.ui.page

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.lsposed.lspatch.R
import org.lsposed.lspatch.config.Configs
import org.lsposed.lspatch.config.MyKeyStore
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.getWindowSize
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

@Composable
fun SettingsScreen(
    bottomInnerPadding: Dp
) {
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
                title = stringResource(R.string.screen_settings),
                scrollBehavior = scrollBehavior,
                modifier = Modifier
                    .hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = 25.dp
                        noiseFactor = 0f
                    }
            )
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .height(getWindowSize().height.dp)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp)
                .hazeSource(hazeState),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card {
                        KeyStore()
                        DetailPatchLogs()
                    }
                }
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }
}

@Composable
private fun KeyStore() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog = remember { mutableStateOf(false) }

    val options = listOf(
        stringResource(R.string.settings_keystore_default),
        stringResource(R.string.settings_keystore_custom)
    )

    SuperDropdown(
        title = stringResource(R.string.settings_keystore),
        items = options,
        selectedIndex = if (MyKeyStore.useDefault) 0 else 1,
        onSelectedIndexChange = {
            if (it == 0) {
                scope.launch { MyKeyStore.reset() }
            } else {
                showDialog.value = true
            }
        }
    )

    if (showDialog.value) {
        var wrongKeystore by rememberSaveable { mutableStateOf(false) }
        var wrongPassword by rememberSaveable { mutableStateOf(false) }
        var wrongAliasName by rememberSaveable { mutableStateOf(false) }
        var wrongAliasPassword by rememberSaveable { mutableStateOf(false) }

        var path by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var alias by rememberSaveable { mutableStateOf("") }
        var aliasPassword by rememberSaveable { mutableStateOf("") }

        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                context.contentResolver.openInputStream(uri).use { input ->
                    MyKeyStore.tmpFile.outputStream().use { output ->
                        input?.copyTo(output)
                    }
                }
                path = uri.path ?: ""
            }
        val errorColor = Color.Red.copy(0.3f)
        SuperDialog(
            show = showDialog,
            onDismissRequest = { showDialog.value = false },
            title = stringResource(R.string.settings_keystore_dialog_title),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            launcher.launch("*/*")
                        }
                    }
                }

                val wrongText = when {
                    wrongAliasPassword -> stringResource(R.string.settings_keystore_wrong_alias_password)
                    wrongAliasName -> stringResource(R.string.settings_keystore_wrong_alias)
                    wrongPassword -> stringResource(R.string.settings_keystore_wrong_password)
                    wrongKeystore -> stringResource(R.string.settings_keystore_wrong_keystore)
                    else -> null
                }
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = wrongText ?: stringResource(R.string.settings_keystore_desc),
                    color = if (wrongText != null) errorColor else Color.Unspecified
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    TextField(
                        value = path,
                        onValueChange = {
                            path = it
                        },
                        readOnly = true,
                        label = stringResource(R.string.settings_keystore_file),
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        labelColor = if (wrongKeystore) errorColor else MiuixTheme.colorScheme.onSecondaryContainer,
                        interactionSource = interactionSource,
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.settings_keystore_password),
                        singleLine = true,
                        labelColor = if (wrongPassword) errorColor else MiuixTheme.colorScheme.onSecondaryContainer,
                    )
                    TextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = stringResource(R.string.settings_keystore_alias),
                        singleLine = true,
                        labelColor = if (wrongAliasName) errorColor else MiuixTheme.colorScheme.onSecondaryContainer,
                    )
                    TextField(
                        value = aliasPassword,
                        onValueChange = { aliasPassword = it },
                        label = stringResource(R.string.settings_keystore_alias_password),
                        singleLine = true,
                        labelColor = if (wrongAliasPassword) errorColor else MiuixTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(android.R.string.cancel),
                    onClick = { showDialog.value = false }
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(android.R.string.ok),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        wrongKeystore = false
                        wrongPassword = false
                        wrongAliasName = false
                        wrongAliasPassword = false

                        if (path.isEmpty()) {
                            wrongKeystore = true
                            return@TextButton
                        }
                        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                        try {
                            MyKeyStore.tmpFile.inputStream().use { input ->
                                keyStore.load(input, password.toCharArray())
                            }
                        } catch (e: IOException) {
                            wrongKeystore = true
                            if (e.message == "KeyStore integrity check failed.") {
                                wrongPassword = true
                            }
                            return@TextButton
                        }
                        if (!keyStore.containsAlias(alias)) {
                            wrongAliasName = true
                            return@TextButton
                        }
                        try {
                            keyStore.getKey(alias, aliasPassword.toCharArray())
                        } catch (e: GeneralSecurityException) {
                            wrongAliasPassword = true
                            return@TextButton
                        }

                        scope.launch { MyKeyStore.setCustom(password, alias, aliasPassword) }
                        showDialog.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailPatchLogs() {
    SuperSwitch(
        checked = Configs.detailPatchLogs,
        onCheckedChange = {
            Configs.detailPatchLogs = it
        },
        title = stringResource(R.string.settings_detail_patch_logs),
    )
}
