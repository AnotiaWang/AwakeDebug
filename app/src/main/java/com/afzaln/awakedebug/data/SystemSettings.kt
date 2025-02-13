package com.afzaln.awakedebug.data

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.afzaln.awakedebug.AwakeDebugApp
import com.afzaln.awakedebug.NotificationListener
import com.afzaln.awakedebug.R
import timber.log.Timber

const val MAX_TIMEOUT = 30 * 60 * 1000
/**
 * Responsible for checking system permissions
 * and modifying the display timeout system setting
 */
class SystemSettings(
    val app: AwakeDebugApp,
    private val prefs: Prefs
) {
    private var toast: Toast = Toast.makeText(app, "", Toast.LENGTH_SHORT)
    val hasNotificationPermission: Boolean
        get() {
            val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationListenerComponent = ComponentName(app, NotificationListener::class.java)
            return notificationManager.isNotificationListenerAccessGranted(notificationListenerComponent)
        }

    val hasModifyPermission: Boolean
        get() {
            return Settings.System.canWrite(app)
        }

    val hasAllPermissions: Boolean
        get() = hasNotificationPermission && hasModifyPermission

    val screenTimeoutLiveData = MutableLiveData(screenOffTimeout)

    private val screenOffTimeout: Int
        get() {
            try {
                return Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            } catch (e: SettingNotFoundException) {
                Timber.e(e)
            }
            return 0
        }

    fun refreshScreenTimeout() {
        if (hasModifyPermission) {
            screenTimeoutLiveData.value = screenOffTimeout
            Timber.d("Refreshed screen timeout to ${screenTimeoutLiveData.value}")
        }
    }

    internal fun extendScreenTimeout() {
        if (hasModifyPermission) {
            prefs.savedTimeout = screenOffTimeout
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, MAX_TIMEOUT)
            Timber.d("Screen timeout is now $MAX_TIMEOUT")
            screenTimeoutLiveData.value = MAX_TIMEOUT
            toast.cancel()
            toast = Toast.makeText(app, R.string.toast_settings_modified, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    internal fun restoreScreenTimeout() {
        if (hasModifyPermission) {
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, prefs.savedTimeout)
            Timber.d("Screen timeout is now ${prefs.savedTimeout}")
            screenTimeoutLiveData.value = prefs.savedTimeout
            toast.cancel()
            toast = Toast.makeText(app, R.string.toast_settings_fallback, Toast.LENGTH_SHORT)
            toast.show()
        }
    }
}
