package com.hmuniformes.multiml;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String ML = "com.mercadolibre";
    private static final String MP = "com.mercadopago.wallet";
    private static final int MAX_SECONDARY = 3;
    private static final String PREFS = "multiml_users";

    private DevicePolicyManager dpm;
    private UserManager userManager;
    private ComponentName admin;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        userManager = (UserManager) getSystemService(USER_SERVICE);
        admin = MultiMlAdminReceiver.componentName(this);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        boolean deviceOwner = dpm.isDeviceOwnerApp(getPackageName());
        boolean profileOwner = dpm.isProfileOwnerApp(getPackageName());

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(9, 14, 26));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if (deviceOwner) {
            ensurePrimaryAffiliation();
            renderDeviceOwner(root);
        } else if (profileOwner) {
            renderSecondary(root);
        } else {
            renderNotProvisioned(root);
        }

        setContentView(scroll);
    }

    private void renderDeviceOwner(LinearLayout root) {
        addText(root, "CONTROLADOR DEL DISPOSITIVO", 12, Color.rgb(87,199,255), true);
        addText(root, "Multi ML", 34, Color.WHITE, true);
        addText(root,
                "Administra hasta 3 usuarios adicionales con sesiones totalmente separadas.",
                16, Color.rgb(148,163,184), false);

        appButton(root, "Cuenta 1 · Mercado Libre", ML);
        appButton(root, "Cuenta 1 · Mercado Pago", MP);

        List<UserHandle> users = secondaryUsers();
        addText(root,
                "Espacios adicionales: " + users.size() + " / " + MAX_SECONDARY,
                15, Color.WHITE, true);

        for (int i = 0; i < users.size(); i++) {
            UserHandle user = users.get(i);
            long serial = userManager.getSerialNumberForUser(user);
            String fallback = "Cuenta " + (i + 2);
            String name = prefs.getString("name_" + serial, fallback);

            final UserHandle target = user;
            action(root, name + " · ABRIR", () -> switchTo(target));
            actionSecondary(root, "Eliminar " + name, () -> confirmRemoveUser(target, name));
        }

        if (users.size() < MAX_SECONDARY) {
            action(root, "+ Crear otra cuenta", this::createManagedUser);
        }

        addText(root,
                "Cada cuenta es un usuario Android distinto. Mercado Libre y Mercado Pago conservan datos y sesiones separados.",
                13, Color.rgb(148,163,184), false);
    }

    private void renderSecondary(LinearLayout root) {
        addText(root, "CUENTA AISLADA", 12, Color.rgb(87,199,255), true);
        addText(root, "Multi ML", 34, Color.WHITE, true);
        addText(root,
                "Este usuario tiene almacenamiento, apps y sesiones independientes.",
                16, Color.rgb(148,163,184), false);

        appButton(root, "Mercado Libre", ML);
        appButton(root, "Mercado Pago", MP);

        action(root, "Cambiar de usuario", () -> {
            try {
                Intent users = new Intent("android.settings.USER_SETTINGS");
                if (users.resolveActivity(getPackageManager()) != null) {
                    startActivity(users);
                } else {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            } catch (Exception e) {
                toast("Abre el selector de usuarios desde los ajustes rápidos.");
            }
        });

        addText(root,
                "Para regresar a Cuenta 1 usa el selector de usuarios de Android.",
                13, Color.rgb(148,163,184), false);
    }

    private void renderNotProvisioned(LinearLayout root) {
        addText(root, "MODO AVANZADO NO ACTIVO", 12, Color.rgb(255,180,80), true);
        addText(root, "Multi ML", 34, Color.WHITE, true);
        addText(root,
                "La app está instalada, pero Android todavía no la reconoce como Device Owner.",
                16, Color.rgb(148,163,184), false);

        appButton(root, "Mercado Libre normal", ML);
        appButton(root, "Mercado Pago normal", MP);

        addText(root,
                "Para crear 3 usuarios adicionales hay que aprovisionar Multi ML como controlador del dispositivo durante la configuración inicial del teléfono.",
                14, Color.rgb(226,232,240), false);
    }

    private void ensurePrimaryAffiliation() {
        try {
            Set<String> ids = dpm.getAffiliationIds(admin);
            if (ids == null || !ids.contains(MultiMlAdminReceiver.AFFILIATION_ID)) {
                dpm.setAffiliationIds(
                        admin,
                        Collections.singleton(MultiMlAdminReceiver.AFFILIATION_ID));
            }
        } catch (Exception ignored) {
        }
    }

    private List<UserHandle> secondaryUsers() {
        try {
            return dpm.getSecondaryUsers(admin);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void createManagedUser() {
        List<UserHandle> before = secondaryUsers();
        if (before.size() >= MAX_SECONDARY) {
            message("Límite alcanzado", "Ya tienes las 3 cuentas adicionales configuradas.");
            return;
        }

        final int accountNumber = before.size() + 2;
        final String name = "Cuenta " + accountNumber;

        new AlertDialog.Builder(this)
                .setTitle("Crear " + name)
                .setMessage("Android creará un usuario independiente. Después podrás instalar Mercado Libre y Mercado Pago con otra sesión.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Crear", (dialog, which) -> doCreateUser(name))
                .show();
    }

    private void doCreateUser(String name) {
        try {
            ensurePrimaryAffiliation();

            PersistableBundle extras = new PersistableBundle();
            extras.putString(
                    MultiMlAdminReceiver.EXTRA_AFFILIATION,
                    MultiMlAdminReceiver.AFFILIATION_ID);

            int flags = DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED;

            UserHandle user = dpm.createAndManageUser(
                    admin,
                    name,
                    admin,
                    extras,
                    flags);

            if (user == null) {
                message("No se pudo crear", "Android no creó el usuario. El fabricante puede haber alcanzado su límite de usuarios.");
                return;
            }

            long serial = userManager.getSerialNumberForUser(user);
            prefs.edit().putString("name_" + serial, name).apply();

            int startResult = dpm.startUserInBackground(admin, user);
            toast(name + " creada. Preparando usuario…");

            if (!dpm.switchUser(admin, user)) {
                message("Cuenta creada",
                        name + " quedó creada, pero Android no permitió cambiar automáticamente. Usa el selector de usuarios.");
            }
        } catch (Exception e) {
            message("No se pudo crear la cuenta",
                    e.getClass().getSimpleName() + ": " +
                            (e.getMessage() == null ? "Android rechazó la operación." : e.getMessage()));
        }
    }

    private void switchTo(UserHandle user) {
        try {
            if (!dpm.switchUser(admin, user)) {
                toast("Android no permitió cambiar a esa cuenta.");
            }
        } catch (Exception e) {
            message("No se pudo abrir", "Android rechazó el cambio de usuario.");
        }
    }

    private void confirmRemoveUser(UserHandle user, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar " + name)
                .setMessage("Se borrarán todas las apps, sesiones y archivos guardados dentro de este usuario.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    try {
                        long serial = userManager.getSerialNumberForUser(user);
                        boolean removed = dpm.removeUser(admin, user);
                        if (removed) {
                            prefs.edit().remove("name_" + serial).apply();
                            toast(name + " eliminada.");
                            render();
                        } else {
                            toast("Android no pudo eliminar " + name + ".");
                        }
                    } catch (Exception e) {
                        message("No se pudo eliminar", "Android rechazó la operación.");
                    }
                })
                .show();
    }

    private void appButton(LinearLayout root, String title, String pkg) {
        boolean installed = installed(pkg);
        action(root, title + (installed ? " · ABRIR" : " · INSTALAR"), () -> {
            if (installed(pkg)) launch(pkg); else store(pkg);
        });
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
            try {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            } catch (Exception ignored) {
                toast("No encontré una tienda para instalar la aplicación.");
            }
        }
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

    private void actionSecondary(LinearLayout root, String title, Runnable run) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.topMargin = dp(6);
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
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("Entendido", null)
                .show();
    }
}
