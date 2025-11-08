package com.em.batterywidget

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    // CORRIGIDO: Injeta o ViewModel usando Koin
    private val batteryViewModel: BatteryViewModel by viewModel()
    private lateinit var logAdapter: BatteryLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CORRIGIDO: Usa o novo layout que acabamos de criar
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_logs)
        val emptyStateTextView: View = findViewById(R.id.tv_empty_state)
        val fabClearLogs: FloatingActionButton = findViewById(R.id.fab_clear_logs)

        logAdapter = BatteryLogAdapter()
        recyclerView.adapter = logAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // CORRIGIDO: Observa o Flow de dados do ViewModel
        lifecycleScope.launch {
            batteryViewModel.allLogs.collectLatest { logs ->
                logAdapter.submitList(logs)
                emptyStateTextView.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        fabClearLogs.setOnClickListener {
            batteryViewModel.clearAllLogs()
            Toast.makeText(this, "Histórico de bateria limpo", Toast.LENGTH_SHORT).show()
        }
    }
}
