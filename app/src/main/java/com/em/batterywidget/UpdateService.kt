package com.em.batterywidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import androidx.core.app.JobIntentService;
import com.em.batterywidget.R;

import java.util.ArrayList;

/**
 * Um JobIntentService para lidar com tarefas assíncronas de atualização de widgets.
 */
public class UpdateService extends JobIntentService {

    private static final String TAG = UpdateService.class.getSimpleName();
    public static final String ACTION_BATTERY_CHANGED = "com.em.batterywidget.action.BATTERY_CHANGED";
    public static final String ACTION_BATTERY_LOW = "com.em.batterywidget.action.BATTERY_LOW";
    public static final String ACTION_BATTERY_OKAY = "com.em.batterywidget.action.BATTERY_OKAY";
    public static final String ACTION_WIDGET_UPDATE = "com.em.batterywidget.action.WIDGET_UPDATE";

    // NOVO: Ação para atualizar apenas o widget de GRÁFICO
    public static final String ACTION_WIDGET_UPDATE_GRAPH_ONLY = "com.em.batterywidget.action.WIDGET_UPDATE_GRAPH_ONLY";

    public static final String EXTRA_WIDGET_IDS = "com.em.batterywidget.extra.WIDGET_IDS";
    private static final int JOB_ID = 1000;

    /**
     * Método auxiliar para enfileirar o trabalho neste JobIntentService.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, UpdateService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();

        if (ACTION_BATTERY_CHANGED.equals(action)) {
            // Chamado quando o nível da bateria muda
            handleBatteryChanged(intent);

        } else if (ACTION_WIDGET_UPDATE.equals(action)) {
            // Chamado por BatteryWidget.java para desenhar BATERIA + TEXTO
            handleWidgetUpdate(intent);

        } else if (ACTION_WIDGET_UPDATE_GRAPH_ONLY.equals(action)) {
            // NOVO: Chamado por BatteryGraphWidget.java para desenhar SOMENTE GRÁFICO
            handleGraphWidgetUpdate(intent);
        }
        // As ações LOW e OKAY continuam a ser tratadas pela lógica abaixo.
    }

    private BatteryInfo getBatteryInfoFromSystem() {
        Intent batteryStatusIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatusIntent == null) return null;
        return new BatteryInfo(batteryStatusIntent);
    }

    private void handleBatteryChanged(Intent intent) {

        BatteryInfo newBatteryInfo = new BatteryInfo(intent);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        final int level = newBatteryInfo.getLevel();
        final boolean isCharging = newBatteryInfo.isCharging();

        // 2. CRÍTICO: Inserção na base de dados (necessário para ambos os widgets)
        Database db = new Database(this);
        try {
            DatabaseEntry entry = new DatabaseEntry(
                    System.currentTimeMillis(),
                    newBatteryInfo.getLevel(),
                    newBatteryInfo.getStatus(),
                    newBatteryInfo.getPlugged(),
                    newBatteryInfo.getVoltage(),
                    newBatteryInfo.getHealth()
            );
            db.insert(entry);
            Log.d(TAG, "Dados de bateria inseridos no DB.");
        } finally {
            db.close();
        }

        // 3. Desenhar e atualizar TODOS os widgets

        // a) Widget Principal (Bateria e Texto)
        String extraInfo = getBatteryExtraInfo(newBatteryInfo);
        RemoteViews batteryViews = createRemoteViews(level, isCharging, extraInfo);
        ComponentName batteryComponentName = new ComponentName(this, BatteryWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(batteryComponentName, batteryViews);

        // b) Widget de Gráfico (Somente Gráfico)
        RemoteViews graphViews = createGraphRemoteViews();
        ComponentName graphComponentName = new ComponentName(this, BatteryGraphWidget.class);
        appWidgetManager.updateAppWidget(graphComponentName, graphViews);

        // 4. Salvamento no SharedPreferences
        newBatteryInfo.saveToSharedPreferences(sharedPreferences);
    }

    /**
     * Atualiza o widget principal (Bateria e Texto).
     */
    private void handleWidgetUpdate(Intent intent) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        final int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
        if (widgetIds == null || widgetIds.length == 0) return;

        int level;
        boolean isCharging;
        String extraInfo;
        BatteryInfo batteryInfo;

        BatteryInfo systemBatteryInfo = getBatteryInfoFromSystem();

