package com.example.tradeproj.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.Data.UserPortfolioAdapter;
import com.example.tradeproj.Models.HoldingsPortfolio;
import com.example.tradeproj.R;
import com.example.tradeproj.handlers.FirebaseManager;
import com.example.tradeproj.handlers.FinnhubApi;
import com.example.tradeproj.Models.UserPortfolio;
import com.example.tradeproj.items.StockItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class portfolio extends Fragment {
    private RecyclerView portfolioRecyclerView;
    private UserPortfolioAdapter portfolioAdapter;
    private FirebaseManager firebaseManager;
    private FinnhubApi finnhubApi;
    private TextView totalProfitLossTextView;
    private TableLayout profitLossTable;
    private Button backButton, depositButton, watchlistButton;

    private static final String TAG = "PortfolioFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio, container, false);

        portfolioRecyclerView = view.findViewById(R.id.portfolioRv);
        totalProfitLossTextView = view.findViewById(R.id.totalProfitLoss);
        profitLossTable = view.findViewById(R.id.profitLossTable);
        backButton = view.findViewById(R.id.backPort);
        depositButton = view.findViewById(R.id.depositCash);
        watchlistButton = view.findViewById(R.id.watchlistButton);

        portfolioRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        portfolioAdapter = new UserPortfolioAdapter(new ArrayList<>());
        portfolioRecyclerView.setAdapter(portfolioAdapter);

        firebaseManager = FirebaseManager.getInstance();
        finnhubApi = FinnhubApi.getInstance(requireContext());

        fetchPortfolio();

        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.trade));
        depositButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.deposit));
        watchlistButton.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.watchList);
            } catch (Exception e) {
                Log.e(TAG, "❌ Navigation to Watchlist failed", e);
                Toast.makeText(getContext(), "Error opening Watchlist", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "📢 Resuming Portfolio Page, fetching stocks...");
        fetchPortfolio();
    }

    /**
     * ✅ Fetches the user's portfolio from Firebase and updates UI.
     */
    private void fetchPortfolio() {
        firebaseManager.getUserPortfolio().thenAccept(portfolio -> {
            if (portfolio == null || portfolio.getHoldings().isEmpty()) {
                Log.e(TAG, "⚠ No stocks found in portfolio.");
                requireActivity().runOnUiThread(() -> {
                    totalProfitLossTextView.setText("No holdings available");
                    portfolioAdapter.updatePortfolio(new ArrayList<>());
                });
                return;
            }

            List<String> symbols = new ArrayList<>(portfolio.getHoldings().keySet());
            Log.d(TAG, "🔍 Portfolio Symbols: " + symbols);

            finnhubApi.fetchStockPrices(symbols,
                    stockItems -> requireActivity().runOnUiThread(() -> {
                        Log.d(TAG, "✅ Updating UI with portfolio stocks.");
                        updatePortfolioUI(portfolio, stockItems);
                    }),
                    error -> Log.e(TAG, "❌ Error fetching portfolio stock prices: " + error)
            );
        }).exceptionally(e -> {
            Log.e(TAG, "❌ Error loading portfolio", e);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Error loading portfolio", Toast.LENGTH_SHORT).show()
            );
            return null;
        });
    }

    /**
     * ✅ Updates the portfolio UI with stock data and calculates profit/loss.
     */
    private void updatePortfolioUI(UserPortfolio portfolio, List<StockItem> stockItems) {
        if (portfolio == null || portfolio.getHoldings().isEmpty()) {
            Log.e(TAG, "❌ No holdings found in portfolio.");
            totalProfitLossTextView.setText("No holdings available");
            return;
        }

        double totalPnl = 0;
        double totalPortfolioValue = 0;
        List<HoldingsPortfolio> updatedHoldings = new ArrayList<>();
        profitLossTable.removeAllViews();
        addTableHeader();

        Map<String, StockItem> stockMap = new HashMap<>();
        for (StockItem stock : stockItems) {
            stockMap.put(stock.getSymbol(), stock);
        }

        for (String holdingSymbol : portfolio.getHoldings().keySet()) {
            UserPortfolio.Holding holding = portfolio.getHoldings().get(holdingSymbol);
            if (holding == null) continue; // ✅ Ensure holding exists

            double buyPrice = holding.getAveragePrice();
            int quantity = holding.getQuantity();
            double currentPrice = stockMap.containsKey(holdingSymbol) ? stockMap.get(holdingSymbol).getPrice() : buyPrice;

            double holdingValue = quantity * currentPrice;
            totalPortfolioValue += holdingValue;
            double percentChange = (buyPrice > 0) ? ((currentPrice - buyPrice) / buyPrice) * 100 : 0;
            double pnl = (currentPrice - buyPrice) * quantity;
            totalPnl += pnl;

            HoldingsPortfolio holdingPortfolio = new HoldingsPortfolio(
                    holdingSymbol, quantity, currentPrice, holdingValue, percentChange, 0
            );

            updatedHoldings.add(holdingPortfolio);
            addStockRow(holdingSymbol, buyPrice, currentPrice, quantity, pnl);
        }

        for (HoldingsPortfolio holding : updatedHoldings) {
            double portfolioPercentage = (totalPortfolioValue > 0) ? (holding.getTotalValue() / totalPortfolioValue) * 100 : 0;
            holding.setPortfolioPercentage(portfolioPercentage);
        }

        portfolioAdapter.updatePortfolio(updatedHoldings);
        totalProfitLossTextView.setText(String.format(Locale.US, "Total Profit/Loss: $%.2f", totalPnl));
        totalProfitLossTextView.setTextColor(totalPnl >= 0 ? Color.GREEN : Color.RED);

        Log.d(TAG, "✅ Portfolio UI Updated.");
    }

    private void addTableHeader() {
        TableRow headerRow = new TableRow(getContext());
        headerRow.addView(createHeaderCell("Stock"));
        headerRow.addView(createHeaderCell("Buy Price"));
        headerRow.addView(createHeaderCell("Current Price"));
        headerRow.addView(createHeaderCell("Qty"));
        headerRow.addView(createHeaderCell("P&L ($)"));
        profitLossTable.addView(headerRow);
    }

    private void addStockRow(String symbol, double buyPrice, double currPrice, int quantity, double pnl) {
        TableRow row = new TableRow(getContext());

        row.addView(createCell(symbol));
        row.addView(createCell(String.format(Locale.US, "$%.2f", buyPrice)));
        row.addView(createCell(String.format(Locale.US, "$%.2f", currPrice)));
        row.addView(createCell(String.valueOf(quantity)));

        TextView pnlCell = createCell(String.format(Locale.US, "$%.2f", pnl));
        pnlCell.setTextColor(pnl >= 0 ? Color.GREEN : Color.RED);
        row.addView(pnlCell);

        profitLossTable.addView(row);
    }

    private TextView createHeaderCell(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextSize(16);
        textView.setPadding(8, 8, 8, 8);
        textView.setTextColor(Color.BLACK);
        textView.setBackgroundColor(Color.LTGRAY);
        return textView;
    }

    private TextView createCell(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextSize(14);
        textView.setPadding(8, 8, 8, 8);
        textView.setTextColor(Color.BLACK);
        return textView;
    }
}
