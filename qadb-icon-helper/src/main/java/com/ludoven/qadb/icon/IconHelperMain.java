package com.ludoven.qadb.icon;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.os.Process;
import android.util.Base64;
import android.util.DisplayMetrics;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

public final class IconHelperMain {
    private static final String CACHE_DIR = "/data/local/tmp/qadb/icons";
    private static final int DEFAULT_SIZE_PX = 192;
    private static final int ICON_CACHE_VERSION = 5;
    private static final int MAX_INLINE_ICON_BYTES = 256 * 1024;

    private IconHelperMain() {
    }

    public static void main(String[] args) {
        long startedAt = System.currentTimeMillis();
        if (args.length >= 1 && "clear-cache".equals(args[0])) {
            clearCache(args.length >= 2 ? args[1] : null, startedAt);
            return;
        }

        if (args.length >= 1 && "list-labels".equals(args[0])) {
            listLabels(startedAt);
            return;
        }

        if (args.length >= 2 && "serve-icons".equals(args[0])) {
            serveIcons(parseSize(args[1]), startedAt);
            return;
        }

        if (args.length >= 3 && "get-icons".equals(args[0])) {
            int sizePx = parseSize(args[1]);
            try {
                Context context = systemContext();
                if (context == null) {
                    fail(null, "system context unavailable", startedAt, null);
                    return;
                }
                PackageManager packageManager = context.getPackageManager();
                for (int index = 2; index < args.length; index++) {
                    handleGetIcon(args[index], sizePx, context, packageManager, System.currentTimeMillis());
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
                fail(null, throwable.getClass().getSimpleName() + ": " + nullToEmpty(throwable.getMessage()), startedAt, null);
            }
            return;
        }

        if (args.length < 2 || !"get-icon".equals(args[0])) {
            fail(null, "usage: get-icon <packageName> [sizePx] | get-icons <sizePx> <packageName...> | serve-icons <sizePx> | list-labels | clear-cache [packageName]", startedAt, null);
            return;
        }

        String packageName = args[1];
        int sizePx = parseSize(args.length >= 3 ? args[2] : null);
        try {
            Context context = systemContext();
            if (context == null) {
                fail(packageName, "system context unavailable", startedAt, null);
                return;
            }

            PackageManager packageManager = context.getPackageManager();
            handleGetIcon(packageName, sizePx, context, packageManager, startedAt);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            fail(packageName, throwable.getClass().getSimpleName() + ": " + nullToEmpty(throwable.getMessage()), startedAt, null);
        }
    }

    private static void serveIcons(int sizePx, long startedAt) {
        try {
            Context context = systemContext();
            if (context == null) {
                fail(null, "system context unavailable", startedAt, null);
                return;
            }
            PackageManager packageManager = context.getPackageManager();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String value = line.trim();
                if (value.length() == 0) continue;
                if ("__qadb_exit__".equals(value)) {
                    System.out.println("BYE elapsedMs=" + (System.currentTimeMillis() - startedAt));
                    System.out.flush();
                    return;
                }
                if (value.startsWith("__qadb_end_batch__")) {
                    String batch = value.substring("__qadb_end_batch__".length()).trim();
                    System.out.println("DONE batch=" + batch + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
                    System.out.flush();
                    continue;
                }
                handleGetIcon(value, sizePx, context, packageManager, System.currentTimeMillis());
                System.out.flush();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            fail(null, throwable.getClass().getSimpleName() + ": " + nullToEmpty(throwable.getMessage()), startedAt, null);
        }
    }

    private static void listLabels(long startedAt) {
        try {
            Context context = systemContext();
            if (context == null) {
                fail(null, "system context unavailable", startedAt, null);
                return;
            }
            PackageManager packageManager = context.getPackageManager();
            List<ApplicationInfo> applications = packageManager.getInstalledApplications(packageQueryFlags());
            if (applications == null) applications = Collections.emptyList();
            applications.sort(new Comparator<ApplicationInfo>() {
                @Override
                public int compare(ApplicationInfo left, ApplicationInfo right) {
                    return left.packageName.compareToIgnoreCase(right.packageName);
                }
            });
            for (ApplicationInfo info : applications) {
                if (info == null || info.packageName == null || info.packageName.length() == 0) continue;
                Intent launchIntent = packageManager.getLaunchIntentForPackage(info.packageName);
                if (launchIntent == null) {
                    launchIntent = packageManager.getLeanbackLaunchIntentForPackage(info.packageName);
                }
                if (launchIntent == null) continue;
                String label = resolveLabel(packageManager, info.packageName, info);
                System.out.println("APP package=" + info.packageName
                    + " label64=" + base64(label)
                    + " enabled=" + (info.enabled ? "1" : "0"));
            }
            System.out.println("DONE elapsedMs=" + (System.currentTimeMillis() - startedAt));
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            fail(null, throwable.getClass().getSimpleName() + ": " + nullToEmpty(throwable.getMessage()), startedAt, null);
        }
    }

    private static void handleGetIcon(
        String packageName,
        int sizePx,
        Context context,
        PackageManager packageManager,
        long startedAt
    ) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, packageQueryFlags());
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            String label = resolveLabel(packageManager, packageName, applicationInfo);
            String cachePath = cachePath(packageName, packageInfo, applicationInfo);
            File cacheFile = new File(cachePath);
            if (cacheFile.isFile() && cacheFile.length() > 0L) {
                ok(packageName, label, cachePath, "cache", true, startedAt, inlinePngBytes(readFileBytes(cacheFile)));
                return;
            }

            IconResult iconResult = resolveIcon(context, packageManager, packageName, applicationInfo, sizePx);
            if (iconResult.drawable == null) {
                iconResult = new IconResult(null, "generatedDefault");
            }

            byte[] png;
            String source = iconResult.source;
            if (iconResult.drawable == null) {
                png = fallbackPngBytes(sizePx);
                source = "generatedDefault";
            } else {
                try {
                    png = drawableToPngBytes(iconResult.drawable, sizePx);
                } catch (Throwable renderError) {
                    png = fallbackPngBytes(sizePx);
                    source = "generatedDefault";
                    renderError.printStackTrace(System.err);
                }
            }
            File dir = cacheFile.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                fail(packageName, "unable to create cache dir: " + dir.getAbsolutePath(), startedAt, source);
                return;
            }
            File tmpFile = new File(cachePath + ".tmp." + Process.myPid());
            FileOutputStream out = new FileOutputStream(tmpFile);
            try {
                out.write(png);
            } finally {
                out.close();
            }
            if (cacheFile.exists() && !cacheFile.delete()) {
                fail(packageName, "unable to replace cache file: " + cachePath, startedAt, source);
                return;
            }
            if (!tmpFile.renameTo(cacheFile)) {
                fail(packageName, "unable to move cache file: " + cachePath, startedAt, source);
                return;
            }

