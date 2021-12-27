package org.vosk.service.utils;

import android.app.ActivityManager;
import android.content.Context;

import org.vosk.service.download.DownloadModelService;

public class Tools {

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
