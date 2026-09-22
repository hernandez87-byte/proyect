package com.hmuniformes.multiml;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;

public class ProvisioningModeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent() == null ? null : getIntent().getAction();

        if (DevicePolicyManager.ACTION_GET_PROVISIONING_MODE.equals(action)) {
            Intent result = new Intent();
            result.putExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                    DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
            );
            setResult(RESULT_OK, result);
        } else if (DevicePolicyManager.ACTION_ADMIN_POLICY_COMPLIANCE.equals(action)) {
            setResult(RESULT_OK);
            Intent launch = new Intent(this, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launch);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
