package com.em.batterywidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Arrays;

public class BatteryGraphWidget extends AppWidgetProvider {

    private static final String TAG = BatteryGraphWidget.class.getSimpleName();

    /**
     * Helper method to get the number of installed widgets for this AppWidgetProvider.
     * (CRÍTICO para a lógica de parada do MonitorService no BatteryWidget.java)
     */
    public static int getNumberOfWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        // CRÍTICO: Usa BatteryGraphWidget.class para encontrar as instâncias corretas
        ComponentName componentName = new ComponentName(context, BatteryGraphWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        return appWidgetIds.length;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate (Graph Widget): IDs para atualização: " + Arrays.toString(appWidgetIds));

        if (appWidgetIds != null && appWidgetIds.length > 0) {
            // Enfileira o UpdateService para desenhar o gráfico
            Intent updateIntent = new Intent(context, UpdateService.class);
            updateIntent.setAction(UpdateService.ACTION_WIDGET_UPDATE_GRAPH_ONLY);
            updateIntent.putExtra(UpdateService.EXTRA_WIDGET_IDS, appWidgetIds);
            UpdateService.enqueueWork(context, updateIntent);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled (Graph Widget): Primeiro widget de gráfico adicionado.");
        // Não inicia o MonitorService aqui porque BatteryWidget é responsável por isso.
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled (Graph Widget): Último widget de gráfico removido.");
        // A lógica de parada está centralizada no onDisabled do BatteryWidget.java.
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d(TAG, "onDeleted (Graph Widget): IDs excluídos: " + Arrays.toString(appWidgetIds));
    }
}