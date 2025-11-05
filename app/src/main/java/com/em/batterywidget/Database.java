package com.em.batterywidget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Gerenciador da base de dados SQLite para o histórico de bateria.
 */
public class Database extends SQLiteOpenHelper {

    private static final String TAG = "BatteryWidgetDatabase";
    private static final String DATABASE_NAME = "BatteryHistoryDB";
    private static final int DATABASE_VERSION = 1;

    // Tabela e colunas
    private static final String TABLE_NAME = "battery_log";
    private static final String KEY_TIME = "time_stamp";
    private static final String KEY_LEVEL = "battery_level";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PLUGGED = "plugged";
    private static final String KEY_VOLTAGE = "voltage";
    private static final String KEY_HEALTH = "health";

    // Índices de Colunas
    public static final int TIME = 0;
    public static final int LEVEL = 1;
    public static final int STATUS = 2;
    public static final int PLUGGED = 3;
    public static final int VOLTAGE = 4;
    public static final int HEALTH = 5;

    private SQLiteDatabase mDatabase;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_TIME + " INTEGER PRIMARY KEY,"
                + KEY_LEVEL + " INTEGER,"
                + KEY_STATUS + " INTEGER,"
                + KEY_PLUGGED + " INTEGER,"
                + KEY_VOLTAGE + " INTEGER,"
                + KEY_HEALTH + " INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "Tabela de registo de bateria criada (com 6 campos).");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Atualizando a base de dados. Todos os dados serão perdidos!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * NOVO: Abre a base de dados para escrita.
     * @return Esta instância de Database.
     */
    public Database openWrite() {
        mDatabase = getWritableDatabase();
        return this;
    }

    /**
     * NOVO: Abre a base de dados para leitura.
     * @return Esta instância de Database.
     */
    public Database openRead() {
        mDatabase = getReadableDatabase();
        return this;
    }

    /**
     * Fecha a base de dados.
     */
    @Override
    public void close() {
        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
            mDatabase = null;
            Log.d(TAG, "Base de dados fechada.");
        }
        super.close();
    }

    /**
     * Adiciona uma nova entrada de histórico da bateria à base de dados.
     * @param entry O objeto DatabaseEntry a ser inserido.
     */
    public void insert(DatabaseEntry entry) {
        SQLiteDatabase db = (mDatabase != null && mDatabase.isOpen() && mDatabase.isReadOnly() == false) ? mDatabase : getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TIME, entry.getTime());
        values.put(KEY_LEVEL, entry.getLevel());
        values.put(KEY_STATUS, entry.getStatus());
        values.put(KEY_PLUGGED, entry.getPlugged());
        values.put(KEY_VOLTAGE, entry.getVoltage());
        values.put(KEY_HEALTH, entry.getHealth());

        long newRowId = db.insert(TABLE_NAME, null, values);
        if (newRowId == -1) {
            Log.w(TAG, "Falha ao inserir ou conflito de chave primária para nível: " + entry.getLevel());
        } else {
            Log.d(TAG, "Nova entrada de bateria inserida com ID: " + newRowId);
        }

        if (db != mDatabase) {
            db.close();
        }
    }

    /**
     * Obtém todas as entradas de histórico da bateria.
     */
    public Cursor getEntries() {
        if (mDatabase == null) {
            try {
                mDatabase = getReadableDatabase();
            } catch (Exception e) {
                Log.e(TAG, "getEntries falhou: Base de dados não pôde ser aberta para leitura.", e);
                return null;
            }
        }

        return mDatabase.query(TABLE_NAME,
                new String[] {KEY_TIME, KEY_LEVEL, KEY_STATUS, KEY_PLUGGED, KEY_VOLTAGE, KEY_HEALTH},
                null,
                null,
                null,
                null,
                KEY_TIME + " ASC");
    }

    // --- MÉTODOS DE TESTE E DEPURACAO ---

    /**
     * Verifica se a base de dados está vazia e insere dados falsos para fins de teste.
     * @param context O contexto da aplicação.
     */
    public static void checkAndInsertMockData(Context context) {
        Database db = new Database(context);
        Cursor cursor = null;
        try {
            cursor = db.getEntries();

            if (cursor == null || cursor.getCount() == 0) {
                Log.d(TAG, "Base de dados vazia. Inserindo dados de teste...");

                long now = System.currentTimeMillis();
                long hour = 60 * 60 * 1000;

                db.insert(new DatabaseEntry(now - 4 * hour, 65, 3, 0, 3800, 2));
                db.insert(new DatabaseEntry(now - 3 * hour, 70, 2, 2, 4050, 2));
                db.insert(new DatabaseEntry(now - 2 * hour, 75, 2, 2, 4200, 2));
                db.insert(new DatabaseEntry(now - 1 * hour, 80, 3, 0, 4100, 2));
                db.insert(new DatabaseEntry(now, 85, 3, 0, 4150, 2));

                Log.d(TAG, "5 entradas de dados de teste inseridas com sucesso.");
            } else {
                Log.d(TAG, "Base de dados já contém " + cursor.getCount() + " entradas.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao tentar inserir dados de teste.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }
}