        if (systemBatteryInfo != null) {
            batteryInfo = systemBatteryInfo;
            level = batteryInfo.getLevel();
            isCharging = batteryInfo.isCharging();
            batteryInfo.saveToSharedPreferences(sharedPreferences);
        } else {
            batteryInfo = new BatteryInfo(sharedPreferences);
            level = batteryInfo.getLevel();
            isCharging = batteryInfo.isCharging();

            if (level < 1) {
                level = 50;
                isCharging = false;
                Log.w(TAG, "handleWidgetUpdate: Falha total na leitura de dados. Usando fallback seguro de 50%.");
            } else {
                Log.w(TAG, "handleWidgetUpdate: Falha na leitura do sistema. Usando dados do SP (Nível: " + level + "%).");
            }
        }

        extraInfo = getBatteryExtraInfo(batteryInfo);

        // Chamada para desenhar APENAS a bateria e o texto
        RemoteViews remoteViews = createRemoteViews(level, isCharging, extraInfo);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(intent.getIntArrayExtra(EXTRA_WIDGET_IDS), remoteViews);
    }

    /**
     * NOVO: Atualiza o widget secundário (Somente Gráfico).
     */
    private void handleGraphWidgetUpdate(Intent intent) {

        final int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
        if (widgetIds == null || widgetIds.length == 0) return;

        // Chamada para desenhar APENAS o gráfico
        RemoteViews remoteViews = createGraphRemoteViews();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(widgetIds, remoteViews);
    }

    // --- LÓGICA DE TEXTO EXTRA ---

    private String mapHealthCodeToString(int healthCode) {
        switch (healthCode) {
            case 1: return "Desconhecida";
            case 2: return "Boa";
            case 3: return "Superaquecida";
            case 4: return "Defeito";
            case 5: return "Sobrevoltada";
            case 6: return "Falha";
            case 7: return "Fria";
            default: return "Desconhecida";
        }
    }

    /**
     * Formata a string de informação extra a ser exibida no rodapé.
     */
    private String getBatteryExtraInfo(BatteryInfo info) {
        if (info == null) return "Informação Extra Indisponível";

        String health = mapHealthCodeToString(info.getHealth());
        String tech = info.getTechnology();

        return "Saúde: " + health + " | Tecnologia: " + tech;
    }


    // --- MÉTODOS DE AJUDA PARA DESENHO ---

    private int getBatteryFillResource(int level) {
        if (level < 5) return R.drawable.lic_10;
        if (level >= 95) return R.drawable.lic_100;
        if (level >= 85) return R.drawable.lic_90;
        if (level >= 75) return R.drawable.lic_80;
        if (level >= 65) return R.drawable.lic_70;
        if (level >= 55) return R.drawable.lic_60;
        if (level >= 45) return R.drawable.lic_50;
        if (level >= 35) return R.drawable.lic_40;
        if (level >= 25) return R.drawable.lic_30;
        if (level >= 15) return R.drawable.lic_20;
        if (level >= 5) return R.drawable.lic_10;
        return R.drawable.lic_10;
    }

    private Bitmap createDiagnosticBitmap(final int level, final boolean isCharging, String error) {
        final int TARGET_WIDTH = 120;
        final int TARGET_HEIGHT = 180;
        Bitmap diagnosticBitmap = Bitmap.createBitmap(TARGET_WIDTH, TARGET_HEIGHT, Bitmap.Config.ARGB_8888);
        diagnosticBitmap.eraseColor(isCharging ? Color.GREEN : Color.RED);
        Canvas canvas = new Canvas(diagnosticBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(30f);
        String errorText = error + " " + level + "%";
        Rect bounds = new Rect();
        paint.getTextBounds(errorText, 0, errorText.length(), bounds);
        int x = (TARGET_WIDTH - bounds.width()) / 2;
        int y = (TARGET_HEIGHT + bounds.height()) / 2;
        canvas.drawText(errorText, x, y, paint);
        return diagnosticBitmap;
    }

    /**
     * Gera um Bitmap para o gráfico de histórico de bateria (Para o Widget de Gráfico).
     */
    public Bitmap createGraphBitmap(Context context) {
        final int GRAPH_WIDTH = 300;
        final int GRAPH_HEIGHT = 180;

        final float PADDING_LEFT = 50f;
        final float PADDING_TOP = 25f;
        final float PADDING_BOTTOM = 20f;

        final float DRAW_WIDTH = GRAPH_WIDTH - PADDING_LEFT - 10f;
        final float DRAW_HEIGHT = GRAPH_HEIGHT - PADDING_TOP - PADDING_BOTTOM;

        Bitmap graphBitmap = Bitmap.createBitmap(GRAPH_WIDTH, GRAPH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(graphBitmap);
        canvas.drawColor(Color.TRANSPARENT);

        // --- 1. LEITURA DE DADOS ---
        Database db = new Database(context);
        Cursor cursor = null;
        ArrayList<Integer> levels = new ArrayList<>();
        ArrayList<Integer> voltages = new ArrayList<>();
        final int MIN_VOLTAGE = 3400;
        final int MAX_VOLTAGE = 4300;
        final int VOLTAGE_RANGE = MAX_VOLTAGE - MIN_VOLTAGE;

        try {
            cursor = db.getEntries();

            if (cursor != null && cursor.moveToFirst()) {
                final int LEVEL_COLUMN_INDEX = Database.LEVEL;
                final int VOLTAGE_COLUMN_INDEX = Database.VOLTAGE;

                do {
                    levels.add(cursor.getInt(LEVEL_COLUMN_INDEX));
                    if (cursor.getColumnCount() > VOLTAGE_COLUMN_INDEX) {
                        voltages.add(cursor.getInt(VOLTAGE_COLUMN_INDEX));
                    } else {
                        voltages.add(MIN_VOLTAGE);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler entradas do DB para o gráfico: " + e.getMessage());
            return graphBitmap;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        int count = levels.size();
        if (count < 1) return graphBitmap;

        // --- 2. CONFIGURAÇÃO DE ESTILOS e DESENHO DA GRADE/RÓTULOS ---
        Paint gridPaint = new Paint(); gridPaint.setColor(Color.parseColor("#44FFFFFF")); gridPaint.setStrokeWidth(1f);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        Paint levelLinePaint = new Paint(); levelLinePaint.setColor(Color.parseColor("#42A5F5")); levelLinePaint.setStrokeWidth(3f); levelLinePaint.setStyle(Paint.Style.STROKE); levelLinePaint.setAntiAlias(true); levelLinePaint.setShadowLayer(3f, 0, 0, Color.BLACK);
        Paint voltageLinePaint = new Paint(); voltageLinePaint.setColor(Color.parseColor("#FFA726")); voltageLinePaint.setStrokeWidth(3f); voltageLinePaint.setStyle(Paint.Style.STROKE); voltageLinePaint.setAntiAlias(true); voltageLinePaint.setShadowLayer(3f, 0, 0, Color.BLACK);
        Paint levelAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG); levelAreaPaint.setStyle(Paint.Style.FILL);

        // Desenho da Grade e Rótulos do Eixo Y
        float[] gridLevels = {1.0f, 0.50f, 0.0f};
        for (int i = 0; i < gridLevels.length; i++) {
            float y = PADDING_TOP + DRAW_HEIGHT * (1f - gridLevels[i]);
            if (i < gridLevels.length - 1) {
                // Linhas horizontais (grid)
                canvas.drawLine(PADDING_LEFT, y, GRAPH_WIDTH, y, gridPaint);
            }
            canvas.drawText((int)(gridLevels[i]*100) + "%", PADDING_LEFT - 5f, y + textPaint.getTextSize() / 3, textPaint);
        }

        // Linha vertical do Eixo Y
        canvas.drawLine(PADDING_LEFT, PADDING_TOP, PADDING_LEFT, GRAPH_HEIGHT - PADDING_BOTTOM, gridPaint);

        // --- 3. CÁLCULO DOS PATHS e DESENHO FINAL (Ajuste de Padding) ---
        float xStep = (count > 1) ? DRAW_WIDTH / (count - 1) : DRAW_WIDTH;

        Path levelLinePath = new Path();
        Path levelAreaPath = new Path();
        Path voltageLinePath = new Path();

        float startY = PADDING_TOP + DRAW_HEIGHT * (1f - (levels.get(0) / 100f));

        levelAreaPath.moveTo(PADDING_LEFT, GRAPH_HEIGHT - PADDING_BOTTOM);
        levelAreaPath.lineTo(PADDING_LEFT, startY);

        for (int i = 0; i < count; i++) {
            float x = PADDING_LEFT + (i * xStep);

            float levelRatio = levels.get(i) / 100f;
            float levelY = PADDING_TOP + DRAW_HEIGHT * (1f - levelRatio);

            if (i == 0) levelLinePath.moveTo(x, levelY); else levelLinePath.lineTo(x, levelY);
            levelAreaPath.lineTo(x, levelY);

            float normalizedVoltage = Math.max(0, voltages.get(i) - MIN_VOLTAGE);
            float voltageRatio = Math.min(1.0f, normalizedVoltage / VOLTAGE_RANGE);
            float voltageY = PADDING_TOP + DRAW_HEIGHT * (1f - voltageRatio);

            if (i == 0) voltageLinePath.moveTo(x, voltageY); else voltageLinePath.lineTo(x, voltageY);
        }

        if (count > 0) {
            levelAreaPath.lineTo(PADDING_LEFT + DRAW_WIDTH, GRAPH_HEIGHT - PADDING_BOTTOM);
            levelAreaPath.lineTo(PADDING_LEFT, GRAPH_HEIGHT - PADDING_BOTTOM);
        }

        LinearGradient levelGradient = new LinearGradient(0, PADDING_TOP, 0, GRAPH_HEIGHT - PADDING_BOTTOM, Color.parseColor("#4442A5F5"), Color.TRANSPARENT, Shader.TileMode.CLAMP);
        levelAreaPaint.setShader(levelGradient);
        canvas.drawPath(levelAreaPath, levelAreaPaint);
        canvas.drawPath(levelLinePath, levelLinePaint);
        canvas.drawPath(voltageLinePath, voltageLinePaint);

        // --- 4. LEGENDA ---
        Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG); legendPaint.setColor(Color.WHITE); legendPaint.setTextSize(14f);

        final float LEGEND_START_X = PADDING_LEFT + 5f;
        final float LEGEND_Y1 = PADDING_TOP / 2 - 5f;

        // Nível (%)
        canvas.drawRect(LEGEND_START_X, LEGEND_Y1, LEGEND_START_X + 10f, LEGEND_Y1 + 10f, levelLinePaint);
        canvas.drawText("Nível (%)", LEGEND_START_X + 15f, LEGEND_Y1 + 9f, legendPaint);

        // Tensão
        final float VOLTAGE_LABEL_X = LEGEND_START_X + legendPaint.measureText("Nível (%)") + 35f;

        canvas.drawRect(VOLTAGE_LABEL_X, LEGEND_Y1, VOLTAGE_LABEL_X + 10f, LEGEND_Y1 + 10f, voltageLinePaint);
        canvas.drawText("Tensão", VOLTAGE_LABEL_X + 15f, LEGEND_Y1 + 9f, legendPaint);

        return graphBitmap;
    }


    /**
     * Bitmap da Bateria (Para o Widget Principal).
     */
    private Bitmap createBatteryBitmap(Context context, final int level, final boolean isCharging) {
        // Aumentando o tamanho para ocupar o widget principal de forma mais proeminente
        final int TARGET_WIDTH = 200;
        final int TARGET_HEIGHT = 300;

        Resources resources = context.getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap finalBitmap = Bitmap.createBitmap(TARGET_WIDTH, TARGET_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBitmap);

        Bitmap fillBitmap = null;
        Bitmap shellBitmap = null;

        try {
            // CAMADA 1: O PREENCHIMENTO DE NÍVEL (lic_XX.png)
            int fillResId = getBatteryFillResource(level);

            if (fillResId != 0) {
                fillBitmap = BitmapFactory.decodeResource(resources, fillResId, options);
                if (fillBitmap != null) {
                    Bitmap scaledFill = Bitmap.createScaledBitmap(fillBitmap, TARGET_WIDTH, TARGET_HEIGHT, true);
                    if (scaledFill != null) {
                        canvas.drawBitmap(scaledFill, 0, 0, null);
                        scaledFill.recycle();
                    }
                }
            }

            // --- CAMADA 2: DESENHO DO ÍCONE DE CARGA (Raio) ---
            if (isCharging) {
                Bitmap chargeBitmap = null;
                Bitmap scaledCharge = null;
                try {
                    chargeBitmap = BitmapFactory.decodeResource(resources, R.drawable.charge, options);
                    if (chargeBitmap != null) {
                        // Faz o raio ocupar uma área proporcionalmente menor para não sobrepor
                        final int CHARGE_WIDTH = (int)(TARGET_WIDTH * 0.7);
                        final int CHARGE_HEIGHT = (int)(TARGET_HEIGHT * 0.4);

                        scaledCharge = Bitmap.createScaledBitmap(chargeBitmap, CHARGE_WIDTH, CHARGE_HEIGHT, true);

                        int chargeX = (TARGET_WIDTH - CHARGE_WIDTH) / 2;
                        int chargeY = (TARGET_HEIGHT - CHARGE_HEIGHT) / 2;

                        canvas.drawBitmap(scaledCharge, chargeX, chargeY, null);
                        if (scaledCharge != null) scaledCharge.recycle();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao desenhar ícone de carga no bitmap: " + e.getMessage());
                } finally {
                    if (chargeBitmap != null && !chargeBitmap.isRecycled()) chargeBitmap.recycle();
                }
            }


            // CAMADA 3: O CONTORNO (battery.png)
            shellBitmap = BitmapFactory.decodeResource(resources, R.drawable.battery, options);
            if (shellBitmap == null) {
                return createDiagnosticBitmap(level, isCharging, "FAIL SHELL");
            }
            Bitmap scaledShell = Bitmap.createScaledBitmap(shellBitmap, TARGET_WIDTH, TARGET_HEIGHT, true);
            if (scaledShell != null) {
                canvas.drawBitmap(scaledShell, 0, 0, null);
                scaledShell.recycle();
            } else {
                return createDiagnosticBitmap(level, isCharging, "FAIL SCALE");
            }

            // --- CAMADA 4: O TEXTO (Porcentagem) ---
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setShadowLayer(6f, 3f, 3f, Color.BLACK); // Sombra aumentada para clareza

            float textSize = TARGET_HEIGHT * 0.25f; // Tamanho do texto maior
            paint.setTextSize(textSize);

            String text = String.valueOf(level) + "%";
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);

            int x = (finalBitmap.getWidth() - bounds.width()) / 2;
            int y = (finalBitmap.getHeight() + bounds.height()) / 2 + (int)(finalBitmap.getHeight() * 0.05f);

            canvas.drawText(text, x, y, paint);

        } catch (Exception e) {
            Log.e(TAG, "Erro geral ao criar o Bitmap da bateria: " + e.getMessage());
            return createDiagnosticBitmap(level, isCharging, "FAIL EXCEPTION");
        } finally {
            if (fillBitmap != null && !fillBitmap.isRecycled()) fillBitmap.recycle();
            if (shellBitmap != null && !shellBitmap.isRecycled()) shellBitmap.recycle();
        }

        return finalBitmap;
    }


    /**
     * Cria as RemoteViews para o widget principal (Bateria + Texto de Status).
     */
    private RemoteViews createRemoteViews(final int level, final boolean isCharging, String extraInfo) {

        // Layout do widget principal, agora sem gráfico
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_view);

        // 1. CRIA o Bitmap da Bateria
        Bitmap batteryBitmap = createBatteryBitmap(this, level, isCharging);

        // 2. DEFINE o Bitmap da Bateria na View remota
        remoteViews.setImageViewBitmap(R.id.battery_view, batteryBitmap);

        // 3. Define o texto de informação extra no rodapé
        remoteViews.setTextViewText(R.id.info_text_bottom, extraInfo);

        // 4. Garante visibilidade e Intents
        remoteViews.setViewVisibility(R.id.battery_view, View.VISIBLE);

        // CORREÇÃO FINAL: REMOVIDA a referência quebrada a R.id.graph_view

        // 5. Intent para abrir a activity
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
     * Cria as RemoteViews para o widget de gráfico.
     */
    private RemoteViews createGraphRemoteViews() {
        // Layout do novo widget de gráfico
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_graph_view);

        // 1. CRIA o Bitmap do Gráfico
        Bitmap graphBitmap = createGraphBitmap(this);

        // 2. DEFINE o Bitmap do Gráfico na View remota
        remoteViews.setImageViewBitmap(R.id.graph_only_view, graphBitmap);

        // 3. Garante visibilidade
        remoteViews.setViewVisibility(R.id.graph_only_view, View.VISIBLE);

        // 4. Intent para abrir a activity (opcional, mas bom ter)
        Intent activityIntent = new Intent(this, WidgetActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, flags);
        remoteViews.setOnClickPendingIntent(R.id.graph_widget_root_view, pendingIntent);

        return remoteViews;
    }
}