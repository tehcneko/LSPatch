package org.lsposed.lspatch.ui.viewmodel.manage

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import org.lsposed.lspatch.util.LSPPackageManager

class ModuleManageViewModel : ViewModel() {

    companion object {
        private const val TAG = "ModuleManageViewModel"
    }

    class XposedInfo(
        val api: Int,
        val description: String,
        val scope: List<String>
    )

    val appList: List<Pair<LSPPackageManager.AppInfo, XposedInfo>> by derivedStateOf {
        LSPPackageManager.appList.mapNotNull { appInfo ->
            if (appInfo.minVersion < 0) return@mapNotNull null
            appInfo to XposedInfo(
                appInfo.minVersion,
                    appInfo.description,
                emptyList() // TODO: scope
            )
        }.also {
            Log.d(TAG, "Loaded ${it.size} Xposed modules")
        }
    }
}
