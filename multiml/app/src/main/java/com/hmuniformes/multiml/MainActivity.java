package com.hmuniformes.multiml;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.content.pm.CrossProfileApps;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_PROFILE = 401;
    private static final String ML = "com.mercadolibre";
    private static final String MP = "com.mercadopago.wallet";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        boolean managed = ((UserManager) getSystemService(USER_SERVICE)).isManagedProfile();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(9, 14, 26));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addText(root, managed ? "ESPACIO AISLADO" : "PERFIL PERSONAL", 12, Color.rgb(87,199,255), true);
        addText(root, "Multi ML", 34, Color.WHITE, true);
        addText(root,
                managed ? "Sesión y datos independientes." :
                        "Crea una segunda instalación mediante el perfil de trabajo de Android.",
                16, Color.rgb(148,163,184), false);

        if (managed) {
            appButton(root, "Mercado Libre", ML);
            appButton(root, "Mercado Pago", MP);

            List<UserHandle> targets = targets();
            if (!targets.isEmpty()) {
                action(root, "Volver al perfil personal", () -> openProfile(targets.get(0)));
            }

            DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (dpm.isProfileOwnerApp(getPackageName())) {
                action(root, "Eliminar espacio Multi ML", this::confirmRemove);
            }
        } else {
            appButton(root, "Mercado Libre normal", ML);
            appButton(root, "Mercado Pago normal", MP);

            List<UserHandle> targets = targets();
            if (targets.isEmpty()) {
                action(root, "Crear espacio Multi ML", this::provision);
            } else {
                action(root, "Abrir espacio Multi ML", () -> openProfile(targets.get(0)));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                CrossProfileApps cpa = getSystemService(CrossProfileApps.class);
                if (cpa != null && cpa.canRequestInteractAcrossProfiles()
                        && !cpa.canInteractAcrossProfiles()) {
                    action(root, "Permitir cambio entre perfiles", () -> {
                        try {
                            startActivity(cpa.createRequestInteractAcrossProfilesIntent());
                        } catch (Exception e) {
                            toast("Android no permitió abrir ese ajuste.");
                        }
                    });
                }
            }
        }

        addText(root,
                "Multi ML no modifica las apps oficiales y no guarda contraseñas.",
                13, Color.rgb(148,163,184), false);
        setContentView(scroll);
    }

    private void appButton(LinearLayout root, String title, String pkg) {
        boolean installed = installed(pkg);
        action(root, title + (installed ? " · ABRIR" : " · INSTALAR"), () -> {
            if (installed(pkg)) launch(pkg); else store(pkg);
        });
    }

    private void provision() {
        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)) {
            message("No compatible", "Este teléfono no anuncia soporte para perfiles de trabajo.");
            return;
        }

        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            message("No disponible",
                    "Android no permite crear el perfil. Puede existir ya un perfil de trabajo o el fabricante puede restringirlo.");
            return;
        }

        Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        intent.putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                MultiMlAdminReceiver.componentName(this));

        try {
            startActivityForResult(intent, REQUEST_PROFILE);
        } catch (ActivityNotFoundException e) {
            message("No disponible", "El sistema no tiene el aprovisionador de perfiles.");
        }
    }

    private List<UserHandle> targets() {
        CrossProfileApps cpa = getSystemService(CrossProfileApps.class);
        return cpa == null ? Collections.emptyList() : cpa.getTargetUserProfiles();
    }

    private void openProfile(UserHandle user) {
        CrossProfileApps cpa = getSystemService(CrossProfileApps.class);
        if (cpa == null) return;
        try {
            cpa.startMainActivity(new ComponentName(this, MainActivity.class), user);
        } catch (Exception e) {
            toast("No pude cambiar de perfil. Activa el permiso entre perfiles.");
        }
    }

    private boolean installed(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void launch(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i); else store(pkg);
    }

    private void store(String pkg) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
            i.setPackage("com.android.vending");
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }

    private void confirmRemove() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar espacio Multi ML")
                .setMessage("Se borrarán las apps y sesiones de este perfil aislado.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, w) -> {
                    DevicePolicyManager dpm =
                            (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                    dpm.wipeData(DevicePolicyManager.WIPE_SILENTLY);
                }).show();
    }

    private void action(LinearLayout root, String title, Runnable run) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        lp.topMargin = dp(14);
        root.addView(b, lp);
    }

    private void addText(LinearLayout root, String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(0, dp(6), 0, dp(6));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }

    private void message(String title, String body) {
        new AlertDialog.Builder(this)
                .setTitle(title).setMessage(body)
                .setPositiveButton("Entendido", null).show();
    }
}
