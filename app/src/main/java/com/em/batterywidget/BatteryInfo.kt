package com.em.batterywidget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryInfo {

    private static final String TAG = BatteryInfo.class.getSimpleName();

    // Constantes Padrão
    private static final int DEFAULT_LEVEL = 50;
    private static final int DEFAULT_STATUS = BatteryManager.BATTERY_STATUS_UNKNOWN;
    private static final int DEFAULT_PLUGGED = 0;

    // Campos de dados da bateria
    private int level;
    private int status;
    private int plugged;
    private int scale;
    private int voltage;
    private int temperature;
    private String technology;
    private int health;

    // Chaves para SharedPreferences (mantido mínimo para fallback do widget)
    private static final String PREF_LEVEL = "battery_level";
    private static final String PREF_STATUS = "battery_status";
    private static final String PREF_PLUGGED = "battery_plugged";

    // Construtor 1: Usado para Fallback do Widget (lê do SharedPreferences)
    public BatteryInfo(SharedPreferences prefs) {
        this.level = prefs.getInt(PREF_LEVEL, DEFAULT_LEVEL);
        this.status = prefs.getInt(PREF_STATUS, DEFAULT_STATUS);
        this.plugged = prefs.getInt(PREF_PLUGGED, DEFAULT_PLUGGED);

        // Valores default para campos não salvos no SP
        this.scale = 100;
        this.voltage = -1;
        this.temperature = -1;
        this.technology = "N/A";
        this.health = BatteryManager.BATTERY_HEALTH_UNKNOWN;
    }

    // Construtor 2: Usado para ler dados do sistema (ACTION_BATTERY_CHANGED Intent)
    public BatteryInfo(Intent intent) {

        // --- CORREÇÃO CRÍTICA PARA PERCENTUAL ERRADO ---
        this.scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, DEFAULT_LEVEL);

        if (this.scale > 0) {
            this.level = (int) ((rawLevel / (float) this.scale) * 100);
            if (this.level < 0) this.level = 0;
            if (this.level > 100) this.level = 100;
        } else {
            this.level = DEFAULT_LEVEL;
        }

        // Campos gerais
        this.status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, DEFAULT_STATUS);
        this.plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, DEFAULT_PLUGGED);

        // --- Adicionado campos para resolver erros de compilação em WidgetActivity.java ---
        this.voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        this.temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        this.technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        if (this.technology == null) this.technology = "N/A";
        this.health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
    }

    // --- MÉTODOS DE MANIPULAÇÃO DE DADOS ---

    // CRÍTICO: Método para resolver o erro em MonitorService.java
    public void saveToIntent(Intent intent) {
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level); // Usando o nível calculado (0-100)
        intent.putExtra(BatteryManager.EXTRA_SCALE, scale);
        intent.putExtra(BatteryManager.EXTRA_STATUS, status);
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, plugged);
        intent.putExtra(BatteryManager.EXTRA_VOLTAGE, voltage);
        intent.putExtra(BatteryManager.EXTRA_TEMPERATURE, temperature);
        intent.putExtra(BatteryManager.EXTRA_TECHNOLOGY, technology);
        intent.putExtra(BatteryManager.EXTRA_HEALTH, health);
        Log.d(TAG, "BatteryInfo salvo no Intent para UpdateService.");
    }

    public void saveToSharedPreferences(SharedPreferences prefs) {
        prefs.edit()
                .putInt(PREF_LEVEL, this.level)
                .putInt(PREF_STATUS, this.status)
                .putInt(PREF_PLUGGED, this.plugged)
                .apply();
        Log.d(TAG, "BatteryInfo salvo no SP: " + this.level + "%, Status: " + this.status);
    }

    // --- GETTERS (RESOLVEM ERROS EM WidgetActivity.java) ---
    public int getLevel() {
        return level;
    }

    public int getStatus() {
        return status;
    }

    public int getPlugged() {
        return plugged;
    }

    public boolean isCharging() {
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    // Getters adicionados:
    public int getScale() {
        return scale;
    }

    public int getVoltage() {
        return voltage;
    }

    public int getTemperature() {
        return temperature;
    }

    public String getTechnology() {
        return technology;
    }

    public int getHealth() {
        return health;
    }
}