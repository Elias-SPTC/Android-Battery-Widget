package com.em.batterywidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.JobIntentService;

/**
 * Um JobIntentService para lidar com tarefas assíncronas de atualização de widgets.
 */
public class UpdateService extends JobIntentService {

    private static final String TAG = UpdateService.class.getSimpleName();
    public static final String ACTION_BATTERY_CHANGED = "com.em.batterywidget.action.BATTERY_CHANGED";
    public static final String ACTION_BATTERY_LOW = "com.em.batterywidget.action.BATTERY_LOW";
    public static final String ACTION_BATTERY_OKAY = "com.em.batterywidget.action.BATTERY_OKAY";
    public static final String ACTION_WIDGET_UPDATE = "com.em.batterywidget.action.WIDGET_UPDATE";

    public static final String EXTRA_WIDGET_IDS = "com.em.batterywidget.extra.WIDGET_IDS";
    private static final int JOB_ID = 1000;

    /**
     * Método auxiliar para enfileirar o trabalho neste JobIntentService.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, UpdateService.class, JOB_ID, work);
    }

    /**
     * @param intent
     * @see JobIntentService
     */
    @Override
    protected void onHandleWork(Intent intent) {
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();

        if (ACTION_BATTERY_CHANGED.equals(action)) {
            handleBatteryChanged(intent);
        } else if (ACTION_BATTERY_LOW.equals(action)) {
            Log.d(TAG, "Battery low action received (TODO)");
        } else if (ACTION_BATTERY_OKAY.equals(action)) {
            handleBatteryOkay();
        } else if (ACTION_WIDGET_UPDATE.equals(action)) {
            handleWidgetUpdate(intent);
        }
    }

    /**
     * Lida com a mudança de estado da bateria, atualizando o widget e o DB.
     */
    private void handleBatteryChanged(Intent intent) {

        // Pega o Intent "sticky" do sistema, que contém os extras de status da bateria.
        Intent batteryStatusIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Instancia o BatteryInfo usando o Intent de status do sistema.
        BatteryInfo newBatteryInfo = new BatteryInfo(batteryStatusIntent);

        // --- LOG DE DIAGNÓSTICO: Verifica os dados REAIS obtidos do sistema ---
        Log.d(TAG, "--- UpdateService: Lendo status da bateria do SISTEMA ---");
        Log.d(TAG, "  System Level: " + newBatteryInfo.getLevel() + "%");
        Log.d(TAG, "  System Voltage: " + newBatteryInfo.getVoltage() + " mV");
        Log.d(TAG, "  System Temperature: " + newBatteryInfo.getTemperature() + " (decimos C)");
        Log.d(TAG, "  System Status: " + newBatteryInfo.getStatus());
        Log.d(TAG, "------------------------------------------------------");

        final int level = newBatteryInfo.getLevel();
        final boolean isCharging = newBatteryInfo.isCharging();

        RemoteViews remoteViews = createRemoteViews(level, isCharging);
        ComponentName componentName = new ComponentName(this, BatteryWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(componentName, remoteViews);

        // Acesso ao SharedPreferences (Leitura)
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        BatteryInfo oldBatteryInfo = new BatteryInfo(sharedPreferences);

        // 1. Lógica de Inserção no Banco de Dados
        if (oldBatteryInfo.getLevel() != newBatteryInfo.getLevel()) {
            Database database = null;
            try {
                database = new Database(this);
                // Nota: Assumindo que DatabaseEntry aceita o nível da bateria
                // É necessário que Database.DatabaseEntry exista
                database.openWrite().insert(new DatabaseEntry(newBatteryInfo.getLevel()));
            } catch (Exception e) {
                Log.e(TAG, "Erro ao inserir no banco de dados", e);
            } finally {
                if (database != null) {
                    database.close();
                }
            }
        }

        // 2. Salvamento no SharedPreferences (Escrita)
        newBatteryInfo.saveToSharedPreferences(sharedPreferences);

        // --- LOG DE DIAGNÓSTICO: Confirma o salvamento no cache ---
        Log.d(TAG, "UpdateService: Dados da bateria SALVOS no SharedPreferences.");
    }

    /**
     * Lida com a ação de Bateria OK.
     */
    private void handleBatteryOkay() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(1500);
        }
    }

    /**
     * Lida com a atualização manual do widget.
     */
    private void handleWidgetUpdate(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        BatteryInfo batteryInfo = new BatteryInfo(sharedPreferences);
        final int level = batteryInfo.getLevel();
        final boolean isCharging = batteryInfo.isCharging();

        RemoteViews remoteViews = createRemoteViews(level, isCharging);
        final int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);

        if (widgetIds != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            appWidgetManager.updateAppWidget(widgetIds, remoteViews);
        }
    }

    /**
     * Cria o objeto RemoteViews para a view do widget.
     * CORRIGIDO para gerenciar os 10 ImageViews de porcentagem do XML (percent10 a percent100).
     */
    private RemoteViews createRemoteViews(final int level, final boolean isCharging) {

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_view);

        // 1. Array de IDs de imagem no XML (de 10% a 100%)
        int[] percentImageViewIds = new int[] {
                R.id.percent10, R.id.percent20, R.id.percent30, R.id.percent40,
                R.id.percent50, R.id.percent60, R.id.percent70, R.id.percent80,
                R.id.percent90, R.id.percent100
        };

        // 2. Oculta todos os 10 ImageViews de porcentagem e a imagem base
        remoteViews.setViewVisibility(R.id.battery_view, View.INVISIBLE);
        for (int id : percentImageViewIds) {
            remoteViews.setViewVisibility(id, View.INVISIBLE);
        }

        // 3. Determina qual ImageView deve ser visível
        int percentageIndex = level / 10;
        if (percentageIndex > 0 && percentageIndex <= 10) {
            // Se o nível for 10% ou mais, mostra o ImageView correspondente (0-based index)
            remoteViews.setViewVisibility(percentImageViewIds[percentageIndex - 1], View.VISIBLE);
        } else {
            // Se o nível for 0% a 9%, mostra a imagem base (R.id.battery_view)
            remoteViews.setViewVisibility(R.id.battery_view, View.VISIBLE);
        }

        // 4. Lógica de carregamento
        // Use View.GONE para remover o espaço quando não estiver carregando.
        remoteViews.setViewVisibility(R.id.charge_view, isCharging ?
                View.VISIBLE : View.GONE);

        // 5. Lógica do texto
        remoteViews.setTextViewText(R.id.batterytext, String.valueOf(level) + "%");

        // 6. Intent para abrir a activity
        Intent activityIntent = new Intent(this, WidgetActivity.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, flags);
        remoteViews.setOnClickPendingIntent(R.id.widget_view, pendingIntent);

        return remoteViews;
    }

    /**
     * Função para obter o recurso drawable da bateria baseado no nível.
     * REMOVIDA A LÓGICA DE ÍCONE, POIS É GERENCIADA POR VISIBILIDADE EM createRemoteViews.
     */
    private int getBatteryIconResource(int level, boolean isCharging) {
        // MANTIDO como placeholder se outras partes do código ainda chamarem, mas a lógica principal está em createRemoteViews
        if (isCharging) {
            return android.R.drawable.ic_lock_idle_charging;
        }
        return android.R.drawable.presence_online;
    }
}