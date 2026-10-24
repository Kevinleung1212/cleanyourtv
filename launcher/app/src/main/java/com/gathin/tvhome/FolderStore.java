package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owner-managed local app folders. No packages are disabled or uninstalled. */
final class FolderStore {
    static final class Folder {
        final String id, name;
        final List<String> packages;
        Folder(String id, String name, List<String> packages) {
            this.id = id; this.name = name; this.packages = packages;
        }
    }
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("custom_folders", Context.MODE_PRIVATE);
    }
    static List<Folder> all(Context c) {
        List<Folder> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("folders", "[]"));
            for (int i = 0; i < a.length() && i < 64; i++) {
                JSONObject f = a.optJSONObject(i);
                if (f == null) continue;
                JSONArray members = f.optJSONArray("members");
                List<String> pkgs = new ArrayList<>();
                if (members != null) for (int j = 0; j < members.length() && j < 128; j++) {
                    String p = members.optString(j, "");
                    if (p.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) pkgs.add(p);
                }
                String id = f.optString("id", "");
                if (id.matches("f[0-9]+")) out.add(new Folder(id, f.optString("name", "Folder"), pkgs));
            }
        } catch (Exception ignored) { }
        return out;
    }
    static Set<String> members(Context c) {
        Set<String> all = new HashSet<>();
        for (Folder f : all(c)) all.addAll(f.packages);
        return all;
    }
    static Folder get(Context c, String id) {
        for (Folder f : all(c)) if (f.id.equals(id)) return f;
        return null;
    }
    private static boolean save(Context c, List<Folder> folders) {
        try {
            JSONArray a = new JSONArray();
            for (Folder f : folders) {
                JSONObject o = new JSONObject().put("id", f.id).put("name", f.name);
                JSONArray m = new JSONArray();
                for (String p : f.packages) m.put(p);
                a.put(o.put("members", m));
            }
            return prefs(c).edit().putString("folders", a.toString()).commit();
        } catch (Exception ex) { return false; }
    }
    static boolean add(Context c, String id, String pkg) {
        if (pkg == null || !pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) return false;
        List<Folder> folders = all(c);
        boolean target = false;
        for (Folder f : folders) {
            f.packages.remove(pkg);
            if (f.id.equals(id)) { f.packages.add(pkg); target = true; }
        }
        return target && save(c, folders);
    }
    static String create(Context c, String name, String pkg) {
        List<Folder> folders = all(c);
        if (folders.size() >= 64) return null;
        String id = "f" + System.currentTimeMillis();
        List<String> members = new ArrayList<>();
        if (pkg != null) members.add(pkg);
        String label = name == null ? "" : name.trim();
        folders.add(new Folder(id, label.isEmpty() ? "Folder" : label.substring(0, Math.min(50, label.length())), members));
        return save(c, folders) ? id : null;
    }
    static boolean rename(Context c, String id, String name) {
        List<Folder> folders = all(c);
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) return false;
        for (int i = 0; i < folders.size(); i++) {
            Folder f = folders.get(i);
            if (f.id.equals(id)) {
                folders.set(i, new Folder(f.id, clean.substring(0, Math.min(50, clean.length())), f.packages));
                return save(c, folders);
            }
        }
        return false;
    }
    static boolean removeMember(Context c, String id, String pkg) {
        List<Folder> folders = all(c);
        for (Folder f : folders) if (f.id.equals(id) && f.packages.remove(pkg)) return save(c, folders);
        return false;
    }
    static boolean delete(Context c, String id) {
        List<Folder> folders = all(c);
        boolean removed = folders.removeIf(f -> f.id.equals(id));
        return removed && save(c, folders);
    }
}