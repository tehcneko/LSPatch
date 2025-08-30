package org.lsposed.lspatch.ui.page

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.lsposed.lspatch.R

enum class BottomBarDestination(
    @StringRes val label: Int,
    val iconSelected: ImageVector,
) {
    Home(R.string.app_name, Icons.Rounded.Home),
    Manage(R.string.screen_manage, Icons.Rounded.Dashboard),
    Settings(R.string.screen_settings, Icons.Rounded.Settings);
}
