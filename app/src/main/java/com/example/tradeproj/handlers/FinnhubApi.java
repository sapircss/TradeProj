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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinnhubApi {
    private static final String API_KEY = "cuqm2uhr01qsd02f4d2gcuqm2uhr01qsd02f4d30";
    private static FinnhubApi instance;
    private final RequestQueue requestQueue;
    private static final String TAG = "FinnhubApi";
    private final Map<String, StockItem> stockCache = new HashMap<>();
    private long lastApiCallTime = 0;
    private static final long RATE_LIMIT_INTERVAL = 60000; // 5 seconds

    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;

    private FinnhubApi(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized FinnhubApi getInstance(Context context) {
        if (instance == null) {
            instance = new FinnhubApi(context);
        }
        return instance;
    }

    // ✅ Fetch top active stocks
    public void fetchTopActiveStocks(ResponseListener<List<String>> listener) {
        String url = "https://finnhub.io/api/v1/stock/symbol?exchange=US&token=" + API_KEY;
        Log.d(TAG, "Fetching top active stocks from: " + url);

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
                        Log.e(TAG, "❌ JSON Parsing Error: " + e.getMessage());
                        listener.onResponse(new ArrayList<>());
                    }
                },
                error -> {
                    Log.e(TAG, "❌ API Request Failed: " + error.getMessage());
                    listener.onResponse(new ArrayList<>());
                });

        requestQueue.add(request);
    }

    // ✅ Fetch stock prices with success and error handling
    public void fetchStockPrices(List<String> symbols, StockDataCallback callback, StockErrorCallback errorCallback) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastApiCallTime < RATE_LIMIT_INTERVAL && !stockCache.isEmpty()) {
            callback.onSuccess(new ArrayList<>(stockCache.values()));
            return;
        }

        lastApiCallTime = currentTime;
        List<StockItem> stockItems = new ArrayList<>();

        for (String symbol : symbols) {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_KEY;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        double price = response.optDouble("c", -1);
                        if (price != -1) {
                            StockItem stock = new StockItem(symbol, price, response.optDouble("d", 0), response.optDouble("dp", 0), 0);
                            stockCache.put(symbol, stock);
                            stockItems.add(stock);
                        }
                        if (stockItems.size() == symbols.size()) {
                            callback.onSuccess(stockItems);
                        }
                    },
                    error -> {
                        if (error.networkResponse != null && error.networkResponse.statusCode == 429 && retryCount < MAX_RETRIES) {
                            retryCount++;
                            Log.e(TAG, "⚠️ API rate limit hit, retrying... (" + retryCount + ")");
                            new Handler(Looper.getMainLooper()).postDelayed(
                                    () -> fetchStockPrices(symbols, callback, errorCallback),
                                    2000 * retryCount); // Retry with delay
                        } else {
                            errorCallback.onError("❌ Failed to fetch stock updates.");
                        }
                    });

            requestQueue.add(request);
        }
    }


    // ✅ Fetch stock prices with rate limiting
    private void requestStockPrices(List<String> symbols, StockDataCallback successCallback, StockErrorCallback errorCallback) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastApiCallTime < RATE_LIMIT_INTERVAL) {
            Log.w(TAG, "⚠️ API Rate Limit reached, delaying request.");
            new Handler(Looper.getMainLooper()).postDelayed(() -> requestStockPrices(symbols, successCallback, errorCallback), RATE_LIMIT_INTERVAL);
            return;
        }

        lastApiCallTime = currentTime;
        fetchStockPrices(symbols, successCallback, errorCallback);
    }

    // ✅ Check favorite stock updates
    public void checkFavoriteStockUpdates(List<String> favoriteSymbols, ResponseListener<List<StockItem>> listener) {
        if (favoriteSymbols == null || favoriteSymbols.isEmpty()) {
            listener.onResponse(new ArrayList<>());
            return;
        }

        List<StockItem> updatedStocks = new ArrayList<>();
        int[] completedRequests = {0};

        for (String symbol : favoriteSymbols) {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_KEY;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        double price = response.optDouble("c", -1);
                        double change = response.optDouble("d", 0);
                        double percentChange = response.optDouble("dp", 0);

                        if (price != -1) {
                            updatedStocks.add(new StockItem(symbol, price, change, percentChange, 0));
                        }

                        completedRequests[0]++;
                        if (completedRequests[0] == favoriteSymbols.size()) {
                            listener.onResponse(updatedStocks);
                        }
                    },
                    error -> {
                        Log.e(TAG, "❌ Error fetching stock update for " + symbol);
                        completedRequests[0]++;
                        if (completedRequests[0] == favoriteSymbols.size()) {
                            listener.onResponse(updatedStocks);
                        }
                    });

            requestQueue.add(request);
        }
    }

    // ✅ Define interfaces for API responses
    public interface ResponseListener<T> {
        void onResponse(T response);
    }

    // 🔹 **Fixed: `StockDataCallback` is now a valid functional interface**
    @FunctionalInterface
    public interface StockDataCallback {
        void onSuccess(List<StockItem> stocks);
    }

    // ✅ Separate interface for errors
    @FunctionalInterface
    public interface StockErrorCallback {
        void onError(String errorMessage);
    }
}
