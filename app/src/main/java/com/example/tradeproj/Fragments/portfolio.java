package com.example.tradeproj.Fragments;

import android.graphics.Color;
import android.os.Bundle;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.Data.StocksAdapter;
import com.example.tradeproj.R;
import com.example.tradeproj.handlers.FirebaseManager;
import com.example.tradeproj.handlers.FinnhubApi;
import com.example.tradeproj.Models.UserPortfolio;
import com.example.tradeproj.items.StockItem;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class portfolio extends Fragment {
    private RecyclerView portfolioRecyclerView;
    private StocksAdapter adapter;
    private FirebaseManager firebaseManager;
    private FinnhubApi finnhubApi;
    private TextView totalProfitLossTextView;
    private TableLayout profitLossTable;
    private Button backButton, depositButton, watchlistButton;

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

        adapter = new StocksAdapter(new ArrayList<>(), getContext(), stock -> showActionDialog(view, stock.getSymbol()));
        portfolioRecyclerView.setAdapter(adapter);

        firebaseManager = FirebaseManager.getInstance();
        finnhubApi = FinnhubApi.getInstance(requireContext());

        loadPortfolio();

        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.trade));
        depositButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.deposit));
        watchlistButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.watchList));

        return view;
    }

    private void loadPortfolio() {
        firebaseManager.getUserPortfolio().thenAccept(portfolio -> {
            if (portfolio == null || portfolio.getHoldings().isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    totalProfitLossTextView.setText("No holdings available");
                    Toast.makeText(getContext(), "No stocks in portfolio!", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            List<String> symbols = new ArrayList<>(portfolio.getHoldings().keySet());

            //  Reset the table and RecyclerView
            requireActivity().runOnUiThread(() -> {
                profitLossTable.removeAllViews();
                addTableHeader();
                profitLossTable.setVisibility(View.VISIBLE);
            });

            fetchStockPricesWithDelay(symbols, portfolio);
        }).exceptionally(e -> {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Error loading portfolio", Toast.LENGTH_SHORT).show()
            );
            return null;
        });
    }

    private void fetchStockPricesWithDelay(List<String> symbols, UserPortfolio portfolio) {
        List<StockItem> updatedStockItems = new ArrayList<>();
        AtomicInteger completedRequests = new AtomicInteger(0);
        AtomicReference<Double> totalPnl = new AtomicReference<>(0.0); // To store total P&L

        for (String symbol : symbols) {
            finnhubApi.fetchStockPrices(
                    List.of(symbol),
                    stockItems -> {
                        if (stockItems == null || stockItems.isEmpty()) {
                            return;
                        }
                        StockItem stock = stockItems.get(0);

                        if (portfolio.getHoldings().containsKey(stock.getSymbol())) {
                            UserPortfolio.Holding holding = portfolio.getHoldings().get(stock.getSymbol());
                            double buyPrice = holding.getAveragePrice();
                            int quantity = holding.getQuantity();
                            double currentPrice = stock.getPrice();
                            double pnl = (currentPrice - buyPrice) * quantity;

                            stock.updateStock(currentPrice, pnl, (pnl / buyPrice) * 100, quantity);
                            updatedStockItems.add(stock);

                            requireActivity().runOnUiThread(() ->
                                    addStockRow(stock.getSymbol(), buyPrice, currentPrice, quantity, pnl)
                            );

                            totalPnl.updateAndGet(v -> v + pnl); // Accumulate total P&L
                        }

                        if (completedRequests.incrementAndGet() == symbols.size()) {
                            updateUI(updatedStockItems);
                            updateTotalProfitLoss(totalPnl.get()); // Update UI with total P&L
                        }
                    },
                    errorMessage -> {}
            );
        }
    }

    private void updateTotalProfitLoss(double totalPnl) {
        requireActivity().runOnUiThread(() -> {
            totalProfitLossTextView.setText(String.format("Total Profit/Loss: $%.2f", totalPnl));
            totalProfitLossTextView.setTextColor(totalPnl >= 0 ? Color.GREEN : Color.RED);
        });
    }



    private void updateUI(List<StockItem> updatedStockItems) {
        requireActivity().runOnUiThread(() -> {
            adapter.updateStocks(updatedStockItems);
            adapter.notifyDataSetChanged(); // Ensure RecyclerView updates properly
        });
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
        requireActivity().runOnUiThread(() -> {
            TableRow row = new TableRow(getContext());

            row.addView(createCell(symbol));
            row.addView(createCell(String.format("$%.2f", buyPrice)));
            row.addView(createCell(String.format("$%.2f", currPrice)));
            row.addView(createCell(String.valueOf(quantity)));

            TextView pnlCell = createCell(String.format("$%.2f", pnl));
            pnlCell.setTextColor(pnl >= 0 ? Color.GREEN : Color.RED);
            row.addView(pnlCell);

            profitLossTable.addView(row);
            profitLossTable.invalidate(); // Force refresh
        });
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



    private void showActionDialog(View view, String stockSymbol) {
        new AlertDialog.Builder(getContext())
                .setTitle("Choose an action")
                .setMessage("Do you want to Buy, Sell, or Deposit cash?")
                .setPositiveButton("Buy", (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedStock", stockSymbol);
                    Navigation.findNavController(view).navigate(R.id.buy, bundle);
                })
                .setNegativeButton("Sell", (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedStock", stockSymbol);
                    Navigation.findNavController(view).navigate(R.id.sell, bundle);
                })
                .setNeutralButton("Deposit Cash", (dialog, which) -> Navigation.findNavController(view).navigate(R.id.deposit))
                .show();
    }
}
