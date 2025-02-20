package com.example.tradeproj.Data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.R;
import com.example.tradeproj.handlers.FirebaseManager;
import com.example.tradeproj.items.StockItem;
import java.util.List;
import java.util.Locale;

public class StocksAdapter extends RecyclerView.Adapter<StocksAdapter.StockViewHolder> {
    private List<StockItem> stockList;
    private final Context context;
    private final OnStockClickListener stockClickListener;
    private final FirebaseManager firebaseManager;

    public interface OnStockClickListener {
        void onStockClick(StockItem stock);
    }

    public StocksAdapter(List<StockItem> stockList, Context context, OnStockClickListener listener) {
        this.stockList = stockList;
        this.context = context;
        this.stockClickListener = listener;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        StockItem stock = stockList.get(position);

        // ✅ Set stock details with Locale-safe formatting
        holder.symbol.setText(stock.getSymbol());
        holder.price.setText(String.format(Locale.US, "$%.2f", stock.getPrice()));
        holder.change.setText(String.format(Locale.US, "%.2f%%", stock.getPercentChange()));

        // ✅ Set text color based on price movement
        int color = stock.getPercentChange() >= 0 ?
                context.getColor(R.color.positive) :
                context.getColor(R.color.negative);
        holder.change.setTextColor(color);

        // ✅ Set favorite icon **without flickering**
        holder.favoriteButton.setImageResource(
                stock.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_border
        );

        // ✅ Handle Favorite Button Click - **Optimized for Instant UI Updates**
        holder.favoriteButton.setOnClickListener(v -> {
            boolean newFavoriteStatus = !stock.isFavorite();
            stock.setFavorite(newFavoriteStatus); // ✅ Update state immediately
            holder.favoriteButton.setImageResource(
                    newFavoriteStatus ? R.drawable.ic_star_filled : R.drawable.ic_star_border
            );

            // ✅ Update Firebase asynchronously
            if (newFavoriteStatus) {
                firebaseManager.addStockToFavorites(stock.getSymbol());
                Toast.makeText(context, stock.getSymbol() + " added to favorites", Toast.LENGTH_SHORT).show();
            } else {
                firebaseManager.removeStockFromFavorites(stock.getSymbol());
                Toast.makeText(context, stock.getSymbol() + " removed from favorites", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Handle item click for stock actions
        holder.itemView.setOnClickListener(v -> stockClickListener.onStockClick(stock));
    }


    @Override
    public int getItemCount() {
        return stockList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateStocks(List<StockItem> newStockList) {
        stockList.clear();
        stockList.addAll(newStockList);
        notifyDataSetChanged(); // ✅ Ensures UI refresh
    }



    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView symbol, price, change;
        ImageButton favoriteButton;

        public StockViewHolder(View itemView) {
            super(itemView);
            symbol = itemView.findViewById(R.id.stock_symbol);
            price = itemView.findViewById(R.id.stock_price);
            change = itemView.findViewById(R.id.stock_change);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
        }
    }
}
