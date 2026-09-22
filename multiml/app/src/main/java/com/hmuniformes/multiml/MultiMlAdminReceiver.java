package com.hmuniformes.multiml;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.Collections;

public class MultiMlAdminReceiver extends DeviceAdminReceiver {
    public static final String AFFILIATION_ID = "multiml-device-group-v1";
    public static final String EXTRA_AFFILIATION = "multiml.affiliation";

    public static ComponentName componentName(Context context) {
        return new ComponentName(context, MultiMlAdminReceiver.class);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);

        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = componentName(context);

        String affiliation = intent == null ? null : intent.getStringExtra(EXTRA_AFFILIATION);
        if (affiliation != null && dpm.isProfileOwnerApp(context.getPackageName())) {
            try {
                dpm.setAffiliationIds(admin, Collections.singleton(affiliation));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = componentName(context);

        try {
            if (dpm.isProfileOwnerApp(context.getPackageName())) {
                dpm.setProfileName(admin, "Multi ML");
                dpm.setProfileEnabled(admin);
            }
        } catch (Exception ignored) {
        }

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
    }
}
