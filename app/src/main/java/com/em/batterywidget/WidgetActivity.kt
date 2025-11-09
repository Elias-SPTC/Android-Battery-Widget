package com.em.batterywidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.em.batterywidget.databinding.ActivityWidgetConfigurationBinding
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WidgetActivity : AppCompatActivity(), KoinComponent {

    private lateinit var binding: ActivityWidgetConfigurationBinding
    private val repository: BatteryRepository by inject()
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // CORRIGIDO: Usa as constantes do objeto WidgetType
        binding.btnConfigureStandingBattery.setOnClickListener {
            configureWidget(WidgetType.TYPE_ICON_DETAIL)
        }
        
        binding.btnConfigureLyingBattery.setOnClickListener {
            configureWidget(WidgetType.TYPE_TEXT_ONLY)
        }

        binding.btnConfigureDetailsTable.setOnClickListener {
            configureWidget(WidgetType.TYPE_DETAILS_TABLE)
        }

        binding.btnConfigureGraph.setOnClickListener {
            configureWidget(WidgetType.TYPE_GRAPH)
        }
    }

    private fun configureWidget(widgetType: Int) {
        lifecycleScope.launch {
            repository.saveWidgetType(appWidgetId, widgetType)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            
            val workRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
            WorkManager.getInstance(applicationContext).enqueue(workRequest)

            finish()
        }
    }
}
