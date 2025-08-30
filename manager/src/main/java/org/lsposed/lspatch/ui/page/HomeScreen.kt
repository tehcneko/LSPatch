package org.lsposed.lspatch.ui.page

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.ramcosta.composedestinations.generated.destinations.NewPatchScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.lsposed.lspatch.R
import org.lsposed.lspatch.share.LSPConfig
import org.lsposed.lspatch.util.ShizukuApi
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.getWindowSize
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomeScreen(
    pagerState: PagerState,
    navigator: DestinationsNavigator,
    bottomInnerPadding: Dp
) {
    // Install from intent
    var isIntentLaunched by rememberSaveable { mutableStateOf(false) }

    LocalActivity.current?.let {
        val intent = it.intent
        LaunchedEffect(Unit) {
            if (!isIntentLaunched && intent.action == Intent.ACTION_VIEW && intent.hasCategory(
                    Intent.CATEGORY_DEFAULT
                ) && intent.type == "application/vnd.android.package-archive"
            ) {
                isIntentLaunched = true
                val uri = intent.data
                if (uri != null) {
                    pagerState.animateScrollToPage(1)
                    navigator.navigate(
                        NewPatchScreenDestination(
                            id = ACTION_INTENT_INSTALL,
                            data = uri
                        )
                    )
                }
            }
        }
    }

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
            TopAppBar(
                title = stringResource(R.string.app_name),
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
                    ShizukuCard()
                    InfoCard()
                    SupportCard()
                }
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }
}

private val listener: (Int, Int) -> Unit = { _, grantResult ->
    ShizukuApi.isPermissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun ShizukuCard() {
    LaunchedEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(listener)
    }
    DisposableEffect(Unit) {
        onDispose {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    if (ShizukuApi.isPermissionGranted) {
        Card(
            modifier = Modifier
                .fillMaxHeight(),
            colors = CardDefaults.defaultColors(
                color = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFAE4)
            ),
            onClick = {
                if (ShizukuApi.isBinderAvailable && !ShizukuApi.isPermissionGranted) {
                    Shizuku.requestPermission(114514)
                }
            },
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(38.dp, 45.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        tint = Color(0xFF36D167),
                        contentDescription = null
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 16.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.shizuku_available),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "API " + Shizuku.getVersion(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    } else {
        Card(
            showIndication = true,
        ) {
            BasicComponent(
                title = stringResource(R.string.shizuku_unavailable),
                summary = stringResource(R.string.home_shizuku_warning),
                leftAction = {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        stringResource(R.string.shizuku_unavailable),
                        modifier = Modifier
                            .padding(end = 16.dp),
                        tint = colorScheme.onBackground,
                    )
                }
            )
        }
    }

}

private val apiVersion = if (Build.VERSION.PREVIEW_SDK_INT != 0) {
    "${Build.VERSION.CODENAME} Preview (API ${Build.VERSION.PREVIEW_SDK_INT})"
} else {
    "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}

private val device = buildString {
    append(Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1))
    if (Build.BRAND != Build.MANUFACTURER) {
        append(" " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1))
    }
    append(" " + Build.MODEL + " ")
}

@Composable
private fun InfoCard() {
    @Composable
    fun InfoText(
        title: String,
        content: String,
        bottomPadding: Dp = 24.dp
    ) {
        Text(
            text = title,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface
        )
        Text(
            text = content,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding)
        )
    }
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            InfoText(
                title = stringResource(R.string.home_api_version),
                content = "${LSPConfig.instance.API_CODE}"
            )
            InfoText(
                title = stringResource(R.string.home_lspatch_version),
                content = LSPConfig.instance.VERSION_NAME + " (${LSPConfig.instance.VERSION_CODE})"
            )
            InfoText(
                title = stringResource(R.string.home_framework_version),
                content = LSPConfig.instance.CORE_VERSION_NAME + " (${LSPConfig.instance.CORE_VERSION_CODE})"
            )
            InfoText(
                title = stringResource(R.string.home_system_version),
                content = apiVersion
            )
            InfoText(
                title = stringResource(R.string.home_device),
                content = device
            )
            InfoText(
                title = stringResource(R.string.home_system_abi),
                content = Build.SUPPORTED_ABIS[0],
                bottomPadding = 0.dp
            )
        }
    }
}

@Preview
@Composable
private fun SupportCard() {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        BasicComponent(
            title = stringResource(R.string.home_support),
            summary = stringResource(R.string.home_description),
            insideMargin = PaddingValues(18.dp),
            rightActions = {
                Icon(
                    modifier = Modifier.size(28.dp),
                    imageVector = Icons.Rounded.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = {
                uriHandler.openUri("https://github.com/LSPosed/LSPatch")
            }
        )
    }
}
