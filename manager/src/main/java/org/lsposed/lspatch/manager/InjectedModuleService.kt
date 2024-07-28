package org.lsposed.lspatch.manager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import org.lsposed.lspd.service.ILSPInjectedModuleService
import org.lsposed.lspd.service.IRemotePreferenceCallback

class InjectedModuleService() : ILSPInjectedModuleService.Stub() {

    override fun getFrameworkPrivilege(): Int {
        return 2
    }

    override fun requestRemotePreferences(group: String?, callback: IRemotePreferenceCallback?): Bundle {
        TODO("Not yet implemented")
    }

    override fun openRemoteFile(path: String?): ParcelFileDescriptor {
        TODO("Not yet implemented")
    }

    override fun getRemoteFileList(): Array<String> {
        TODO("Not yet implemented")
    }
}
