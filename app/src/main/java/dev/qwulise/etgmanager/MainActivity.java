package dev.qwulise.etgmanager;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private static final String CONFIG_PATH = "/data/adb/etgmanager/config.properties";
    private static final int BLUE = Color.rgb(70, 91, 205);

    private String pythonMode = "guard";
    private boolean crashGuard = true;
    private boolean memoryLog = true;

    private TextView statusText;
    private RadioButton allowRadio;
    private RadioButton guardRadio;
    private RadioButton blockRadio;
    private Switch crashSwitch;
    private Switch memorySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(245, 246, 250));
        getWindow().setNavigationBarColor(Color.rgb(245, 246, 250));
        loadConfigFromRoot();
        buildUi();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 246, 250));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(18));
        scroll.addView(root);

        TextView title = text("ETGmanager", 30, true, Color.rgb(20, 24, 35));
        root.addView(title);
        TextView sub = text("LSPosed guard для ExteraGram • v0.1.2", 14, false, Color.rgb(100, 106, 120));
        sub.setPadding(0, dp(3), 0, dp(14));
        root.addView(sub);

        statusText = text("", 14, false, Color.rgb(35, 39, 50));
        root.addView(card("Статус", statusText));

        LinearLayout modeBox = new LinearLayout(this);
        modeBox.setOrientation(LinearLayout.VERTICAL);
        modeBox.addView(desc("Выбери, как ETGmanager относится к встроенным Python-плагинам ETG."));

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        allowRadio = radio("Allow — Python не трогаем");
        guardRadio = radio("Guard — Python работает, но под контролем");
        blockRadio = radio("Block — полностью гасим Python engine");
        group.addView(guardRadio);
        group.addView(blockRadio);
        group.addView(allowRadio);
        if ("allow".equals(pythonMode)) allowRadio.setChecked(true);
        else if ("block".equals(pythonMode)) blockRadio.setChecked(true);
        else guardRadio.setChecked(true);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int checkedId) {
                if (checkedId == allowRadio.getId()) pythonMode = "allow";
                else if (checkedId == blockRadio.getId()) pythonMode = "block";
                else pythonMode = "guard";
                refreshStatus();
            }
        });
        modeBox.addView(group);
        root.addView(card("Python режим", modeBox));

        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        crashSwitch = sw("Crash guard", "Записывает память и причину перед падением ETG", crashGuard);
        memorySwitch = sw("Memory log", "Пишет RAM до/после запуска plugin engine", memoryLog);
        crashSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                crashGuard = checked;
                refreshStatus();
            }
        });
        memorySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                memoryLog = checked;
                refreshStatus();
            }
        });
        options.addView(crashSwitch);
        options.addView(memorySwitch);
        root.addView(card("Защита", options));

        Button save = button("Сохранить конфиг через root", true);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveConfigWithRoot();
            }
        });
        root.addView(save);

        Button reload = button("Перечитать конфиг", false);
        reload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                loadConfigFromRoot();
                buildUi();
                toast("Конфиг перечитан");
            }
        });
        root.addView(reload);

        TextView help = desc("После изменения режима полностью останови ETG и открой заново. LSPosed scope должен быть только com.exteragram.messenger.");
        help.setPadding(0, dp(12), 0, dp(4));
        root.addView(help);

        setContentView(scroll);
    }

    private void refreshStatus() {
        if (statusText == null) return;
        String modeText;
        if ("allow".equals(pythonMode)) modeText = "Python: allow, менеджер почти не вмешивается";
        else if ("block".equals(pythonMode)) modeText = "Python: block, максимальная экономия";
        else modeText = "Python: guard, рекомендованный режим";
        statusText.setText(
                modeText + "\n" +
                "Crash guard: " + onOff(crashGuard) + "\n" +
                "Memory log: " + onOff(memoryLog) + "\n" +
                "Config: " + CONFIG_PATH
        );
    }

    private void loadConfigFromRoot() {
        String out = su("cat " + CONFIG_PATH + " 2>/dev/null");
        if (out == null || out.trim().isEmpty()) return;
        String[] lines = out.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            int i = line.indexOf('=');
            if (i <= 0) continue;
            String key = line.substring(0, i).trim();
            String val = line.substring(i + 1).trim().toLowerCase();
            if ("python_mode".equals(key)) {
                if ("allow".equals(val) || "guard".equals(val) || "block".equals(val)) pythonMode = val;
            } else if ("enable_crash_guard".equals(key)) {
                crashGuard = parseBool(val, crashGuard);
            } else if ("enable_memory_log".equals(key)) {
                memoryLog = parseBool(val, memoryLog);
            }
        }
    }

    private void saveConfigWithRoot() {
        String cfg = "python_mode=" + pythonMode + "\n" +
                "enable_crash_guard=" + crashGuard + "\n" +
                "enable_memory_log=" + memoryLog + "\n";
        String escaped = cfg.replace("'", "'\\''");
        String cmd = "mkdir -p /data/adb/etgmanager && printf '" + escaped + "' > " + CONFIG_PATH + " && chmod 644 " + CONFIG_PATH + " && echo ETGMANAGER_OK";
        String out = su(cmd);
        if (out != null && out.contains("ETGMANAGER_OK")) {
            toast("Сохранено. Перезапусти ETG.");
        } else {
            toast("Root не дал сохранить конфиг");
        }
        refreshStatus();
    }

    private String su(String command) {
        StringBuilder sb = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            while ((line = er.readLine()) != null) sb.append(line).append('\n');
            p.waitFor();
        } catch (Throwable t) {
            return "";
        } finally {
            if (p != null) p.destroy();
        }
        return sb.toString();
    }

    private boolean parseBool(String value, boolean def) {
        if ("1".equals(value) || "true".equals(value) || "yes".equals(value) || "on".equals(value)) return true;
        if ("0".equals(value) || "false".equals(value) || "no".equals(value) || "off".equals(value)) return false;
        return def;
    }

    private LinearLayout card(String title, View content) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.rgb(230, 232, 238));
        box.setBackground(bg);
        TextView t = text(title, 18, true, Color.rgb(24, 28, 40));
        t.setPadding(0, 0, 0, dp(8));
        box.addView(t);
        box.addView(content);
        return box;
    }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private TextView desc(String s) {
        TextView v = text(s, 14, false, Color.rgb(100, 106, 120));
        v.setPadding(0, 0, 0, dp(8));
        return v;
    }

    private RadioButton radio(String s) {
        RadioButton r = new RadioButton(this);
        r.setText(s);
        r.setTextSize(15f);
        r.setTextColor(Color.rgb(35, 39, 50));
        r.setId(View.generateViewId());
        r.setPadding(0, dp(4), 0, dp(4));
        return r;
    }

    private Switch sw(String title, String subtitle, boolean checked) {
        Switch s = new Switch(this);
        s.setText(title + "\n" + subtitle);
        s.setTextSize(15f);
        s.setTextColor(Color.rgb(35, 39, 50));
        s.setChecked(checked);
        s.setPadding(0, dp(6), 0, dp(6));
        return s;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15f);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        if (primary) {
            bg.setColor(BLUE);
            b.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), Color.rgb(220, 224, 236));
            b.setTextColor(Color.rgb(35, 39, 50));
        }
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.setMargins(0, 0, 0, dp(10));
        b.setLayoutParams(lp);
        return b;
    }

    private String onOff(boolean v) { return v ? "on" : "off"; }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
