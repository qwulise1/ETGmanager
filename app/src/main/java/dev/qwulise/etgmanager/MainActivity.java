package dev.qwulise.etgmanager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setPadding(42, 42, 42, 42);
        tv.setTextSize(15f);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setText(
                "ETGmanager v0.1.0\n\n" +
                "LSPosed module for ExteraGram.\n\n" +
                "Что делает сейчас:\n" +
                "• цепляется к com.exteragram.messenger\n" +
                "• по умолчанию блокирует Python plugin engine ETG\n" +
                "• не даёт Chaquopy/Python-плагинам стартовать\n" +
                "• пишет memory/crash logs в LSPosed/Xposed logs\n" +
                "• не добавляет свои DEX-плагины и не грузит Python\n\n" +
                "После установки:\n" +
                "1. Включи модуль в LSPosed.\n" +
                "2. Scope: com.exteragram.messenger.\n" +
                "3. Полностью останови ETG и открой заново.\n\n" +
                "Конфиг root-путём:\n" +
                "/data/adb/etgmanager/config.properties\n\n" +
                "Пример:\n" +
                "disable_python_engine=true\n" +
                "enable_crash_guard=true\n" +
                "enable_memory_log=true\n"
        );
        setContentView(tv);
    }
}
