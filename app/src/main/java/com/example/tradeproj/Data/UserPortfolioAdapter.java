package com.example.tradeproj.Data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.R;
import com.example.tradeproj.Models.HoldingsPortfolio;
import java.util.List;

public class UserPortfolioAdapter extends RecyclerView.Adapter<UserPortfolioAdapter.ViewHolder> {
    private final List<HoldingsPortfolio> holdings;

    public UserPortfolioAdapter(List<HoldingsPortfolio> holdings) {
        this.holdings = holdings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_portfolio_holding, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HoldingsPortfolio holding = holdings.get(position);
        holder.symbol.setText(holding.getSymbol());
        holder.shares.setText(String.format("Shares: %d", holding.getShares()));
        holder.price.setText(String.format("$%.2f", holding.getPrice()));
        holder.totalValue.setText(String.format("Total: $%.2f", holding.getTotalValue()));
        holder.holdingChange.setText(String.format("%.2f%%", holding.getHoldingChange()));

        // Set color based on profit/loss
        int color = (holding.getHoldingChange() >= 0) ?
                holder.itemView.getContext().getColor(R.color.positive) :
                holder.itemView.getContext().getColor(R.color.negative);
        holder.holdingChange.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return holdings != null ? holdings.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView symbol, shares, price, totalValue, holdingChange;

        ViewHolder(View itemView) {
            super(itemView);
            symbol = itemView.findViewById(R.id.holding_symbol);
            shares = itemView.findViewById(R.id.holding_shares);
            price = itemView.findViewById(R.id.holding_price); // ✅ FIXED ID
            totalValue = itemView.findViewById(R.id.holding_total_value); // ✅ FIXED ID
            holdingChange = itemView.findViewById(R.id.holding_change);
        }
    }
}