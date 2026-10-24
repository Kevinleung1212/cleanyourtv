package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** TV remote friendly virtual folder: OK launches; hold OK removes a shortcut from the folder. */
public final class FolderActivity extends Activity {
    private String folderId;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        folderId = getIntent().getStringExtra("folder_id");
        render();
    }
    private void render() {
        FolderStore.Folder folder = FolderStore.get(this, folderId);
        if (folder == null) { finish(); return; }
        LinearLayout column = new LinearLayout(this);
        column.setPadding(48, 36, 48, 36);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(0xFF111923);
        TextView title = new TextView(this);
        title.setText("📁 " + folder.name);
        title.setTextSize(28); title.setTextColor(Color.WHITE);
        column.addView(title);
        PackageManager pm = getPackageManager();
        for (String pkg : folder.packages) {
            Intent target = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (target == null) target = pm.getLaunchIntentForPackage(pkg);
            final Intent launch = target;
            String name = pkg;
            try { name = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
            catch (Exception ignored) { }
            TextView row = new TextView(this);
            row.setText(name + (launch == null ? " (cannot open)" : ""));
            row.setTextColor(Color.WHITE); row.setTextSize(22);
            row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(24, 18, 24, 18);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                if (launch == null) { Toast.makeText(this, "App cannot be opened", Toast.LENGTH_SHORT).show(); return; }
                try { startActivity(launch); } catch (Exception ex) {
                    Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show();
                }
            });
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setMessage("Remove this app from folder?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            FolderStore.removeMember(this, folderId, pkg); render();
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            });
            column.addView(row);
        }
        TextView hint = new TextView(this);
        hint.setText("Long press OK to remove an app from the folder. Back returns Home.");
        hint.setTextColor(0xFFB0BEC5); hint.setPadding(10, 22, 10, 5);
        column.addView(hint);
        ScrollView scroll = new ScrollView(this); scroll.addView(column); setContentView(scroll);
    }
}