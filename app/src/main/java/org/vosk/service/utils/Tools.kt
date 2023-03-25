package org.vosk.service.utils

import android.app.ActivityManager
import android.content.Context
import org.vosk.service.download.DownloadModelService
import java.io.File

object Tools {
	@JvmStatic
    fun getModelFileRootPath(context: Context): File {
		return File(context.filesDir, "models")
	}

	@JvmStatic
    fun isServiceRunning(context: Context): Boolean {
		val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		for (service in manager.getRunningServices(Int.MAX_VALUE)) {
			if (DownloadModelService::class.java.name == service.service.className) {
				return true
			}
		}
		return false
	}
}