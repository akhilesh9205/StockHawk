package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.DetailGraphActivity;


/**
 * Implementation of App Widget functionality.
 */
public class StockWidget extends AppWidgetProvider {

    private static final String TAG = StockWidget.class.getSimpleName();

    private static final String INTENT_ACTION = "com.sam_chordas.android.stockhawk.WIDGET_CLICKED";
    private static DataObserver sDataObserver;
    private static HandlerThread sWorkerThread;
    private static Handler sHandler;

    static {
        sWorkerThread = new HandlerThread("Widget_data_worker");
        sWorkerThread.start();
        sHandler = new Handler(sWorkerThread.getLooper());
    }

    public StockWidget() {
        Log.d(TAG, "Constructor called");
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Intent intent = new Intent(context,WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stock_widget);
        views.setRemoteAdapter(R.id.list, intent);


        Intent clickIntent = new Intent(context, StockWidget.class);
        clickIntent.setAction(INTENT_ACTION);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        clickIntent.setData(Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME)));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 101, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.list, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called");
        if (INTENT_ACTION.equals(intent.getAction())) {
            String symbol = intent.getStringExtra("symbol");

            Intent i = new Intent(context, DetailGraphActivity.class);
            i.putExtra("symbol", symbol);
            i.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() called");
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled() called");
        // Enter relevant functionality for when the first widget is created
        ContentResolver resolver = context.getContentResolver();

        if (sDataObserver == null) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, StockWidget.class);
            sDataObserver = new DataObserver(manager, componentName, sHandler);
            resolver.registerContentObserver(QuoteProvider.Quotes.CONTENT_URI, true, sDataObserver);
        }

    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled() called");
        // Enter relevant functionality for when the last widget is disabled
        ContentResolver resolver = context.getContentResolver();
        if (sDataObserver != null) {
            resolver.unregisterContentObserver(sDataObserver);
        }
    }

}

