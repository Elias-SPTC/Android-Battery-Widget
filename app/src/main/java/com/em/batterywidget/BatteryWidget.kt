package com.em.batterywidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Arrays;

/**
 * Lida com o ciclo de vida e eventos do widget da PILHA/INDICADOR.
 * * Este Provider foi simplificado para apenas solicitar a atualização
 * do desenho da bateria e do texto de status via UpdateService.
 */
public class BatteryWidget extends AppWidgetProvider {

    private static final String TAG = BatteryWidget.class.getSimpleName();

    /**
     * Helper method to get the number of installed widgets for this AppWidgetProvider.
     */
    public static int getNumberOfWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, BatteryWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        return appWidgetIds.length;
    }

    /**
     * Chamado para fornecer RemoteViews para o widget especificado (Pilha + Status).
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.d(TAG, "onUpdate (Battery Widget): IDs para atualização: " + Arrays.toString(appWidgetIds));

        if (appWidgetIds != null && appWidgetIds.length > 0) {

            // Enfileira o UpdateService para desenhar o conteúdo dinâmico (Bateria e Texto).
            // O uso de JobIntentService é essencial para não bloquear a UI.
            Intent updateIntent = new Intent(context, UpdateService.class);
            updateIntent.setAction(UpdateService.ACTION_WIDGET_UPDATE);

            // Passa os IDs fornecidos pelo sistema
            updateIntent.putExtra(UpdateService.EXTRA_WIDGET_IDS, appWidgetIds);

            UpdateService.enqueueWork(context, updateIntent);
        }
    }

    /**
     * Chamado quando um ou mais widgets são excluídos do host.
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d(TAG, "onDeleted (Battery Widget): Widget(s) IDs removidos: " + Arrays.toString(appWidgetIds));
    }

    /**
     * Chamado quando o primeiro widget da aplicação é adicionado ao host.
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled (Battery Widget): Iniciando MonitorService.");

        // Inicia o serviço de monitoramento (que alimenta os dados de ambos os widgets)
        Intent monitorIntent = new Intent(context, MonitorService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(monitorIntent);
        } else {
            context.startService(monitorIntent);
        }
    }

    /**
     * Chamado quando o último widget da aplicação é removido do host.
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled (Battery Widget): Verificando se MonitorService pode ser parado.");

        // Verifica se ainda existe algum widget ativo (incluindo o Graph Widget)
        if (getNumberOfWidgets(context) == 0 && BatteryGraphWidget.getNumberOfWidgets(context) == 0) {
            Log.d(TAG, "onDisabled: Nenhum widget ativo. Parando MonitorService.");
            Intent monitorIntent = new Intent(context, MonitorService.class);
            context.stopService(monitorIntent);
        } else {
            Log.d(TAG, "onDisabled: Outros widgets ativos. MonitorService mantido.");
        }
    }
}