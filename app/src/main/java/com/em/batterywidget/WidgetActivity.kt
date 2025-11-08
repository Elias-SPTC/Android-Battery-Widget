package com.em.batterywidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder // <-- IMPORT ADICIONADO
import androidx.work.WorkManager             // <-- IMPORT ADICIONADO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WidgetActivity : AppCompatActivity(), KoinComponent {

    private val repository: BatteryRepository by inject()
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_widget_configuration)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        findViewById<View>(R.id.btn_configure_standing_battery).setOnClickListener {
            configureWidget(WidgetUpdater.TYPE_ICON_DETAIL)
        }
        
        findViewById<View>(R.id.btn_configure_lying_battery).setOnClickListener {
            configureWidget(WidgetUpdater.TYPE_TEXT_ONLY)
        }

        findViewById<View>(R.id.btn_configure_details_table).setOnClickListener {
            configureWidget(WidgetUpdater.TYPE_DETAILS_TABLE)
        }

        findViewById<View>(R.id.btn_configure_graph).setOnClickListener {
            configureWidget(WidgetUpdater.TYPE_GRAPH)
        }
    }

    private fun configureWidget(widgetType: Int) {
        lifecycleScope.launch {
            repository.saveWidgetType(appWidgetId, widgetType)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            
            // Força a primeira atualização
            val workRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
            WorkManager.getInstance(applicationContext).enqueue(workRequest)

            finish()
        }
    }
}
