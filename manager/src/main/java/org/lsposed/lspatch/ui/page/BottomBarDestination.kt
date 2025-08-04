package org.lsposed.lspatch.ui.page

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ManageScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import org.lsposed.lspatch.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.NavigatorSwitch
import top.yukonga.miuix.kmp.icon.icons.useful.Personal
import top.yukonga.miuix.kmp.icon.icons.useful.Settings

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    @StringRes val label: Int,
    val iconSelected: ImageVector,
) {
    Manage(ManageScreenDestination, R.string.screen_manage, MiuixIcons.Useful.Personal),
    Home(HomeScreenDestination, R.string.app_name, MiuixIcons.Useful.NavigatorSwitch),
    Settings(SettingsScreenDestination, R.string.screen_settings, MiuixIcons.Useful.Settings);
}
