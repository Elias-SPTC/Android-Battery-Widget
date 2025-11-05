package com.em.batterywidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Este BroadcastReceiver garante que o MonitorService seja iniciado após a inicialização
 * do dispositivo, desde que haja pelo menos um widget ativo.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BootReceiver.class.getSimpleName();

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.d(TAG, "BOOT_COMPLETED recebido. Verificando widgets ativos.");

            // Verifica se há widgets ativos
            if (BatteryWidget.getNumberOfWidgets(context) > 0) {
                Log.d(TAG, "Widgets ativos encontrados. Iniciando MonitorService.");

                Intent serviceIntent = new Intent(context, MonitorService.class);

                // *** CORREÇÃO CRÍTICA: Usar startForegroundService para API 26+ ***
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Requer startForegroundService para iniciar um serviço em background
                    context.startForegroundService(serviceIntent);
                    Log.d(TAG, "MonitorService iniciado via startForegroundService().");
                } else {
                    context.startService(serviceIntent);
                    Log.d(TAG, "MonitorService iniciado via startService().");
                }
            } else {
                Log.d(TAG, "Nenhum widget ativo. MonitorService não será iniciado.");
            }
        }

        // NOTA: O Receiver no AndroidManifest.xml está registrado para BATTERY_CHANGED.
        // No entanto, é mais eficiente deixar o MonitorService, uma vez iniciado,
        // lidar com ACTION_BATTERY_CHANGED (como ele já faz), evitando a dupla execução.
        // Portanto, a lógica adicional para BATTERY_CHANGED é omitida aqui.
    }
}