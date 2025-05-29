package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        
        scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelScheduledUpdate(context)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_WIDGET_UPDATE) {
            updateAllWidgets(context)
            scheduleNextUpdate(context)
        }
    }
    


    companion object {
        private const val ACTION_WIDGET_UPDATE = "com.example.myapplication.WIDGET_UPDATE"
        private const val UPDATE_INTERVAL = 30000L
        
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            
            val currentTime = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())
            
            views.setTextViewText(R.id.widget_time, timeFormat.format(currentTime.time))
            views.setTextViewText(R.id.widget_date, dateFormat.format(currentTime.time))
            
            val nextAlarmInfo = getNextAlarmInfo(context)
            views.setTextViewText(R.id.widget_next_alarm, nextAlarmInfo)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun getNextAlarmInfo(context: Context): String {
            val alarmDataManager = AlarmDataManager(context)
            return alarmDataManager.getTimeUntilNextAlarm()
        }
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ClockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
        
        private fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ClockWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val nextUpdate = System.currentTimeMillis() + UPDATE_INTERVAL
            
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    nextUpdate,
                    pendingIntent
                )
            } catch (e: Exception) {
                try {
                    alarmManager.set(AlarmManager.RTC, nextUpdate, pendingIntent)
                } catch (e2: Exception) {
                    //
                }
            }
        }
        
        private fun cancelScheduledUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ClockWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
        }
    }
    
    private fun scheduleNextUpdate(context: Context) {
        Companion.scheduleNextUpdate(context)
    }
    
    private fun cancelScheduledUpdate(context: Context) {
        Companion.cancelScheduledUpdate(context)
    }
} 