            ok(packageName, label, cachePath, source, false, startedAt, inlinePngBytes(png));
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            fail(packageName, throwable.getClass().getSimpleName() + ": " + nullToEmpty(throwable.getMessage()), startedAt, null);
        }
    }

    private static IconResult resolveIcon(
        Context context,
        PackageManager packageManager,
        String packageName,
        ApplicationInfo applicationInfo,
        int sizePx
    ) {
        IconCandidateFilter filter = new IconCandidateFilter(packageManager, sizePx);
        IconResult candidate = loadLauncherAppsIcon(context, packageName);
        if (filter.accept(candidate)) return candidate;

        candidate = loadPackageContextIcon(context, packageName, applicationInfo.icon, "packageContext.applicationInfo.icon");
        if (filter.accept(candidate)) return candidate;

        candidate = loadPackageResourceIcon(packageManager, applicationInfo, applicationInfo.icon, "packageResources.applicationInfo.icon");
        if (filter.accept(candidate)) return candidate;

        candidate = loadResourceIcon(applicationInfo, applicationInfo.icon, "applicationInfo.icon");
        if (filter.accept(candidate)) return candidate;

        int roundIcon = roundIconResId(applicationInfo);
        if (roundIcon != 0) {
            candidate = loadPackageContextIcon(context, packageName, roundIcon, "packageContext.roundIcon");
            if (filter.accept(candidate)) return candidate;

            candidate = loadPackageResourceIcon(packageManager, applicationInfo, roundIcon, "packageResources.roundIcon");
            if (filter.accept(candidate)) return candidate;

            candidate = loadResourceIcon(applicationInfo, roundIcon, "roundIcon");
            if (filter.accept(candidate)) return candidate;
        }

        candidate = loadLaunchIntentIcon(packageManager, packageName);
        if (filter.accept(candidate)) return candidate;

        candidate = loadLauncherActivityIcon(packageManager, packageName);
        if (filter.accept(candidate)) return candidate;

        candidate = loadApplicationInfoIcon(packageManager, applicationInfo);
        if (filter.accept(candidate)) return candidate;

        candidate = loadApplicationIcon(packageManager, packageName);
        if (filter.accept(candidate)) return candidate;

        try {
            return new IconResult(packageManager.getDefaultActivityIcon(), "default");
        } catch (Throwable ignored) {
            return new IconResult(null, "generatedDefault");
        }
    }

    private static int packageQueryFlags() {
        return Build.VERSION.SDK_INT >= 24 ? PackageManager.MATCH_DISABLED_COMPONENTS : 0;
    }

    private static Context systemContext() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method systemMain = activityThreadClass.getDeclaredMethod("systemMain");
        Object activityThread = systemMain.invoke(null);
        Method getSystemContext = activityThreadClass.getDeclaredMethod("getSystemContext");
        return (Context) getSystemContext.invoke(activityThread);
    }

    private static IconResult loadLauncherAppsIcon(Context context, String packageName) {
        if (Build.VERSION.SDK_INT < 21 || context == null || packageName == null || packageName.length() == 0) return null;
        try {
            Object service = context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (!(service instanceof LauncherApps)) return null;
            LauncherApps launcherApps = (LauncherApps) service;
            List<LauncherActivityInfo> activities = launcherApps.getActivityList(packageName, Process.myUserHandle());
            if (activities == null || activities.isEmpty()) return null;
            for (LauncherActivityInfo activityInfo : activities) {
                if (activityInfo == null) continue;
                Drawable icon = activityInfo.getIcon(0);
                if (icon != null) return new IconResult(icon, "launcherApps");
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static IconResult loadLauncherActivityIcon(PackageManager packageManager, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(packageName);
            List<ResolveInfo> infos = packageManager.queryIntentActivities(intent, packageQueryFlags());
            if (infos == null || infos.isEmpty()) return null;
            infos.sort(new Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo left, ResolveInfo right) {
                    int leftOrder = left == null ? 0 : left.priority;
                    int rightOrder = right == null ? 0 : right.priority;
                    return Integer.compare(rightOrder, leftOrder);
                }
            });
            for (ResolveInfo info : infos) {
                ActivityInfo activityInfo = info.activityInfo;
                if (activityInfo == null) continue;
                IconResult icon = loadActivityInfoIcon(packageManager, activityInfo, "launcherActivityIcon");
                if (icon != null && icon.drawable != null) return icon;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static IconResult loadLaunchIntentIcon(PackageManager packageManager, String packageName) {
        IconResult result = loadLaunchIntentIcon(packageManager, packageName, false);
        return result != null ? result : loadLaunchIntentIcon(packageManager, packageName, true);
    }

    private static IconResult loadLaunchIntentIcon(PackageManager packageManager, String packageName, boolean leanback) {
        try {
            Intent intent = leanback
                ? packageManager.getLeanbackLaunchIntentForPackage(packageName)
                : packageManager.getLaunchIntentForPackage(packageName);
            if (intent == null) return null;
            ComponentName component = intent.getComponent();
            ActivityInfo activityInfo = component != null
                ? packageManager.getActivityInfo(component, packageQueryFlags())
                : null;
            if (activityInfo == null) {
                ResolveInfo resolveInfo = packageManager.resolveActivity(intent, packageQueryFlags());
                activityInfo = resolveInfo == null ? null : resolveInfo.activityInfo;
            }
            if (activityInfo == null) return null;
            return loadActivityInfoIcon(
                packageManager,
                activityInfo,
                leanback ? "leanbackLaunchIntentIcon" : "launchIntentIcon"
            );
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static IconResult loadActivityInfoIcon(
        PackageManager packageManager,
        ActivityInfo activityInfo,
        String source
    ) {
        if (activityInfo == null) return null;
        int iconResId = activityInfo.getIconResource();
        if (iconResId == 0) iconResId = activityInfo.icon;
        IconResult candidate = loadPackageResourceIcon(packageManager, activityInfo.applicationInfo, iconResId, source + ".resource");
        if (candidate != null && candidate.drawable != null) return candidate;

        candidate = loadResourceIcon(activityInfo.applicationInfo, iconResId, source + ".assetResource");
        if (candidate != null && candidate.drawable != null) return candidate;

        try {
            Drawable icon = activityInfo.loadIcon(packageManager);
            return icon == null ? null : new IconResult(icon, source + ".loadIcon");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IconResult loadApplicationIcon(PackageManager packageManager, String packageName) {
        try {
            Drawable icon = packageManager.getApplicationIcon(packageName);
            return icon == null ? null : new IconResult(icon, "getApplicationIcon");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IconResult loadApplicationInfoIcon(PackageManager packageManager, ApplicationInfo applicationInfo) {
        if (applicationInfo == null) return null;
        try {
            Drawable icon = applicationInfo.loadIcon(packageManager);
            return icon == null ? null : new IconResult(icon, "applicationInfo.loadIcon");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IconResult loadPackageContextIcon(
        Context context,
        String packageName,
        int iconResId,
        String source
    ) {
        if (context == null || packageName == null || packageName.length() == 0 || iconResId == 0) return null;
        try {
            Context packageContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            Resources resources = packageContext.getResources();
            Drawable drawable = Build.VERSION.SDK_INT >= 21
                ? resources.getDrawable(iconResId, packageContext.getTheme())
                : resources.getDrawable(iconResId);
            return drawable == null ? null : new IconResult(drawable, source);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IconResult loadPackageResourceIcon(
        PackageManager packageManager,
        ApplicationInfo applicationInfo,
        int iconResId,
        String source
    ) {
        if (applicationInfo == null || iconResId == 0) return null;
        try {
            Resources resources = packageManager.getResourcesForApplication(applicationInfo);
            Drawable drawable = Build.VERSION.SDK_INT >= 21
                ? resources.getDrawable(iconResId, null)
                : resources.getDrawable(iconResId);
            return drawable == null ? null : new IconResult(drawable, source);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IconResult loadResourceIcon(
        ApplicationInfo applicationInfo,
        int iconResId,
        String source
    ) {
        if (applicationInfo == null || iconResId == 0) return null;
        try {
            AssetManager assetManager = AssetManager.class.getDeclaredConstructor().newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            if (applicationInfo.sourceDir == null || applicationInfo.sourceDir.length() == 0) return null;
            int addedBase = addAssetPath(assetManager, addAssetPath, applicationInfo.sourceDir);
            if (addedBase == 0) return null;
            if (applicationInfo.splitSourceDirs != null) {
                for (String splitSourceDir : applicationInfo.splitSourceDirs) {
                    addAssetPath(assetManager, addAssetPath, splitSourceDir);
                }
            }
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            Configuration configuration = new Configuration();
            configuration.setToDefaults();
            Resources resources = new Resources(assetManager, metrics, configuration);
            Drawable drawable = Build.VERSION.SDK_INT >= 21
                ? resources.getDrawable(iconResId, null)
                : resources.getDrawable(iconResId);
            return drawable == null ? null : new IconResult(drawable, source);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int addAssetPath(AssetManager assetManager, Method addAssetPath, String path) {
        if (path == null || path.length() == 0) return 0;
        try {
            Object result = addAssetPath.invoke(assetManager, path);
            return result instanceof Integer ? (Integer) result : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int roundIconResId(ApplicationInfo applicationInfo) {
        if (Build.VERSION.SDK_INT < 25 || applicationInfo == null) return 0;
        try {
            Object value = ApplicationInfo.class.getField("roundIcon").get(applicationInfo);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String resolveLabel(
        PackageManager packageManager,
        String packageName,
        ApplicationInfo applicationInfo
    ) {
        try {
            CharSequence label = applicationInfo == null ? null : applicationInfo.loadLabel(packageManager);
            if (label != null && label.length() > 0) return label.toString();
        } catch (Throwable ignored) {
        }
        try {
            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            if (label != null && label.length() > 0) return label.toString();
        } catch (Throwable ignored) {
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(packageName);
            List<ResolveInfo> infos = packageManager.queryIntentActivities(intent, packageQueryFlags());
            if (infos != null) {
                for (ResolveInfo info : infos) {
                    CharSequence label = info.loadLabel(packageManager);
                    if (label != null && label.length() > 0) return label.toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return packageName;
    }

    private static byte[] drawableToPngBytes(Drawable drawable, int sizePx) throws Exception {
        int safeSize = Math.max(1, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        Drawable copy = drawable.getConstantState() != null ? drawable.getConstantState().newDrawable().mutate() : drawable.mutate();
        int oldLeft = copy.getBounds().left;
        int oldTop = copy.getBounds().top;
        int oldRight = copy.getBounds().right;
        int oldBottom = copy.getBounds().bottom;
        copy.setBounds(0, 0, safeSize, safeSize);
        copy.draw(canvas);
        copy.setBounds(oldLeft, oldTop, oldRight, oldBottom);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            throw new IllegalStateException("PNG compression failed");
        }
        bitmap.recycle();
        return output.toByteArray();
    }

    private static byte[] readFileBytes(File file) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(file.length(), 64 * 1024));
        FileInputStream input = new FileInputStream(file);
        try {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } finally {
            input.close();
        }
        return output.toByteArray();
    }

    private static byte[] inlinePngBytes(byte[] png) {
        if (png == null || png.length == 0 || png.length > MAX_INLINE_ICON_BYTES) return null;
        return png;
    }

    private static byte[] fallbackPngBytes(int sizePx) throws Exception {
        int safeSize = Math.max(1, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(255, 232, 236, 242));
        float radius = safeSize * 0.22f;
        canvas.drawRoundRect(0, 0, safeSize, safeSize, radius, radius, paint);
        paint.setColor(Color.argb(255, 118, 132, 153));
        float inset = safeSize * 0.28f;
        canvas.drawCircle(safeSize / 2f, safeSize / 2f, safeSize / 2f - inset, paint);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            throw new IllegalStateException("fallback PNG compression failed");
        }
        bitmap.recycle();
        return output.toByteArray();
    }

    private static String cachePath(String packageName, PackageInfo packageInfo, ApplicationInfo applicationInfo) {
        long versionCode = Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
        long lastUpdateTime = packageInfo.lastUpdateTime;
        long sourceSize = applicationInfo != null && applicationInfo.sourceDir != null
            ? new File(applicationInfo.sourceDir).length()
            : 0L;
        int splitCount = applicationInfo != null && applicationInfo.splitSourceDirs != null
            ? applicationInfo.splitSourceDirs.length
            : 0;
        return CACHE_DIR + "/" + safeFileToken(packageName) + "_" + versionCode + "_" + lastUpdateTime
            + "_" + sourceSize + "_" + splitCount + "_v" + ICON_CACHE_VERSION + ".png";
    }

    private static String safeFileToken(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static int parseSize(String raw) {
        if (raw == null) return DEFAULT_SIZE_PX;
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(32, Math.min(512, value));
        } catch (NumberFormatException ignored) {
            return DEFAULT_SIZE_PX;
        }
    }

    private static void clearCache(String packageName, long startedAt) {
        File dir = new File(CACHE_DIR);
        if (!dir.isDirectory()) {
            System.out.println("OK package=" + nullToEmpty(packageName)
                + " cleared=0 elapsedMs=" + (System.currentTimeMillis() - startedAt));
            return;
        }
        String prefix = packageName == null || packageName.length() == 0 ? null : safeFileToken(packageName) + "_";
        int cleared = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isFile()) continue;
                if (prefix != null && !file.getName().startsWith(prefix)) continue;
                if (file.delete()) cleared++;
            }
        }
        System.out.println("OK package=" + nullToEmpty(packageName)
            + " cleared=" + cleared
            + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
    }

    private static void ok(
        String packageName,
        String label,
        String path,
        String source,
        boolean cacheHit,
        long startedAt,
        byte[] png
    ) {
        System.out.println("OK package=" + nullToEmpty(packageName)
            + " label64=" + base64(label)
            + " path=" + path
            + " source=" + source
            + " cache=" + (cacheHit ? "hit" : "miss")
            + " elapsedMs=" + (System.currentTimeMillis() - startedAt)
            + " data64=" + base64(png));
    }

    private static void fail(String packageName, String reason, long startedAt, String source) {
        System.out.println("ERR package=" + nullToEmpty(packageName)
            + " reason=" + reason.replace('\n', ' ')
            + " source=" + nullToEmpty(source)
            + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String base64(String value) {
        return Base64.encodeToString(nullToEmpty(value).getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    private static String base64(byte[] value) {
        if (value == null || value.length == 0) return "";
        return Base64.encodeToString(value, Base64.URL_SAFE | Base64.NO_WRAP);
    }

    private static boolean sameBytes(byte[] left, byte[] right) {
        if (left == right) return true;
        if (left == null || right == null || left.length != right.length) return false;
        for (int index = 0; index < left.length; index++) {
            if (left[index] != right[index]) return false;
        }
        return true;
    }

    private static final class IconCandidateFilter {
        private final PackageManager packageManager;
        private final int sizePx;
        private byte[] defaultIconBytes;

        IconCandidateFilter(PackageManager packageManager, int sizePx) {
            this.packageManager = packageManager;
            this.sizePx = sizePx;
        }

        boolean accept(IconResult candidate) {
            if (candidate == null || candidate.drawable == null) return false;
            return !isDefaultActivityIcon(candidate.drawable);
        }

        private boolean isDefaultActivityIcon(Drawable drawable) {
            try {
                return sameBytes(drawableToPngBytes(drawable, sizePx), defaultIconBytes());
            } catch (Throwable ignored) {
                return false;
            }
        }

        private byte[] defaultIconBytes() throws Exception {
            if (defaultIconBytes == null) {
                defaultIconBytes = drawableToPngBytes(packageManager.getDefaultActivityIcon(), sizePx);
            }
            return defaultIconBytes;
        }
    }

    private static final class IconResult {
        final Drawable drawable;
        final String source;

        IconResult(Drawable drawable, String source) {
            this.drawable = drawable;
            this.source = source;
        }
    }
}
