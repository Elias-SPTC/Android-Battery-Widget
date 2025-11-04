package com.em.batterywidget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Gerenciador da base de dados SQLite para o histórico de bateria.
 * Fornece métodos para criar a tabela, abrir/fechar a conexão, inserir e obter dados.
 */
public class Database extends SQLiteOpenHelper {

    private static final String TAG = "BatteryWidgetDatabase";
    private static final String DATABASE_NAME = "BatteryHistoryDB";
    private static final int DATABASE_VERSION = 1;

    // Tabela e colunas
    private static final String TABLE_NAME = "battery_log";
    private static final String KEY_TIME = "time_stamp";
    private static final String KEY_LEVEL = "battery_level";

    // Índices de Colunas (usados em WidgetActivity para ler o Cursor)
    // O Cursor retornado por getEntries() deve ter KEY_TIME na posição 0 e KEY_LEVEL na posição 1.
    public static final int TIME = 0;
    public static final int LEVEL = 1;

    private SQLiteDatabase mDatabase;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Cria a tabela de histórico da bateria
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_TIME + " INTEGER PRIMARY KEY," // Chave primária (timestamp)
                + KEY_LEVEL + " INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "Tabela de registo de bateria criada.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Atualizando a base de dados. Todos os dados serão perdidos!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Abre a base de dados para escrita.
     * @return Esta instância de Database.
     */
    public Database openWrite() {
        mDatabase = getWritableDatabase();
        return this;
    }

    /**
     * Abre a base de dados para leitura.
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
        SQLiteDatabase db = (mDatabase != null && mDatabase.isOpen()) ? mDatabase : getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TIME, entry.getTime());
        values.put(KEY_LEVEL, entry.getLevel());

        long newRowId = db.insert(TABLE_NAME, null, values);
        if (newRowId == -1) {
            Log.w(TAG, "Falha ao inserir ou conflito de chave primária para nível: " + entry.getLevel());
        } else {
            Log.d(TAG, "Nova entrada de bateria inserida com ID: " + newRowId);
        }

        // Se a base de dados foi aberta internamente, deve ser fechada.
        if (db != mDatabase) {
            db.close();
        }
    }

    /**
     * Obtém todas as entradas de histórico da bateria, ordenadas por tempo.
     * @return Cursor com as colunas KEY_TIME (índice 0) e KEY_LEVEL (índice 1).
     */
    public Cursor getEntries() {
        if (mDatabase == null) {
            Log.e(TAG, "getEntries falhou: Base de dados não aberta.");
            return null;
        }

        // Seleciona as colunas na ordem TIME (0), LEVEL (1)
        return mDatabase.query(TABLE_NAME,
                new String[] {KEY_TIME, KEY_LEVEL},
                null,
                null,
                null,
                null,
                KEY_TIME + " ASC"); // Ordena por tempo ascendente
    }

    // --- MÉTODOS DE TESTE E DEPURACAO ---

    /**
     * Verifica se a base de dados está vazia e insere dados falsos para fins de teste
     * se estiver. Isso garante que o gráfico não fique vazio na primeira execução.
     * @param context O contexto da aplicação.
     */
    public static void checkAndInsertMockData(Context context) {
        Database db = new Database(context);
        Cursor cursor = null;
        try {
            db.openRead(); // Abre a base de dados para ler
            cursor = db.getEntries();

            if (cursor == null || cursor.getCount() == 0) {
                Log.d(TAG, "Base de dados vazia. Inserindo dados de teste...");
                db.close(); // Fecha a leitura

                db.openWrite(); // Abre para escrita (irá reabrir)

                long now = System.currentTimeMillis();
                long hour = 60 * 60 * 1000;

                // Simula 5 pontos de dados nas últimas 5 horas
                db.insert(new DatabaseEntry(now - 4 * hour, 65));
                db.insert(new DatabaseEntry(now - 3 * hour, 70));
                db.insert(new DatabaseEntry(now - 2 * hour, 75));
                db.insert(new DatabaseEntry(now - 1 * hour, 80));
                db.insert(new DatabaseEntry(now, 85));

                Log.d(TAG, "5 entradas de dados de teste inseridas com sucesso.");
            } else {
                Log.d(TAG, "Base de dados já contém " + cursor.getCount() + " entradas. Não é necessário inserir dados de teste.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao tentar inserir dados de teste.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close(); // Garante o fechamento
        }
    }
}