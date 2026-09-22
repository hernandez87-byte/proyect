package com.hmuniformes.multiml;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class MultiMlAdminReceiver extends DeviceAdminReceiver {
    public static ComponentName componentName(Context context) {
        return new ComponentName(context, MultiMlAdminReceiver.class);
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = componentName(context);
        dpm.setProfileName(admin, "Multi ML");
        dpm.setProfileEnabled(admin);

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
    }
}
