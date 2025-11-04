package com.em.batterywidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.BatteryManager;
import android.content.Intent; // Importação adicionada para Intent

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Atividade para exibir um gráfico do histórico da bateria e todos os detalhes técnicos.
 */
public class WidgetActivity extends Activity {

    private static final String TAG = "BatteryWidgetActivity";
    // Formato para temperatura (Celsius com uma casa decimal)
    private static final DecimalFormat TEMP_FORMATTER = new DecimalFormat("0.0");

    // Componentes do AChartEngine
    private LinearLayout mGraphLayout;
    private GraphicalView mGraphicalView;
    private XYSeries mXYSeries;

    // UI elements for showing status
    private TextView mStatusTextView;
    private TextView mStateTextView;
    private TextView mPlugTextView;
    private TextView mLevelTextView;
    private TextView mScaleTextView;
    private TextView mVoltageTextView;
    private TextView mTempTextView;
    private TextView mTechnologyTextView;
    private TextView mHealthTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);

        Log.d(TAG, "Activity onCreate iniciado com o layout de detalhes (activity_widget).");

        // Inicialização e busca de UI
        try {
            // Contêiner do Gráfico (ID: chart)
            mGraphLayout = findViewById(R.id.chart);
            // TextView para Status de Carregamento/Mensagens (ID: status_text)
            mStatusTextView = findViewById(R.id.status_text);

            // Campos de Status da Tabela (IDs Corrigidos/Novos)
            mStateTextView = findViewById(R.id.state);
            mPlugTextView = findViewById(R.id.plug);
            mLevelTextView = findViewById(R.id.level);
            mScaleTextView = findViewById(R.id.scale);
            mVoltageTextView = findViewById(R.id.voltage);
            mTempTextView = findViewById(R.id.temperature);
            mTechnologyTextView = findViewById(R.id.technology);
            mHealthTextView = findViewById(R.id.health);

            Log.d(TAG, "IDs de layout procurados. mGraphLayout encontrado? " + (mGraphLayout != null));
        } catch (Exception e) {
            Log.e(TAG, "Falha CRÍTICA ao encontrar IDs de layout da tabela. Verifique se o novo 'activity_widget.xml' contém os IDs: chart, state, plug, level, etc.", e);
        }

        // --- NOVO: FORÇA A EXECUÇÃO DO UPDATESERVICE PARA GARANTIR DADOS FRESCOS NO CACHE ---
        Log.d(TAG, "Forçando execução do UpdateService para garantir dados frescos no cache.");
        Intent serviceIntent = new Intent(this, UpdateService.class);
        serviceIntent.setAction(UpdateService.ACTION_BATTERY_CHANGED);
        UpdateService.enqueueWork(this, serviceIntent);
        // ----------------------------------------------------------------------------------

        // 1. Carrega o status atual da bateria e preenche a Tabela.
        // Nota: A chamada aqui é imediata, mas o UpdateService pode ser mais lento.
        // Se ainda for 0%, precisamos adicionar um delay ou BroadcastReceiver na Activity.
        loadAndDisplayCurrentBatteryStatus();

        // Garante que a base de dados e os dados de teste existam para fins de teste de renderização.
        try {
            Database.checkAndInsertMockData(this);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao tentar inserir dados de teste. Pode ser problema de inicialização da DB.", e);
        }

        // Inicia o carregamento de dados e desenho do gráfico de forma assíncrona
        drawGraphAsync();
    }

    /**
     * Carrega a última BatteryInfo salva (do SharedPreferences) e preenche a Tabela de Detalhes.
     */
    private void loadAndDisplayCurrentBatteryStatus() {
        // Assume que a última info foi salva pelo UpdateService no SharedPreferences padrão.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        BatteryInfo batteryInfo = new BatteryInfo(sharedPreferences);

        // --- LOG DE DIAGNÓSTICO: Verifica os dados LIDOS do cache (SharedPreferences) ---
        Log.d(TAG, "--- Activity: Lendo dados do SharedPreferences (CACHE) ---");
        Log.d(TAG, "  Cache Level: " + batteryInfo.getLevel() + "%");
        Log.d(TAG, "  Cache Voltage: " + batteryInfo.getVoltage() + " mV");
        Log.d(TAG, "  Cache Temperature: " + batteryInfo.getTemperature() + " (decimos C)");
        Log.d(TAG, "  Cache Health: " + getHealthString(batteryInfo.getHealth()));
        Log.d(TAG, "--------------------------------------------------------");

        updateBatteryStatus(mStateTextView, getBatteryStatusString(batteryInfo.getStatus()));
        updateBatteryStatus(mPlugTextView, getPluggedString(batteryInfo.getPlugged()));
        updateBatteryStatus(mLevelTextView, batteryInfo.getLevel() + "%");
        updateBatteryStatus(mScaleTextView, batteryInfo.getScale() + "");
        updateBatteryStatus(mVoltageTextView, batteryInfo.getVoltage() + " mV");
        updateBatteryStatus(mTempTextView, convertTemperature(batteryInfo.getTemperature()) + " °C");
        updateBatteryStatus(mTechnologyTextView, batteryInfo.getTechnology());
        updateBatteryStatus(mHealthTextView, getHealthString(batteryInfo.getHealth()));
    }

    /**
     * Helper para converter o código de status da bateria para uma string legível.
     */
    private String getBatteryStatusString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "Not Charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return "Unknown";
        }
    }

    /**
     * Helper para converter o código de plugged (conectado) para uma string legível.
     */
    private String getPluggedString(int plugged) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            return "Wireless";
        }

        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "AC";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB";
            default:
                return "Unplugged";
        }
    }

    /**
     * Helper para converter o código de saúde da bateria para uma string legível.
     */
    private String getHealthString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Good";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Cold";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Overheat";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Failure";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            default:
                return "Unknown";
        }
    }

    /**
     * Converte a temperatura de décimos de grau Celsius para Celsius e formata.
     */
    private String convertTemperature(int temperature) {
        double tempInC = temperature / 10.0;
        return TEMP_FORMATTER.format(tempInC);
    }

    /**
     * Helper para atualizar TextViews com verificação de nulidade.
     */
    private void updateBatteryStatus(TextView textView, String value) {
        if (textView != null) {
            textView.setText(value);
        }
    }

    /**
     * Inicializa a view do gráfico (AChartEngine GraphicalView) e o Renderer.
     */
    private void initGraphicalView() {
        if (mGraphLayout == null) {
            Log.e(TAG, "initGraphicalView abortado: mGraphLayout é NULL (ID R.id.chart).");
            return;
        }

        if (mGraphicalView == null) {
            Log.d(TAG, "Inicializando GraphicalView (AChartEngine).");

            // 1. Configuração do Renderer (Visual)
            XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

            // Definições visuais básicas
            renderer.setYAxisMax(100);
            renderer.setYAxisMin(0);
            renderer.setAxisTitleTextSize(20);
            renderer.setChartTitleTextSize(24);
            renderer.setLabelsTextSize(18);
            renderer.setLegendTextSize(18);

            // Configurações de interação
            renderer.setPanEnabled(true, false);
            renderer.setZoomEnabled(true, false);
            renderer.setZoomButtonsVisible(false);
            renderer.setShowGrid(true);

            // Cores
            renderer.setLabelsColor(Color.DKGRAY);
            renderer.setAxesColor(Color.GRAY);
            renderer.setMargins(new int[]{40, 40, 25, 10});
            renderer.setApplyBackgroundColor(true);
            renderer.setBackgroundColor(Color.TRANSPARENT);
            renderer.setMarginsColor(Color.argb(0, 0, 0, 0));

            // 2. Configuração do Data Set e Series (Dados)
            XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
            if (mXYSeries == null) {
                mXYSeries = new XYSeries("Histórico do Nível da Bateria");
            }
            dataSet.addSeries(mXYSeries);

            // 3. Configuração do Renderer da Série (Linha)
            XYSeriesRenderer xySeriesRenderer = new XYSeriesRenderer();
            xySeriesRenderer.setColor(Color.parseColor("#4CAF50")); // Cor verde vibrante
            xySeriesRenderer.setLineWidth(4);
            renderer.addSeriesRenderer(xySeriesRenderer);

            // 4. Criação da View do Gráfico
            mGraphicalView = ChartFactory.getTimeChartView(this, dataSet, renderer, "dd/MM/yy HH:mm");

            // Adiciona a view ao layout
            mGraphLayout.removeAllViews();
            mGraphLayout.addView(mGraphicalView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                mGraphLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            Log.d(TAG, "GraphicalView adicionado ao mGraphLayout.");
        }
    }

    /**
     * Utiliza AsyncTask para ler o banco de dados em um thread de fundo.
     */
    private void drawGraphAsync() {
        if (mStatusTextView != null) {
            mStatusTextView.setText("A carregar dados do histórico...");
        } else {
            Log.w(TAG, "mStatusTextView é NULL. Não é possível mostrar status de carregamento.");
        }

        new AsyncTask<Void, Void, Cursor>() {

            private Database database = null;

            @Override
            protected Cursor doInBackground(Void... params) {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    Log.d(TAG, "AsyncTask: Iniciando leitura da base de dados.");
                    database = new Database(WidgetActivity.this);
                    return database.openRead().getEntries();
                } catch (Exception e) {
                    Log.e(TAG, "AsyncTask: Erro CRÍTICO ao ler dados para o gráfico", e);
                    if (database != null) database.close();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                    Log.w(TAG, "onPostExecute abortado: Activity está a terminar/destruída.");
                    if (cursor != null) cursor.close();
                    if (database != null) database.close();
                    return;
                }

                try {
                    initGraphicalView();

                    if (mXYSeries != null) {
                        mXYSeries.clear();
                    }

                    int count = 0;
                    if (cursor != null && cursor.moveToFirst()) {
                        Log.d(TAG, "Dados da base de dados encontrados. Processando...");
                        do {
                            long time = cursor.getLong(Database.TIME);
                            int level = cursor.getInt(Database.LEVEL);
                            mXYSeries.add(time, level);
                            count++;
                        } while (cursor.moveToNext());

                        updateBatteryStatus(mStatusTextView, "Dados carregados com sucesso. Total de pontos: " + count);
                        Log.d(TAG, "Total de pontos de histórico carregados: " + count);

                    } else {
                        updateBatteryStatus(mStatusTextView, "Nenhum dado de histórico de bateria encontrado.");
                        Log.w(TAG, "Nenhum dado encontrado na base de dados para o gráfico.");
                    }

                    if (mGraphicalView != null) {
                        Log.d(TAG, "Repintando mGraphicalView...");
                        mGraphicalView.repaint();
                        Log.d(TAG, "Repintar concluído.");
                    } else {
                        Log.e(TAG, "ERRO: mGraphicalView é NULL após initGraphicalView().");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao desenhar gráfico após a leitura (Exceção de UI)", e);
                    updateBatteryStatus(mStatusTextView, "Erro ao exibir gráfico.");
                } finally {
                    if (cursor != null) {
                        cursor.close();
                        Log.d(TAG, "Cursor fechado.");
                    }
                    if (database != null) {
                        database.close();
                        Log.d(TAG, "Base de dados fechada.");
                    }
                }
            }
        }.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity onDestroy.");
        if (mGraphLayout != null) {
            mGraphLayout.removeAllViews();
        }
        mGraphicalView = null;
        mXYSeries = null;
    }
}