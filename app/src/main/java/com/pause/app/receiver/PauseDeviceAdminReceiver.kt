package com.pause.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin receiver that prevents uninstalling Focus when Parental Control is active.
 * Parent must explicitly enable this during setup. Can be disabled via PIN entry.
 */
class PauseDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // ParentalControlManager will be notified via callback - wired in Phase 2
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // ParentalControlManager will update config - wired in Phase 2
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling Device Admin will allow Focus to be uninstalled. " +
            "Parental controls will no longer prevent uninstallation."
    }
}
