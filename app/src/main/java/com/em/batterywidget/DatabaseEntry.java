package com.em.batterywidget;

import java.util.Date;

/**
 * Representa uma entrada de histórico do nível da bateria, garantindo imutabilidade.
 */
public final class DatabaseEntry {

    private final long time;
    private final int level;

    /**
     * Construtor principal. Cria uma nova entrada com o timestamp atual.
     * @param level O nível de bateria (0-100).
     */
    public DatabaseEntry(int level) {
        // Uso de System.currentTimeMillis() é mais eficiente que new Date().getTime()
        this.time = System.currentTimeMillis();
        this.level = level;
    }

    /**
     * Construtor para recriar uma entrada a partir de dados lidos da base de dados.
     * @param time O timestamp (em milissegundos) do registo.
     * @param level O nível de bateria (0-100).
     */
    public DatabaseEntry(long time, int level) {
        this.time = time;
        this.level = level;
    }

    public long getTime() {
        return time;
    }

    public int getLevel() {
        return level;
    }
}