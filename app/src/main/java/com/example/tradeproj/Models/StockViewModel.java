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
    private static final long CACHE_EXPIRY_TIME = 60000; // 1 Minute Cache

    private final MutableLiveData<List<StockItem>> stockList = new MutableLiveData<>();
    private final MutableLiveData<List<StockItem>> topStockList = new MutableLiveData<>(); // ✅ Cache for top stocks
    private final Map<String, StockItem> stockCache = new HashMap<>();

    private final FinnhubApi finnhubApi;
    private long lastUpdatedTime = 0;
    private long lastTopStocksUpdateTime = 0; // ✅ Track top stock cache time

    private static final String TAG = "StockViewModel";

    public StockViewModel(Application application) {
        super(application);
        finnhubApi = FinnhubApi.getInstance(application.getApplicationContext());
    }

    public LiveData<List<StockItem>> getStockList() {
        return stockList;
    }

    /**
     * ✅ Fetches stock data with caching to avoid redundant API calls.
     */
    public void fetchStockData(List<String> symbols) {
        long currentTime = System.currentTimeMillis();
        List<StockItem> cachedStocks = new ArrayList<>();

        // ✅ Check if requested symbols exist in cache
        boolean allCached = true;
        for (String symbol : symbols) {
            if (stockCache.containsKey(symbol) && (currentTime - lastUpdatedTime < CACHE_EXPIRY_TIME)) {
                cachedStocks.add(stockCache.get(symbol));
            } else {
                allCached = false;
            }
        }

        if (allCached) {
            Log.d(TAG, "✅ Returning cached stock data.");
            stockList.postValue(cachedStocks);
            return;
        }

        // ✅ Fetch latest prices if cache is outdated
        Log.d(TAG, "🔄 Fetching stock prices from API...");
        finnhubApi.fetchStockPrices(symbols,
                stocks -> {
                    if (stocks != null && !stocks.isEmpty()) {
                        for (StockItem stock : stocks) {
                            stockCache.put(stock.getSymbol(), stock); // ✅ Update cache only for fetched stocks
                        }
                        lastUpdatedTime = System.currentTimeMillis();
                        stockList.postValue(new ArrayList<>(stockCache.values()));
                    }
                },
                error -> Log.e(TAG, "❌ Error fetching stock prices: " + error)
        );
    }

    /**
     * ✅ Fetches the top 10 stocks and caches them.
     */
    public void fetchTopStocks() {
        long currentTime = System.currentTimeMillis();

        // ✅ Use cached top stocks if not expired
        if (topStockList.getValue() != null && !topStockList.getValue().isEmpty()
                && (currentTime - lastTopStocksUpdateTime < CACHE_EXPIRY_TIME)) {
            Log.d(TAG, "✅ Returning cached top stocks.");
            stockList.postValue(topStockList.getValue());  // ✅ Fix: Ensure UI gets updated
            return;
        }

        Log.d(TAG, "🔍 Fetching top 10 active stocks...");
        finnhubApi.fetchTopActiveStocks(symbols -> {
            if (symbols == null || symbols.isEmpty()) {
                Log.e(TAG, "❌ No symbols available. Failed to fetch top stocks.");
                return;
            }

            // ✅ Fetch stock details for top stocks
            finnhubApi.fetchStockPrices(symbols,
                    stocks -> {
                        if (stocks == null || stocks.isEmpty()) {
                            Log.e(TAG, "❌ No stock prices received for top stocks.");
                            return;
                        }

                        Log.d(TAG, "✅ Successfully fetched top stocks.");
                        topStockList.postValue(stocks);  // ✅ Ensure LiveData is updated
                        stockList.postValue(stocks); // ✅ Fix UI not updating
                        lastTopStocksUpdateTime = System.currentTimeMillis();
                    },
                    error -> Log.e(TAG, "❌ Error fetching top stock prices: " + error)
            );
        });
    }


}
