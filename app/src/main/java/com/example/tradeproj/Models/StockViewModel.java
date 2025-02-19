package com.example.tradeproj.Models;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.tradeproj.handlers.FinnhubApi;
import com.example.tradeproj.items.StockItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockViewModel extends AndroidViewModel {
    public static final long CACHE_DURATION = 60000; // 1 Minute Cache

    private final MutableLiveData<List<StockItem>> stockList = new MutableLiveData<>();
    private final Map<String, StockItem> stockCache = new HashMap<>();

    private final FinnhubApi finnhubApi;
    private final List<StockItem> cachedStockList = new ArrayList<>();
    private long lastUpdatedTime = 0;

    private static final long CACHE_EXPIRY_TIME = 60000; // 1 Minute


    public StockViewModel(Application application) {
        super(application);
        // 🔹 FIX: Pass application context to FinnhubApi
        finnhubApi = FinnhubApi.getInstance(application.getApplicationContext());
    }

    public LiveData<List<StockItem>> getStockList() {
        return stockList;
    }

    public void fetchStockData(List<String> symbols) {
        long currentTime = System.currentTimeMillis();

        // ✅ Use cached data if within expiry time
        if (!stockCache.isEmpty() && (currentTime - lastUpdatedTime < CACHE_EXPIRY_TIME)) {
            List<StockItem> cachedStocks = new ArrayList<>(stockCache.values());
            stockList.postValue(cachedStocks);
            return;
        }

        finnhubApi.fetchStockPrices(symbols,
                stocks -> {
                    if (stocks != null && !stocks.isEmpty()) {
                        stockCache.clear();
                        for (StockItem stock : stocks) {
                            stockCache.put(stock.getSymbol(), stock);
                        }
                        lastUpdatedTime = System.currentTimeMillis();
                        stockList.postValue(new ArrayList<>(stockCache.values()));
                    }
                },
                error -> Log.e("StockViewModel", "❌ Error fetching stock prices: " + error)
        );
    }


    public void fetchTopStocks() {
        finnhubApi.fetchTopActiveStocks(symbols -> {
            if (symbols == null || symbols.isEmpty()) {
                Log.e("StockViewModel", "❌ Failed to fetch top stocks");
                return;
            }
            fetchStockData(symbols);
        });
    }


}
