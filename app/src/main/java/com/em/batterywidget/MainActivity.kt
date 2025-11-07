package com.em.batterywidget

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.em.batterywidget.ui.BatteryLogAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.widget.Toast

/**
 * A atividade principal da aplicação que exibe o histórico de logs da bateria.
 *
 * Usa Koin para injetar o BatteryViewModel e LiveData para observar as alterações no banco de dados.
 */
class MainActivity : AppCompatActivity() {

    // Injeção do ViewModel usando Koin
    private val batteryViewModel: BatteryViewModel by viewModel()

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var fabClearLogs: FloatingActionButton
    private val logAdapter = BatteryLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Certifique-se de que o layout XML foi criado em 'res/layout/activity_main.xml'
        setContentView(R.layout.activity_main)

        // Inicializar Views
        recyclerView = findViewById(R.id.recycler_view_logs)
        emptyStateTextView = findViewById(R.id.tv_empty_state)
        fabClearLogs = findViewById(R.id.fab_clear_logs)

        // Configurar RecyclerView
        recyclerView.adapter = logAdapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        // Observar dados do ViewModel
        observeBatteryLogs()

        // Configurar o botão para limpar logs
        fabClearLogs.setOnClickListener {
            batteryViewModel.clearAllLogs()
            Toast.makeText(this, "Logs de bateria limpos.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configura a observação do LiveData de logs de bateria.
     */
    private fun observeBatteryLogs() {
        batteryViewModel.allLogs.observe(this) { logs ->
            // Atualiza o Adapter com a nova lista de logs
            logAdapter.submitList(logs)

            // Alterna o estado vazio
            if (logs.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyStateTextView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyStateTextView.visibility = View.GONE
            }
        }
    }
}