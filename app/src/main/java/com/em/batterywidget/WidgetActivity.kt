package com.em.batterywidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup; // <--- CORREÇÃO 1: IMPORTAÇÃO OBRIGATÓRIA!
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.database.Cursor;

import com.em.batterywidget.Database;


public class WidgetActivity extends AppCompatActivity {

    private static final String TAG = WidgetActivity.class.getSimpleName();

    // VISTAS DA UI
    private ImageView mGraphImageView;

    private TextView mStateTextView;
    private TextView mPlugTextView;
    private TextView mLevelTextView;
    private TextView mScaleTextView;
    private TextView mVoltageTextView;
    private TextView mTempTextView;
    private TextView mTechnologyTextView;
    private TextView mHealthTextView;

    // TextView para mensagem de "Sem Dados" se o gráfico estiver vazio
    private TextView mNoDataTextView;

    private SharedPreferences mSharedPreferences;

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                Log.d(TAG, "Activity recebeu um ACTION_BATTERY_CHANGED (em tempo real)");

                BatteryInfo batteryInfo = new BatteryInfo(intent);
                populateTable(batteryInfo);

                if (mSharedPreferences != null) {
                    batteryInfo.saveToSharedPreferences(mSharedPreferences);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);

        Log.d(TAG, "Activity onCreate iniciado.");

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Inicialização e busca de UI
        try {
            // ID 'chart' agora é uma ImageView, não um LinearLayout
            mGraphImageView = findViewById(R.id.chart);

            // Adicionando um TextView para a mensagem de "Sem Dados"
            mNoDataTextView = new TextView(this);
            mNoDataTextView.setText("Sem dados de histórico para o gráfico.");
            mNoDataTextView.setGravity(Gravity.CENTER);
            mNoDataTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mNoDataTextView.setVisibility(View.GONE);


            mStateTextView = findViewById(R.id.state);
            mPlugTextView = findViewById(R.id.plug);
            mLevelTextView = findViewById(R.id.level);
            mScaleTextView = findViewById(R.id.scale);
            mVoltageTextView = findViewById(R.id.voltage);
            mTempTextView = findViewById(R.id.temperature);
            mTechnologyTextView = findViewById(R.id.technology);
            mHealthTextView = findViewById(R.id.health);

            Log.d(TAG, "IDs de layout encontrados.");
        } catch (Exception e) {
            Log.e(TAG, "Falha CRÍTICA ao encontrar IDs de layout da tabela.", e);
        }

        // Carrega o status inicial do cache
        loadAndDisplayCurrentBatteryStatus();

        // CORREÇÃO: Adicionando chamada para inserir dados de teste (do Database.java)
        Database.checkAndInsertMockData(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        Log.d(TAG, "Receiver de bateria (Activity) registrado.");

        loadAndDisplayCurrentBatteryStatus();
        drawGraphAsync(); // Chamado em onResume
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryReceiver);
        Log.d(TAG, "Receiver de bateria (Activity) desregistrado.");
    }

    /**
     * Carrega os dados do SharedPreferences (cache) e preenche a tabela.
     */
    private void loadAndDisplayCurrentBatteryStatus() {
        Log.d(TAG, "Carregando dados do SharedPreferences (cache)...");
        BatteryInfo batteryInfo = new BatteryInfo(mSharedPreferences);
        populateTable(batteryInfo);
    }

    /**
     * Preenche a tabela da UI com os dados de um objeto BatteryInfo.
     */
    private void populateTable(BatteryInfo batteryInfo) {
        if (mLevelTextView == null) {
            Log.w(TAG, "populateTable chamado, mas as Views da tabela são nulas. Pulando.");
            return;
        }

        // CORREÇÃO: Estes métodos agora existem abaixo!
        mStateTextView.setText(getStatusString(batteryInfo.getStatus()));
        mPlugTextView.setText(getPlugString(batteryInfo.getPlugged()));
        mLevelTextView.setText(batteryInfo.getLevel() + "%");
        mScaleTextView.setText(String.valueOf(batteryInfo.getScale()));
        mVoltageTextView.setText(batteryInfo.getVoltage() + " mV");
        mTempTextView.setText(getTemperatureString(batteryInfo.getTemperature()));
        mTechnologyTextView.setText(batteryInfo.getTechnology());
        mHealthTextView.setText(getHealthString(batteryInfo.getHealth()));

        Log.d(TAG, "Tabela da UI atualizada. Nível: " + batteryInfo.getLevel() + "%");
    }

    // --- CORREÇÃO 2: MÉTODOS AUXILIARES DE FORMATAÇÃO DE STRING INSERIDOS ---

    private String getStatusString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "Carregando";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "Descarregando";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "Completa";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "Não está carregando";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return "Desconhecido";
        }
    }

    private String getPlugString(int plugged) {
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "Tomada (AC)";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "Sem fio (Wireless)";
            case 0: // Significa desplugado
                return "Desconectado";
            default:
                return "Desconhecido";
        }
    }

    private String getTemperatureString(int temperature) {
        // A temperatura é dada em décimos de grau Celsius (ex: 300 = 30.0°C)
        if (temperature == 0) return "N/A";
        float tempCelsius = (float) temperature / 10.0f;
        return String.format("%.1f °C", tempCelsius);
    }

    private String getHealthString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Boa";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Superaquecida";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Morta";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Fria";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Sobretensão";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Falha";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            default:
                return "Desconhecida";
        }
    }

    // --- LÓGICA DO GRÁFICO (CORRIGIDA E MODERNIZADA) ---

    private void drawGraphAsync() {
        Log.d(TAG, "drawGraphAsync() chamado.");
        if (mGraphImageView == null) {
            Log.w(TAG, "mGraphImageView é nulo, pulando o desenho do gráfico.");
            return;
        }

        UpdateService updateService = new UpdateService();

        mGraphImageView.setVisibility(View.GONE);
        if (mNoDataTextView != null) mNoDataTextView.setVisibility(View.GONE);


        new Thread(() -> {

            // 1. Gera o Bitmap do gráfico.
            // O createGraphBitmap em UpdateService já lida com o BD internamente.
            Bitmap graphBitmap = updateService.createGraphBitmap(WidgetActivity.this);

            // Verifica se o Bitmap foi criado e contém dados válidos
            final boolean hasData = (graphBitmap != null && graphBitmap.getWidth() > 1 && graphBitmap.getHeight() > 1);


            // Atualiza a UI na thread principal
            runOnUiThread(() -> {

                if (hasData) {
                    Log.d(TAG, "Gráfico gerado com sucesso. Exibindo...");

                    // Define o Bitmap DIRETAMENTE no ImageView
                    mGraphImageView.setImageBitmap(graphBitmap);
                    mGraphImageView.setScaleType(ImageView.ScaleType.FIT_CENTER); // Usa fitCenter para ajuste elegante
                    mGraphImageView.setVisibility(View.VISIBLE);
                    if (mNoDataTextView != null) mNoDataTextView.setVisibility(View.GONE);

                } else {
                    Log.w(TAG, "Nenhuma entrada no BD para desenhar o gráfico (Bitmap nulo/vazio).");
                    mGraphImageView.setVisibility(View.GONE);
                    mGraphImageView.setImageBitmap(null);
                }
            });
        }).start();
    }
}