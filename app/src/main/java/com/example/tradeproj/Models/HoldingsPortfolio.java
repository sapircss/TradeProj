package com.example.tradeproj.Models;

import com.example.tradeproj.items.StockItem;

import java.util.ArrayList;
import java.util.List;

public class HoldingsPortfolio {
    private String symbol;
    private int shares;
    private double price;
    private double totalValue;
    private double holdingChange;
    private double portfolioPercentage;

    public HoldingsPortfolio(String symbol, int shares, double price, double totalValue, double holdingChange, double portfolioPercentage) {
        this.symbol = symbol;
        this.shares = shares;
        this.price = price;
        this.totalValue = totalValue;
        this.holdingChange = holdingChange;
        this.portfolioPercentage = portfolioPercentage;
    }

    private List<HoldingsPortfolio> convertStockItemsToHoldings(List<StockItem> stockItems, UserPortfolio portfolio) {
        List<HoldingsPortfolio> holdings = new ArrayList<>();
        double totalPortfolioValue = 0;

        for (StockItem stock : stockItems) {
            if (portfolio.getHoldings().containsKey(stock.getSymbol())) {
                UserPortfolio.Holding holding = portfolio.getHoldings().get(stock.getSymbol());
                double totalValue = holding.getQuantity() * stock.getPrice();
                double holdingChange = ((stock.getPrice() - holding.getAveragePrice()) / holding.getAveragePrice()) * 100;
                totalPortfolioValue += totalValue; // ✅ Sum total portfolio value

                holdings.add(new HoldingsPortfolio(
                        stock.getSymbol(),
                        holding.getQuantity(),
                        stock.getPrice(),
                        totalValue,
                        holdingChange,
                        0  // ✅ Placeholder for portfolioPercentage
                ));
            }
        }

        // ✅ Calculate percentage for each holding
        for (HoldingsPortfolio holding : holdings) {
            double portfolioPercentage = (totalPortfolioValue > 0) ? (holding.getTotalValue() / totalPortfolioValue) * 100 : 0;
            holding.setPortfolioPercentage(portfolioPercentage);
        }

        return holdings;
    }


    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public double getHoldingChange() { return holdingChange; }
    public void setHoldingChange(double holdingChange) { this.holdingChange = holdingChange; }

    public double getPortfolioPercentage() { return portfolioPercentage; }
    public void setPortfolioPercentage(double portfolioPercentage) { this.portfolioPercentage = portfolioPercentage; }
}
