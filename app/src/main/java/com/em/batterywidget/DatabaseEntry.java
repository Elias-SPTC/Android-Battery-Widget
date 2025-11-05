package com.em.batterywidget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Representa uma única entrada de dados de bateria armazenada na base de dados (SQLite).
 */
public class DatabaseEntry {
    // Colunas da base de dados (correspondendo aos nomes de coluna em Database.java)
    public static final String KEY_TIME = "time_stamp";
    public static final String KEY_LEVEL = "battery_level";
    public static final String KEY_STATUS = "status";
    public static final String KEY_PLUGGED = "plugged";
    public static final String KEY_VOLTAGE = "voltage"; // NOVO: Tensão em mV
    public static final String KEY_HEALTH = "health";   // NOVO: Código de saúde

    private long timestamp;
    private int level;
    private int status;
    private int plugged;
    private int voltage; // NOVO
    private int health;  // NOVO

    /**
     * Construtor principal usado para construir objetos a partir da base de dados ou para inserção completa.
     * Inclui os campos voltage e health.
     */
    public DatabaseEntry(long timestamp, int level, int status, int plugged, int voltage, int health) {
        this.timestamp = timestamp;
        this.level = level;
        this.status = status;
        this.plugged = plugged;
        this.voltage = voltage;
        this.health = health;
    }

    /**
     * Construtor auxiliar (para inserção de dados de demonstração no Database.java,
     * onde apenas nível e timestamp são fornecidos, assumindo status, plugged, voltage, health como 0 e HEALTH_UNKNOWN (1)).
     */
    public DatabaseEntry(long timestamp, int level) {
        // Assume defaults: 0V, HEALTH_UNKNOWN (código 1)
        this(timestamp, level, 0, 0, 0, 1);
    }

    /**
     * Construtor de fallback.
     */
    public DatabaseEntry() {
        this(System.currentTimeMillis(), 0, 0, 0, 0, 1);
    }

    // Getters
    public long getTimestamp() {
        return timestamp;
    }

    public long getTime() {
        return getTimestamp();
    }

    public int getLevel() {
        return level;
    }

    public int getStatus() {
        return status;
    }

    public int getPlugged() {
        return plugged;
    }

    // NOVO: Getters para Tensão e Saúde
    public int getVoltage() {
        return voltage;
    }

    public int getHealth() {
        return health;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public boolean isCharging() {
        return plugged != 0;
    }

    @Override
    public String toString() {
        return "DatabaseEntry [Time=" + getFormattedTime() + ", Level=" + level + "%, Tensão=" + voltage + "mV, Saúde=" + health + "]";
    }
}