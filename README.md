# ETGmanager

LSPosed manager/guard for **ExteraGram / ETG**.

Author: **@qwulise**

## What it is

ETGmanager is not a replacement for ETG itself and not a Python runtime clone.
It is a lightweight LSPosed layer that puts ETG's plugin engine under control.

The default idea is **manager-first**:

```text
ETGmanager controls and monitors
ETG Python plugins may still work
Java/LSPosed layer stays the main guard
```

## v0.1.1 features

- visual Android manager screen;
- Python mode selector: `guard`, `block`, `allow`;
- root config writer for `/data/adb/etgmanager/config.properties`;
- crash guard toggle;
- memory log toggle;
- LSPosed hook for `com.exteragram.messenger`;
- logs ETG plugin engine startup/shutdown in guard mode;
- can fully block ETG Python engine in block mode.

## Python modes

### guard — default and recommended

Python plugins can work, but ETGmanager:

- watches `PluginsController`;
- watches `PythonPluginsEngine`;
- logs memory before/after plugin engine calls;
- disables Python SDK auto-update and beta SDK update flags.

### block — maximum battery/RAM saving

ETGmanager blocks:

- `PluginsController.init/restart`;
- `PythonPluginsEngine.init/loadPlugins/loadPlugin/unloadPlugin`;
- `pluginsEngine` config flag.

### allow — maximum compatibility

ETGmanager does not touch Python plugin engine. It only keeps crash/memory logging if enabled.

## Install

1. Build/download the debug APK from GitHub Actions.
2. Install APK.
3. Enable module in LSPosed.
4. Scope only: `com.exteragram.messenger`.
5. Open ETGmanager and choose mode.
6. Save config through root.
7. Force stop ETG and open it again.
8. Check LSPosed logs for `ETGmanager:` lines.

## Config path

```text
/data/adb/etgmanager/config.properties
```

Example:

```properties
python_mode=guard
enable_crash_guard=true
enable_memory_log=true
```

## Build

```bash
gradle :app:assembleDebug
```

GitHub Actions builds a debug APK automatically on every push to `main`.
