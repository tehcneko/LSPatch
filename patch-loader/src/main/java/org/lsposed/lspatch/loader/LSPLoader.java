package org.lsposed.lspatch.loader;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.content.res.XResources;
import android.os.Build;

import org.lsposed.lspd.impl.LSPosedContext;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedModuleInterface;

@SuppressLint("BlockedPrivateApi")
public class LSPLoader {
    private final static Field defaultClassLoaderField;

    static {
        Field field = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                field = LoadedApk.class.getDeclaredField("mDefaultClassLoader");
                field.setAccessible(true);
            } catch (Throwable ignored) {
            }
        }
        defaultClassLoaderField = field;
    }

    public static void initModules(LoadedApk loadedApk) {
        XposedInit.loadedPackagesInProcess.add(loadedApk.getPackageName());
        XResources.setPackageNameForResDir(loadedApk.getPackageName(), loadedApk.getResDir());
        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                XposedBridge.sLoadedPackageCallbacks);
        lpparam.packageName = loadedApk.getPackageName();
        lpparam.processName = ActivityThread.currentProcessName();
        lpparam.classLoader = loadedApk.getClassLoader();
        lpparam.appInfo = loadedApk.getApplicationInfo();
        lpparam.isFirstApplication = true;
        XC_LoadPackage.callAll(lpparam);

        LSPosedContext.callOnPackageLoaded(new XposedModuleInterface.PackageLoadedParam() {
            @Override
            public String getPackageName() {
                return loadedApk.getPackageName();
            }

            @Override
            public ApplicationInfo getApplicationInfo() {
                return loadedApk.getApplicationInfo();
            }

            @Override
            public ClassLoader getDefaultClassLoader() {
                try {
                    return (ClassLoader) defaultClassLoaderField.get(loadedApk);
                } catch (Throwable t) {
                    throw new IllegalStateException(t);
                }
            }

            @Override
            public ClassLoader getClassLoader() {
                return loadedApk.getClassLoader();
            }

            @Override
            public boolean isFirstPackage() {
                return true;
            }
        });
    }
}
