package dev.qwulise.etgmanager;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class EtgManagerHook implements IXposedHookLoadPackage {
    private static final String TAG = "ETGmanager";
    private static final String TARGET = "com.exteragram.messenger";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;
        log("loaded in " + lpparam.packageName + ", process=" + lpparam.processName);
        installCrashLogger();
        logMemory("start");
        forceConfig(lpparam.classLoader);
        stopEtgPluginController(lpparam.classLoader);
        stopPythonEngine(lpparam.classLoader);
    }

    private void forceConfig(final ClassLoader cl) {
        final Class<?> cfg = XposedHelpers.findClassIfExists("com.exteragram.messenger.ExteraConfig", cl);
        if (cfg == null) {
            log("ExteraConfig not found");
            return;
        }
        applyConfig(cfg, "early");
        XC_MethodHook after = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                applyConfig(cfg, String.valueOf(((Method) param.method).getName()));
            }
        };
        hookAll(cfg, "loadConfig", after);
        hookAll(cfg, "reloadConfig", after);
        hookAll(cfg, "init", after);
    }

    private void applyConfig(Class<?> cfg, String reason) {
        setBool(cfg, "pluginsEngine", false);
        setBool(cfg, "pluginsSafeMode", true);
        setBool(cfg, "pluginsPySdkAutoUpdate", false);
        setBool(cfg, "pluginsPySdkBetaVersions", false);
        log("plugin engine off: " + reason);
    }

    private void stopEtgPluginController(ClassLoader cl) {
        Class<?> pc = XposedHelpers.findClassIfExists("com.exteragram.messenger.plugins.PluginsController", cl);
        if (pc == null) return;
        hookAll(pc, "isPluginEngineSupported", new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) {
                return false;
            }
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

    private void stopPythonEngine(ClassLoader cl) {
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
        try {
            XposedHelpers.setStaticBooleanField(cls, field, value);
        } catch (Throwable ignored) {}
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

    private static void log(String s) {
        XposedBridge.log(TAG + ": " + s);
    }
}
