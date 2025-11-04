package com.em.batterywidget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gerenciador de SharedPreferences. Uso de 'apply()' para gravação assíncrona.
 */
public class Preferences {

    private SharedPreferences sharedPreferences;

    public Preferences(String preference, Context context) {
        // Usar Context.getApplicationContext() pode ajudar na consistência, mas Context.MODE_PRIVATE é seguro.
        sharedPreferences = context.getSharedPreferences(preference, Context.MODE_PRIVATE);
    }

    /**
     * Define um valor inteiro de forma assíncrona.
     * @param key Chave da preferência.
     * @param value Valor inteiro a ser salvo.
     */
    public void setValue(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        // CRÍTICO: Substituído commit() por apply() para gravação assíncrona (evita travamento da UI)
        editor.apply();
    }

    /**
     * Define um valor booleano de forma assíncrona.
     * @param key Chave da preferência.
     * @param value Valor booleano a ser salvo.
     */
    public void setValue(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Define um valor string de forma assíncrona.
     * @param key Chave da preferência.
     * @param value Valor string a ser salvo.
     */
    public void setValue(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public int getValue(String key, int value) {
        return sharedPreferences.getInt(key, value);
    }

    public boolean getValue(String key, boolean value) {
        return sharedPreferences.getBoolean(key, value);
    }

    public String getValue(String key, String value) {
        return sharedPreferences.getString(key, value);
    }

}