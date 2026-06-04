# ETGmanager

LSPosed Battery/Crash Guard for **ExteraGram / ETG**.

Author: **@qwulise**

## What it does in v0.1.0

This is not a Python plugin loader and not a DEX plugin marketplace.
It is a lightweight LSPosed module whose goal is to stop ETG from wasting RAM/battery on the built-in Python plugin engine.

Current features:

- targets `com.exteragram.messenger`;
- disables ETG Python plugin engine by default;
- blocks `PluginsController.init/restart`;
- blocks `PythonPluginsEngine.init/loadPlugins`;
- blocks Python SDK updater calls;
- blocks Chaquopy `Python.start` as an extra guard;
- logs heap memory snapshots to LSPosed/Xposed logs;
- installs crash logger which records the crash and memory before passing crash to Android.

## Why Java?

LSPosed modules are Android code running directly inside the target app process.
Using Java/Kotlin here is not for making “Java plugins”; it is for avoiding ETG's Python/Chaquopy runtime completely.

The idea is:

```text
ETG Python plugins: heavier, Chaquopy, plugin watchdog, more RAM pressure
ETGmanager LSPosed: tiny Java hook layer, no Python runtime, no ETG plugin engine
```

## Install

1. Build/download the debug APK from GitHub Actions.
2. Install APK.
3. Enable module in LSPosed.
4. Scope only: `com.exteragram.messenger`.
5. Force stop ETG and open it again.
6. Check LSPosed logs for `ETGmanager:` lines.

## Optional root config

Create:

```text
/data/adb/etgmanager/config.properties
```

Example:

```properties
disable_python_engine=true
enable_crash_guard=true
enable_memory_log=true
```

Default values are all `true`.

## Build

```bash
gradle :app:assembleDebug
```

GitHub Actions builds a debug APK automatically on every push to `main`.
