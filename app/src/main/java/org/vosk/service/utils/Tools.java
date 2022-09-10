package org.vosk.service.utils;

import android.app.ActivityManager;
import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.vosk.service.download.DownloadModelService;

import java.io.File;

public class Tools {

    public static File getModelFileRootPath(@NotNull Context context) {
        return new File(context.getFilesDir(), "models");
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DownloadModelService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
