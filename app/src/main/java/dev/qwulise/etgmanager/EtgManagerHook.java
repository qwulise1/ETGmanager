package dev.qwulise.etgmanager;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class EtgManagerHook implements IXposedHookLoadPackage {
    private static final String TAG = "ETGmanager";
    private static final String TARGET = "com.exteragram.messenger";

    private Settings settings;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        settings = Settings.load();
        log("loaded in " + lpparam.packageName + ", process=" + lpparam.processName + ", mode=" + settings.pythonMode);

        if (settings.enableCrashGuard) installCrashLogger();
        if (settings.enableMemoryLog) logMemory("start");

        if ("block".equals(settings.pythonMode)) {
            forcePythonOff(lpparam.classLoader);
            blockEtgPluginController(lpparam.classLoader);
            blockPythonEngine(lpparam.classLoader);
        } else if ("guard".equals(settings.pythonMode)) {
            guardPythonMode(lpparam.classLoader);
            watchEtgPluginController(lpparam.classLoader);
            watchPythonEngine(lpparam.classLoader);
        } else {
            log("python mode allow: ETG python engine is untouched");
        }
    }

    private void guardPythonMode(final ClassLoader cl) {
        final Class<?> cfg = XposedHelpers.findClassIfExists("com.exteragram.messenger.ExteraConfig", cl);
        if (cfg == null) {
            log("ExteraConfig not found");
            return;
        }
        applyGuardConfig(cfg, "early");
        XC_MethodHook after = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                applyGuardConfig(cfg, String.valueOf(((Method) param.method).getName()));
            }
        };
        hookAll(cfg, "loadConfig", after);
        hookAll(cfg, "reloadConfig", after);
        hookAll(cfg, "init", after);
    }

    private void applyGuardConfig(Class<?> cfg, String reason) {
        setBool(cfg, "pluginsPySdkAutoUpdate", false);
        setBool(cfg, "pluginsPySdkBetaVersions", false);
        log("python guard applied: sdk autoupdate off, reason=" + reason);
    }

    private void forcePythonOff(final ClassLoader cl) {
        final Class<?> cfg = XposedHelpers.findClassIfExists("com.exteragram.messenger.ExteraConfig", cl);
        if (cfg == null) return;
        applyBlockConfig(cfg, "early");
        XC_MethodHook after = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                applyBlockConfig(cfg, String.valueOf(((Method) param.method).getName()));
            }
        };
        hookAll(cfg, "loadConfig", after);
        hookAll(cfg, "reloadConfig", after);
        hookAll(cfg, "init", after);
    }

    private void applyBlockConfig(Class<?> cfg, String reason) {
        setBool(cfg, "pluginsEngine", false);
        setBool(cfg, "pluginsPySdkAutoUpdate", false);
        setBool(cfg, "pluginsPySdkBetaVersions", false);
        log("python engine blocked: " + reason);
    }

    private void watchEtgPluginController(ClassLoader cl) {
        Class<?> pc = XposedHelpers.findClassIfExists("com.exteragram.messenger.plugins.PluginsController", cl);
        if (pc == null) return;
        XC_MethodHook watch = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                log("PluginsController." + ((Method) param.method).getName() + " begin");
                if (settings.enableMemoryLog) logMemory("before-plugins-controller");
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                log("PluginsController." + ((Method) param.method).getName() + " end");
                if (settings.enableMemoryLog) logMemory("after-plugins-controller");
            }
        };
        hookAll(pc, "init", watch);
        hookAll(pc, "restart", watch);
    }

    private void watchPythonEngine(ClassLoader cl) {
        Class<?> pe = XposedHelpers.findClassIfExists("com.exteragram.messenger.plugins.PythonPluginsEngine", cl);
        if (pe == null) return;
        XC_MethodHook watch = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                log("PythonPluginsEngine." + ((Method) param.method).getName() + " begin");
                if (settings.enableMemoryLog) logMemory("before-python-engine");
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                log("PythonPluginsEngine." + ((Method) param.method).getName() + " end");
                if (settings.enableMemoryLog) logMemory("after-python-engine");
            }
        };
        hookAll(pe, "init", watch);
        hookAll(pe, "loadPlugins", watch);
        hookAll(pe, "loadPlugin", watch);
        hookAll(pe, "unloadPlugin", watch);
    }

    private void blockEtgPluginController(ClassLoader cl) {
        Class<?> pc = XposedHelpers.findClassIfExists("com.exteragram.messenger.plugins.PluginsController", cl);
        if (pc == null) return;
        hookAll(pc, "isPluginEngineSupported", new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) { return false; }
        });
        XC_MethodReplacement noop = new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) {
                log("skip PluginsController." + ((Method) param.method).getName());
                return null;
            }
        };
        hookAll(pc, "init", noop);
        hookAll(pc, "restart", noop);
    }

    private void blockPythonEngine(ClassLoader cl) {
        Class<?> pe = XposedHelpers.findClassIfExists("com.exteragram.messenger.plugins.PythonPluginsEngine", cl);
        if (pe == null) return;
        XC_MethodReplacement noop = new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) {
                log("skip PythonPluginsEngine." + ((Method) param.method).getName());
                return null;
            }
        };
        hookAll(pe, "init", noop);
        hookAll(pe, "loadPlugins", noop);
        hookAll(pe, "loadPlugin", noop);
        hookAll(pe, "unloadPlugin", noop);
    }

    private void hookAll(Class<?> cls, String name, XC_MethodHook hook) {
        try {
            int count = XposedBridge.hookAllMethods(cls, name, hook).size();
            if (count > 0) log("hooked " + cls.getName() + "." + name + " x" + count);
        } catch (Throwable t) {
            log("hook failed " + cls.getName() + "." + name + ": " + t);
        }
    }

    private void setBool(Class<?> cls, String field, boolean value) {
        try { XposedHelpers.setStaticBooleanField(cls, field, value); } catch (Throwable ignored) {}
    }

    private void installCrashLogger() {
        try {
            final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override public void uncaughtException(Thread thread, Throwable throwable) {
                    log("crash thread=" + thread.getName() + ": " + throwable);
                    logMemory("before-crash");
                    if (old != null) old.uncaughtException(thread, throwable);
                }
            });
        } catch (Throwable t) {
            log("crash logger failed: " + t);
        }
    }

    private void logMemory(String point) {
        try {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024L / 1024L;
            long total = rt.totalMemory() / 1024L / 1024L;
            long max = rt.maxMemory() / 1024L / 1024L;
            log("memory " + point + ": used=" + used + "MB total=" + total + "MB max=" + max + "MB");
        } catch (Throwable ignored) {}
    }

    private static void log(String s) { XposedBridge.log(TAG + ": " + s); }

    private static final class Settings {
        String pythonMode = "guard";
        boolean enableCrashGuard = true;
        boolean enableMemoryLog = true;

        static Settings load() {
            Settings s = new Settings();
            File file = new File("/data/adb/etgmanager/config.properties");
            if (!file.exists()) return s;
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(file)) {
                p.load(in);
                String mode = p.getProperty("python_mode", s.pythonMode).trim().toLowerCase();
                if ("allow".equals(mode) || "guard".equals(mode) || "block".equals(mode)) s.pythonMode = mode;
                s.enableCrashGuard = bool(p, "enable_crash_guard", s.enableCrashGuard);
                s.enableMemoryLog = bool(p, "enable_memory_log", s.enableMemoryLog);
            } catch (Throwable t) {
                log("config read failed: " + t);
            }
            return s;
        }

        private static boolean bool(Properties p, String key, boolean def) {
            String v = p.getProperty(key);
            if (v == null) return def;
            v = v.trim().toLowerCase();
            if ("1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v)) return true;
            if ("0".equals(v) || "false".equals(v) || "no".equals(v) || "off".equals(v)) return false;
            return def;
        }
    }
}
