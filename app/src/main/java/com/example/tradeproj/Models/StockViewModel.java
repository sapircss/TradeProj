package com.example.tradeproj.Models;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.tradeproj.handlers.FinnhubApi;
import com.example.tradeproj.items.StockItem;
import java.util.ArrayList;
import java.util.List;

public class StockViewModel extends AndroidViewModel {
    private final MutableLiveData<List<StockItem>> stockList = new MutableLiveData<>();
    private final FinnhubApi finnhubApi;
    private static final String TAG = "StockViewModel";
    private final List<StockItem> topStockList = new ArrayList<>();

    public StockViewModel(Application application) {
        super(application);
        finnhubApi = FinnhubApi.getInstance(application.getApplicationContext());
    }

    public LiveData<List<StockItem>> getStockList() {
        return stockList;
    }

    public void fetchStockData(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            Log.e(TAG, "❌ No symbols provided for fetchStockData.");
            return;
        }

        finnhubApi.fetchStockPrices(symbols, stocks -> {
            if (stocks != null && !stocks.isEmpty()) {
                stockList.postValue(stocks);
                Log.d(TAG, "✅ Stock data updated: " + stocks.size() + " items.");
            } else {
                Log.e(TAG, "❌ Stock data fetch returned empty.");
                stockList.postValue(null);
            }
        });
    }

    public void fetchTopStocks() {
        if (!topStockList.isEmpty()) {
            stockList.postValue(topStockList);
            Log.d(TAG, "✅ Using cached top stocks.");
            return;
        }

        finnhubApi.fetchTopActiveStocks(symbols -> {
            if (symbols != null && !symbols.isEmpty()) {
                Log.d(TAG, "✅ Retrieved top stocks: " + symbols);
                finnhubApi.fetchStockPrices(symbols, stocks -> {
                    if (stocks != null && !stocks.isEmpty()) {
                        topStockList.clear();
                        topStockList.addAll(stocks);
                        stockList.postValue(topStockList);
                        Log.d(TAG, "✅ Top 10 stocks updated.");
                    } else {
                        Log.e(TAG, "❌ Error fetching stock data for top stocks.");
                    }
                });
            } else {
                Log.e(TAG, "❌ No top stocks retrieved.");
            }
        });
    }

}
