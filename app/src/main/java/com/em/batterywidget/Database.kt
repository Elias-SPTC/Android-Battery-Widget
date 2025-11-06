package com.em.batterywidget

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Gerenciador de Banco de Dados SQLite para armazenar o histórico de leituras da bateria.
 * Ele usa DatabaseEntry.kt para tipar as entradas.
 */
class Database(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val TAG = "Database"

    // --- Definições de Tabela e Colunas ---

    companion object {
        // Detalhes do Banco de Dados
        const val DATABASE_VERSION = 2 // Versão aumentada para adicionar colunas de saúde/tensão
        const val DATABASE_NAME = "BatteryHistory.db"

        // Nome da Tabela
        private const val TABLE_NAME = "battery_data"

        // Nomes das Colunas (Refletem DatabaseEntry.kt)
        const val COLUMN_NAME_ID = "_id" // Coluna primária
        const val COLUMN_NAME_TIMESTAMP = "timestamp" // Usado como KEY_TIME
        const val COLUMN_NAME_LEVEL = "level"
        const val COLUMN_NAME_STATUS = "status"
        const val COLUMN_NAME_PLUGGED = "plugged"
        const val COLUMN_NAME_VOLTAGE = "voltage"
        const val COLUMN_NAME_HEALTH = "health"

        // Colunas para acesso via Cursor
        const val ID = 0
        const val TIMESTAMP = 1
        const val LEVEL = 2
        const val STATUS = 3
        const val PLUGGED = 4
        const val VOLTAGE = 5
        const val HEALTH = 6
    }

    // --- Comandos SQL ---

    // SQL para criar a tabela
    private val SQL_CREATE_ENTRIES =
        "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_NAME_ID INTEGER PRIMARY KEY," +
                "$COLUMN_NAME_TIMESTAMP INTEGER," +
                "$COLUMN_NAME_LEVEL INTEGER," +
                "$COLUMN_NAME_STATUS INTEGER," +
                "$COLUMN_NAME_PLUGGED INTEGER," +
                "$COLUMN_NAME_VOLTAGE INTEGER," +
                "$COLUMN_NAME_HEALTH INTEGER)"

    // SQL para apagar a tabela
    private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"

    // --- Sobrescritas de SQLiteOpenHelper ---

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
        Log.d(TAG, "Tabela $TABLE_NAME criada com sucesso.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Esta estratégia de atualização é destrutiva (apaga e recria).
        // Para uma aplicação real, você usaria comandos ALTER TABLE aqui.
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
        Log.d(TAG, "Database atualizado de $oldVersion para $newVersion.")
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    // --- Operações CRUD ---

    /**
     * Insere uma nova entrada de dados da bateria no banco de dados.
     */
    fun insertEntry(entry: DatabaseEntry): Long {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_NAME_TIMESTAMP, entry.timestamp)
            put(COLUMN_NAME_LEVEL, entry.level)
            put(COLUMN_NAME_STATUS, entry.status)
            put(COLUMN_NAME_PLUGGED, entry.plugged)
            put(COLUMN_NAME_VOLTAGE, entry.voltage)
            put(COLUMN_NAME_HEALTH, entry.health)
        }

        // Insere a nova linha, retornando a chave primária
        val newRowId = db.insert(TABLE_NAME, null, values)
        Log.d(TAG, "Nova entrada inserida com ID: $newRowId - Nível: ${entry.level}%")
        db.close()
        return newRowId
    }

    /**
     * Retorna todas as entradas do banco de dados, ordenadas por timestamp.
     * Necessário para a criação do gráfico.
     */
    fun getEntries(): Cursor? {
        val db = this.readableDatabase
        val projection = arrayOf(
            COLUMN_NAME_ID,
            COLUMN_NAME_TIMESTAMP,
            COLUMN_NAME_LEVEL,
            COLUMN_NAME_STATUS,
            COLUMN_NAME_PLUGGED,
            COLUMN_NAME_VOLTAGE,
            COLUMN_NAME_HEALTH
        )

        // Limita a 1000 entradas mais recentes para otimização de gráfico
        val sortOrder = "$COLUMN_NAME_TIMESTAMP DESC LIMIT 1000"

        val cursor = db.query(
            TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            sortOrder
        )
        // Não fechamos o DB aqui; o chamador deve fechar o Cursor.
        return cursor
    }

    /**
     * Verifica se existe pelo menos uma entrada no DB. Se não, insere dados de mock.
     * Usado pela WidgetActivity.
     */
    fun checkAndInsertMockData() {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()

        if (count == 0) {
            Log.i(TAG, "DB vazio. Inserindo dados de mock para demonstração do gráfico.")
            val now = System.currentTimeMillis()
            val hourMs = 60 * 60 * 1000L

            // Insere 10 entradas simulando a descarga e recarga
            insertEntry(DatabaseEntry(now - 9 * hourMs, 85, 1, 0, 4000, 2)) // Descarregando
            insertEntry(DatabaseEntry(now - 7 * hourMs, 70, 1, 0, 3950, 2))
            insertEntry(DatabaseEntry(now - 5 * hourMs, 60, 2, 1, 4100, 2)) // Conectado e Carregando
            insertEntry(DatabaseEntry(now - 3 * hourMs, 80, 2, 1, 4200, 2))
            insertEntry(DatabaseEntry(now - 1 * hourMs, 90, 5, 2, 4300, 2)) // Quase Cheio, Fonte AC
        }
    }

    /**
     * Limpa toda a tabela de dados.
     */
    fun clearHistory() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        Log.d(TAG, "Tabela $TABLE_NAME limpa.")
        db.close()
    }
}