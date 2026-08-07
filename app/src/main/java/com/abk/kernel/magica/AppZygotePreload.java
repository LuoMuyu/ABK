package com.abk.kernel.magica;

import android.app.ZygotePreload;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class AppZygotePreload implements ZygotePreload {
    private static final String TAG = "ABKMagica";
    private static final String KSUD_LIBRARY_NAME = "libksud.so";
    private static final String REQUEST_RELATIVE_PATH = "files/magica-jailbreak/request.properties";
    private static final String REQUEST_MODULE_PATH = "modulePath";

    private static native void forkDontCareAndExecKsud(
            String ksudPath,
            String packageName,
            String modulePath
    );

    @Override
    public void doPreload(@NonNull ApplicationInfo appInfo) {
        File ksud = new File(appInfo.nativeLibraryDir, KSUD_LIBRARY_NAME);
        File request = new File(appInfo.dataDir, REQUEST_RELATIVE_PATH);
        try {
            if (!ksud.isFile()) {
                throw new IllegalStateException("ksud does not exist: " + ksud);
            }
            String modulePath = readModulePath(request);
            if (!request.delete()) {
                Log.w(TAG, "failed to delete consumed request: " + request);
            }
            System.loadLibrary("abkksu");
            Log.d(TAG, "executing magica with external module: " + modulePath);
            forkDontCareAndExecKsud(ksud.getAbsolutePath(), appInfo.packageName, modulePath);
        } catch (Throwable error) {
            Log.e(TAG, "failed to start magica late-load", error);
        }
    }

    private static String readModulePath(File request) throws Exception {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(request)) {
            properties.load(input);
        }
        String modulePath = properties.getProperty(REQUEST_MODULE_PATH, "").trim();
        if (modulePath.isEmpty()) {
            throw new IllegalStateException("missing " + REQUEST_MODULE_PATH);
        }
        if (!new File(modulePath).isFile()) {
            throw new IllegalStateException("module does not exist: " + modulePath);
        }
        return modulePath;
    }
}
