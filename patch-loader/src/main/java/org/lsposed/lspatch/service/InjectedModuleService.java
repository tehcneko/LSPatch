package org.lsposed.lspatch.service;

import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import org.lsposed.lspd.service.ILSPInjectedModuleService;
import org.lsposed.lspd.service.IRemotePreferenceCallback;

public class InjectedModuleService extends ILSPInjectedModuleService.Stub {
    @Override
    public int getFrameworkPrivilege() throws RemoteException {
        return 3;
    }

    @Override
    public Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback) throws RemoteException {
        return null;
    }

    @Override
    public ParcelFileDescriptor openRemoteFile(String path) throws RemoteException {
        return null;
    }

    @Override
    public String[] getRemoteFileList() throws RemoteException {
        return new String[0];
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
