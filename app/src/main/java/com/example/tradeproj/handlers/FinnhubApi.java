package com.example.tradeproj.handlers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.tradeproj.items.StockItem;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FinnhubApi {
    private static final String API_KEY = "cuqm2uhr01qsd02f4d2gcuqm2uhr01qsd02f4d30";
    private static FinnhubApi instance;
    private final RequestQueue requestQueue;
    private static final String TAG = "FinnhubApi";
    private final Map<String, StockItem> stockCache = new HashMap<>();
    private final Map<String, Long> lastRequestTimes = new HashMap<>();
    private static final long RATE_LIMIT_INTERVAL = 60000; // 1 minute
    private static final long RETRY_DELAY = 5000; // Retry API call after 5 sec if rate limit exceeded

    private FinnhubApi(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized FinnhubApi getInstance(Context context) {
        if (instance == null) {
            instance = new FinnhubApi(context);
        }
        return instance;
    }

    // Fetch top active stocks
    public void fetchTopActiveStocks(ResponseListener<List<String>> listener) {
        String url = "https://finnhub.io/api/v1/stock/symbol?exchange=US&token=" + API_KEY;
        Log.d(TAG, "Fetching top active stocks: " + url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        List<String> stockSymbols = new ArrayList<>();
                        for (int i = 0; i < Math.min(response.length(), 10); i++) {
                            JSONObject stock = response.getJSONObject(i);
                            String symbol = stock.optString("symbol", "");
                            if (!symbol.isEmpty()) {
                                stockSymbols.add(symbol);
                            }
                        }
                        listener.onResponse(stockSymbols);
                    } catch (Exception e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                        listener.onResponse(new ArrayList<>());
                    }
                },
                error -> {
                    Log.e(TAG, "API Request Failed: " + error.getMessage());
                    listener.onResponse(new ArrayList<>());
                });

        requestQueue.add(request);
    }

    //  Fetch stock prices with cache & rate limit handling
    public void fetchStockPrices(List<String> symbols, StockDataCallback callback, StockErrorCallback errorCallback) {
        long currentTime = System.currentTimeMillis();
        List<StockItem> stockItems = new ArrayList<>();
        List<String> missingSymbols = new ArrayList<>();
        AtomicInteger completedRequests = new AtomicInteger(0);

        //  Use cache if data is recent
        for (String symbol : symbols) {
            if (stockCache.containsKey(symbol) && (currentTime - lastRequestTimes.getOrDefault(symbol, 0L) < RATE_LIMIT_INTERVAL)) {
                Log.d(TAG, " Using cached stock data for: " + symbol);
                stockItems.add(stockCache.get(symbol));
            } else {
                missingSymbols.add(symbol);
            }
        }

        //  Return cached results if no new API calls are needed
        if (missingSymbols.isEmpty()) {
            callback.onSuccess(stockItems);
            return;
        }

        final int totalRequests = missingSymbols.size();

        for (int i = 0; i < totalRequests; i++) {
            final String symbol = missingSymbols.get(i);
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    makeApiRequest(symbol, stockItems, totalRequests, completedRequests, callback, errorCallback), i * 200);
        }
    }

    //  Helper function to make API requests with rate limit handling
    private void makeApiRequest(String symbol, List<StockItem> stockItems, int totalRequests, AtomicInteger completedRequests, StockDataCallback callback, StockErrorCallback errorCallback) {
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_KEY;
        Log.d(TAG, "📡 Fetching stock data: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    double price = response.optDouble("c", -1);
                    double change = response.optDouble("d", 0);
                    double percentChange = response.optDouble("dp", 0);
                    long volume = response.optLong("v", 0);
                    int quantity = 0;

                    double buyPrice = stockCache.containsKey(symbol) ? stockCache.get(symbol).getBuyPrice() : 0.0;

                    if (price != -1) {
                        StockItem stock = new StockItem(symbol, price, change, percentChange, volume, quantity, buyPrice);
                        stockCache.put(symbol, stock);
                        lastRequestTimes.put(symbol, System.currentTimeMillis());
                        stockItems.add(stock);
                        Log.d(TAG, " Updated Price for " + symbol + ": $" + price);
                    } else {
                        Log.e(TAG, " Invalid stock price for: " + symbol);
                    }

                    if (completedRequests.incrementAndGet() == totalRequests) {
                        callback.onSuccess(stockItems);
                    }
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 429) {
                        Log.e(TAG, " Rate Limit Exceeded for " + symbol + ". Retrying in " + RETRY_DELAY / 1000 + "s...");
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                makeApiRequest(symbol, stockItems, totalRequests, completedRequests, callback, errorCallback), RETRY_DELAY);
                    } else {
                        Log.e(TAG, " Error fetching stock data for " + symbol + ": " + error.getMessage());
                        if (completedRequests.incrementAndGet() == totalRequests) {
                            callback.onSuccess(stockItems);
                        }
                    }
                });

        requestQueue.add(request);
    }

    //  Fetch & update favorite stocks dynamically
    public void checkFavoriteStockUpdates(List<String> favoriteSymbols, ResponseListener<List<StockItem>> listener) {
        if (favoriteSymbols == null || favoriteSymbols.isEmpty()) {
            listener.onResponse(new ArrayList<>());
            return;
        }

        List<StockItem> updatedStocks = new ArrayList<>();
        AtomicInteger completedRequests = new AtomicInteger(0);

        for (String symbol : favoriteSymbols) {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_KEY;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        double price = response.optDouble("c", -1);
                        double change = response.optDouble("d", 0);
                        double percentChange = response.optDouble("dp", 0);
                        long volume = response.optLong("v", 0);
                        int quantity = 0;

                        double buyPrice = stockCache.containsKey(symbol) ? stockCache.get(symbol).getBuyPrice() : 0.0;

                        if (price != -1) {
                            updatedStocks.add(new StockItem(symbol, price, change, percentChange, volume, quantity, buyPrice));
                            Log.d(TAG, " Updated Favorite Stock: " + symbol + " @ $" + price);
                        }

                        if (completedRequests.incrementAndGet() == favoriteSymbols.size()) {
                            listener.onResponse(updatedStocks);
                        }
                    },
                    error -> {
                        Log.e(TAG, " Error fetching stock update for " + symbol + ": " + error.getMessage());
                        if (completedRequests.incrementAndGet() == favoriteSymbols.size()) {
                            listener.onResponse(updatedStocks);
                        }
                    });

            requestQueue.add(request);
        }
    }

    //  Define interfaces for API responses
    public interface ResponseListener<T> {
        void onResponse(T response);
    }

    @FunctionalInterface
    public interface StockDataCallback {
        void onSuccess(List<StockItem> stocks);
    }

    @FunctionalInterface
    public interface StockErrorCallback {
        void onError(String errorMessage);
    }
}
