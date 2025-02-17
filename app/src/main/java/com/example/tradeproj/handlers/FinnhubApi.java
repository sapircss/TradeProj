package com.example.tradeproj.handlers;

import android.content.Context;
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
import java.util.List;

public class FinnhubApi {
    private static final String API_KEY = "cu7laohr01qkucct97d0cu7laohr01qkucct97dg";
    private static FinnhubApi instance;
    private final RequestQueue requestQueue;
    private static final String TAG = "FinnhubApi";

    private FinnhubApi(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized FinnhubApi getInstance(Context context) {
        if (instance == null) {
            instance = new FinnhubApi(context);
        }
        return instance;
    }

    // ✅ Fixed: Added the missing method fetchTopActiveStocks
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
                        Log.d(TAG, "✅ Retrieved top stock symbols: " + stockSymbols);
                        listener.onResponse(stockSymbols);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ JSON Parsing Error: " + e.getMessage());
                        listener.onResponse(new ArrayList<>());
                    }
                },
                error -> {
                    Log.e(TAG, "❌ API Request Failed: " + error.getMessage(), error);
                    listener.onResponse(new ArrayList<>());
                });

        requestQueue.add(request);
    }

    public void fetchStockPrices(List<String> symbols, ResponseListener<List<StockItem>> listener) {
        if (symbols.isEmpty()) {
            listener.onResponse(new ArrayList<>());
            return;
        }

        List<StockItem> stockItems = new ArrayList<>();
        for (String symbol : symbols) {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_KEY;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        double price = response.optDouble("c", -1);
                        double change = response.optDouble("d", 0);
                        double percentChange = response.optDouble("dp", 0);

                        if (price != -1) {
                            // Pass 0 for volume as we are not using it in this method
                            stockItems.add(new StockItem(symbol, price, change, percentChange, 0));
                            if (stockItems.size() == symbols.size()) {
                                listener.onResponse(stockItems);
                            }
                        }
                    }, error -> Log.e(TAG, "API Error: " + error.getMessage()));
            requestQueue.add(request);
        }
    }



    public void checkFavoriteStockUpdates(List<String> favoriteSymbols, ResponseListener<List<StockItem>> listener) {
        if (favoriteSymbols.isEmpty()) {
            listener.onResponse(new ArrayList<>());
            return;
        }

        List<StockItem> updatedStocks = new ArrayList<>();
        for (String symbol : favoriteSymbols) {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_KEY;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        double price = response.optDouble("c", -1);
                        double change = response.optDouble("d", 0);
                        double percentChange = response.optDouble("dp", 0);

                        if (price != -1) {
                            updatedStocks.add(new StockItem(symbol, price, change, percentChange, 0));
                            if (updatedStocks.size() == favoriteSymbols.size()) {
                                listener.onResponse(updatedStocks);
                            }
                        }
                    }, error -> Log.e(TAG, "❌ Error fetching stock update for " + symbol));

            requestQueue.add(request);
        }
    }


    public interface ResponseListener<T> {
        void onResponse(T response);
    }
}
