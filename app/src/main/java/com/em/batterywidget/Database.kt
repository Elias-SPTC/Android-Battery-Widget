package com.em.batterywidget

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Classe para gerenciar a persistência de dados de histórico de bateria usando SQLite.
 * Implementa o padrão Singleton.
 *
 * NOTA DE CORREÇÃO: Os campos de temperatura e voltagem na HistoryData foram mantidos
 * como Int, e a conversão de/para Float no BatteryInfo é tratada nos métodos de E/S.
 * A classe BatteryInfo é esperada em outros arquivos.
 */
class Database private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val TAG = "BatteryDatabase"

    // --- Definições de Constantes ---
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "BatteryHistory.db"

        // Nome da tabela principal
        private const val TABLE_HISTORY = "history"

        // Nomes das colunas da tabela
        private const val COLUMN_ID = "_id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_LEVEL = "level"
        private const val COLUMN_TEMPERATURE = "temperature" // Salvo como Int (ex: 250 para 25.0)
        private const val COLUMN_VOLTAGE = "voltage"
        private const val COLUMN_PLUGGED = "plugged"
        private const val COLUMN_STATUS = "status"

        // Constantes exigidas por BatteryRenderer.kt
        const val TIMESTAMP = COLUMN_TIMESTAMP
        const val LEVEL = COLUMN_LEVEL
        const val VOLTAGE = COLUMN_VOLTAGE

        @Volatile
        private var INSTANCE: Database? = null

        /**
         * Retorna a instância singleton do banco de dados.
         * Garante que apenas uma instância seja criada (Thread-safe).
         */
        fun getInstance(context: Context): Database {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Database(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Data class para representar um registro de histórico recuperado do banco de dados.
     */
    data class HistoryData(
        val id: Long,
        val timestamp: Long,
        val level: Int,
        // Mantido como Int, presumindo que o valor foi multiplicado por 10 e salvo como Int.
        val temperature: Int,
        val voltage: Int,
        val plugged: Int,
        val status: Int
    )

    /**
     * Data class MOCK para compilação.
     * Presume-se que o arquivo BatteryInfo.kt existe e é parecido com isto.
     */
    data class BatteryInfo(
        val timestamp: Long,
        val level: Int,
        val status: Int,
        val plugged: Int,
        val voltage: Int,
        val health: Int,
        val technology: String,
        val temperature: Float
    )


    // --- Criação do Banco de Dados ---

    override fun onCreate(db: SQLiteDatabase) {
        // SQL para criar a tabela de histórico
        val CREATE_HISTORY_TABLE = "CREATE TABLE $TABLE_HISTORY (" +
                "$COLUMN_ID INTEGER PRIMARY KEY," +
                "$COLUMN_TIMESTAMP INTEGER NOT NULL," +
                "$COLUMN_LEVEL INTEGER NOT NULL," +
                "$COLUMN_TEMPERATURE INTEGER NOT NULL," + // Corrigido para ser INTEGER
                "$COLUMN_VOLTAGE INTEGER NOT NULL," +
                "$COLUMN_PLUGGED INTEGER NOT NULL," +
                "$COLUMN_STATUS INTEGER NOT NULL" +
                ")"
        db.execSQL(CREATE_HISTORY_TABLE)
        Log.i(TAG, "Tabela de histórico criada com sucesso.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
        Log.w(TAG, "Banco de dados atualizado de $oldVersion para $newVersion. Tabela descartada.")
    }

    // --- Métodos de Operação para BatteryMonitor ---

    /**
     * Insere um novo registro de dados de bateria no histórico.
     * Alias para insertHistory.
     *
     * @param info O objeto BatteryInfo contendo os dados a serem salvos.
     * @return O ID da nova linha inserida, ou -1 se ocorrer um erro.
     */
    fun insertEntry(info: BatteryInfo): Long {
        return insertHistory(info)
    }

    /**
     * Obtém a última entrada registrada no banco de dados.
     * Usado por BatteryMonitor para aplicar a lógica de limitação de salvamento.
     */
    fun getLastEntry(): BatteryInfo? {
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            val query = "SELECT * FROM $TABLE_HISTORY ORDER BY $COLUMN_TIMESTAMP DESC LIMIT 1"
            cursor = db.rawQuery(query, null)

            if (cursor.moveToFirst()) {
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val level = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEVEL))
                val voltage = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VOLTAGE))
                // A temperatura é lida como Int (valor bruto, ex: 250 para 25.0°C)
                val temperatureRaw = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEMPERATURE))
                val plugged = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLUGGED))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATUS))

                // Converte a temperatura de volta para Float em Celsius (dividindo por 10)
                val temperatureInCelsius = temperatureRaw / 10.0f

                return BatteryInfo(
                    timestamp = timestamp,
                    level = level,
                    status = status,
                    plugged = plugged,
                    voltage = voltage,
                    // O campo health e technology não são armazenados na tabela history, usamos stubs.
                    health = -1,
                    technology = "N/A",
                    temperature = temperatureInCelsius // Usa o Float convertido
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter última entrada: " + e.message)
        } finally {
            cursor?.close()
        }
        return null
    }

    // --- Métodos de CRUD/Operação do original ---

    /**
     * Insere um novo registro de dados de bateria no histórico.
     */
    fun insertHistory(info: BatteryInfo): Long {
        val db = this.writableDatabase

        // Converte a temperatura Float para Int (multiplica por 10 para precisão de 1 casa decimal)
        // Por exemplo: 25.5°C -> 255. O SQLite armazena isso como INTEGER.
        val temperatureRawInt = (info.temperature * 10).toInt()

        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, info.timestamp) // Usa o timestamp do Intent
            put(COLUMN_LEVEL, info.level)
            put(COLUMN_TEMPERATURE, temperatureRawInt) // Salva valor bruto como INTEGER
            put(COLUMN_VOLTAGE, info.voltage)
            put(COLUMN_PLUGGED, info.plugged)
            put(COLUMN_STATUS, info.status)
        }

        val newRowId = db.insert(TABLE_HISTORY, null, values)
        if (newRowId != -1L) {
            Log.d(TAG, "Registro de histórico inserido com ID: $newRowId, Nível: ${info.level}%")
        } else {
            Log.e(TAG, "Falha ao inserir registro de histórico.")
        }
        return newRowId
    }

    /**
     * Recupera o histórico de dados da bateria (para Activity).
     */
    fun getHistory(sinceTimestamp: Long): List<HistoryData> {
        val historyList = mutableListOf<HistoryData>()
        val db = this.readableDatabase

        val projection = arrayOf(
            COLUMN_ID, COLUMN_TIMESTAMP, COLUMN_LEVEL, COLUMN_TEMPERATURE,
            COLUMN_VOLTAGE, COLUMN_PLUGGED, COLUMN_STATUS
        )

        val selection = "$COLUMN_TIMESTAMP > ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())
        val sortOrder = "$COLUMN_TIMESTAMP ASC"

        val cursor = db.query(TABLE_HISTORY, projection, selection, selectionArgs, null, null, sortOrder)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val level = it.getInt(it.getColumnIndexOrThrow(COLUMN_LEVEL))
                // temperature e voltage lidos como Int (valor bruto)
                val temperature = it.getInt(it.getColumnIndexOrThrow(COLUMN_TEMPERATURE))
                val voltage = it.getInt(it.getColumnIndexOrThrow(COLUMN_VOLTAGE))
                val plugged = it.getInt(it.getColumnIndexOrThrow(COLUMN_PLUGGED))
                val status = it.getInt(it.getColumnIndexOrThrow(COLUMN_STATUS))

                historyList.add(
                    HistoryData(id, timestamp, level, temperature, voltage, plugged, status)
                )
            }
        }
        Log.d(TAG, "Histórico recuperado: ${historyList.size} registros desde $sinceTimestamp.")
        return historyList
    }

    /**
     * Método auxiliar para BatteryRenderer.kt. Retorna um Cursor com os últimos 100 registros.
     * Os dados são retornados do mais novo para o mais antigo, como BatteryRenderer.kt espera.
     */
    fun getEntries(): Cursor? {
        val db = this.readableDatabase
        // Seleciona as colunas esperadas pelo BatteryRenderer: TIMESTAMP, LEVEL, VOLTAGE
        val columns = arrayOf(COLUMN_ID, COLUMN_TIMESTAMP, COLUMN_LEVEL, COLUMN_VOLTAGE)
        val limit = "100"
        return db.query(
            TABLE_HISTORY,
            columns,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC", // Mais recente primeiro
            limit
        )
    }

    /**
     * Remove registros de histórico mais antigos do que um período específico.
     */
    fun cleanupOldData(daysToKeep: Int = 30): Int {
        val db = this.writableDatabase
        val cutoffTimestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        val whereClause = "$COLUMN_TIMESTAMP < ?"
        val whereArgs = arrayOf(cutoffTimestamp.toString())
        val deletedRows = db.delete(TABLE_HISTORY, whereClause, whereArgs)
        Log.i(TAG, "Limpeza de dados: $deletedRows registros antigos deletados.")
        return deletedRows
    }
}