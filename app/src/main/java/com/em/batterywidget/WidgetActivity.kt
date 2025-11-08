package com.em.batterywidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Atividade de configuração exibida quando um novo widget é adicionado à tela inicial.
 * Permite ao usuário escolher qual tipo de widget deseja criar.
 */
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

        findViewById<View>(R.id.btn_configure_icon).setOnClickListener {
            configureWidget(WidgetUpdater.TYPE_ICON_DETAIL)
        }
        findViewById<View>(R.id.btn_configure_graph).setOnClickListener {
            // O widget de gráfico tem seu próprio provider, mas o configuramos aqui.
            // A lógica de salvar o tipo não é estritamente necessária se os providers forem diferentes,
            // mas mantemos para consistência se quisermos unificá-los no futuro.
            configureWidget(WidgetUpdater.TYPE_GRAPH) 
        }
        findViewById<View>(R.id.btn_configure_text).setOnClickListener {
            configureWidget(WidgetUpdater.TYPE_TEXT_ONLY)
        }
    }

    private fun configureWidget(widgetType: Int) {
        lifecycleScope.launch {
            repository.saveWidgetType(appWidgetId, widgetType)

            val appWidgetManager = AppWidgetManager.getInstance(this@WidgetActivity)
            
            // Força a primeira atualização do widget recém-criado.
            // Escolhe o provider correto para chamar o onUpdate.
            val provider = if (widgetType == WidgetUpdater.TYPE_GRAPH) {
                BatteryGraphWidgetProvider()
            } else {
                BatteryAppWidgetProvider()
            }
            provider.onUpdate(this@WidgetActivity, appWidgetManager, intArrayOf(appWidgetId))

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}
