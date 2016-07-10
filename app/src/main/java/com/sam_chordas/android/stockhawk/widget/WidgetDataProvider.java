package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by CodeMyMobile on 10-07-2016.
 */
public class WidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private Cursor mCursor;
    private Context mContext;
    private Intent mIntent;

    public WidgetDataProvider(Context mContext, Intent intent) {
        this.mContext = mContext;
        this.mIntent = intent;
    }

    private void initCursor() {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onCreate() {
        initCursor();
    }

    @Override
    public void onDataSetChanged() {
        initCursor();
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {

        String symbol = "";
        String price = "";
        String change = "";
        int isUp = 1;
        if (mCursor.moveToPosition(position)) {
            final int symbolIndex = mCursor.getColumnIndex(QuoteColumns.SYMBOL);
            final int bidPriceIndex = mCursor.getColumnIndex(QuoteColumns.BIDPRICE);
            final int changeIndex = mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE);
            final int isUpIndex = mCursor.getColumnIndex(QuoteColumns.ISUP);
            symbol = mCursor.getString(symbolIndex);
            price = mCursor.getString(bidPriceIndex);
            change = mCursor.getString(changeIndex);
            isUp = mCursor.getInt(isUpIndex);
        }


        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
        remoteViews.setTextViewText(R.id.stock_symbol, symbol);
        remoteViews.setTextViewText(R.id.bid_price, price);
        remoteViews.setTextViewText(R.id.change, change);

        if (isUp == 1){
            remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
        } else{
            remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
        }


        Intent intent = new Intent();
        final Bundle extras = new Bundle();
        extras.putString("symbol", symbol);
        intent.putExtras(extras);
        remoteViews.setOnClickFillInIntent(R.id.item_parent, intent);
        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
