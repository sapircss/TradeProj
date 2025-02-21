package com.example.tradeproj.Models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserPortfolio {
    private String userId;
    private Map<String, Holding> holdings;
    private double cashBalance;
    private double initialInvestment;
    private List<String> watchlist;
    private double totalPortfolioValue; // ✅ Added total value

    public UserPortfolio() {
        this.holdings = new HashMap<>();
        this.watchlist = new ArrayList<>();
    }

    public UserPortfolio(String userId, double initialInvestment) {
        this.userId = userId;
        this.holdings = new HashMap<>();
        this.cashBalance = initialInvestment;
        this.initialInvestment = initialInvestment;
        this.watchlist = new ArrayList<>();
    }

    // ✅ Calculates total portfolio value based on current stock prices
    public double getTotalPortfolioValue() {
        double total = cashBalance;
        for (Holding holding : holdings.values()) {
            total += holding.getQuantity() * holding.getCurrentPrice(); // ✅ Now using market price
        }
        return total;
    }

    // ✅ Calculates total P&L in dollars
    public double getPnl() {
        double totalPnl = 0;
        for (Holding holding : holdings.values()) {
            double pnl = (holding.getCurrentPrice() - holding.getAveragePrice()) * holding.getQuantity();
            totalPnl += pnl;
        }
        return totalPnl;
    }

    // ✅ Calculates total P&L as a percentage of the initial investment
    public double getPnlPercentage() {
        if (initialInvestment == 0) return 0;
        return (getPnl() / initialInvestment) * 100;
    }

    public void addToWatchlist(String symbol) {
        if (!watchlist.contains(symbol)) {
            watchlist.add(symbol);
        }
    }

    public void removeFromWatchlist(String symbol) {
        watchlist.remove(symbol);
    }

    public void updateCashBalance(double amount) {
        this.cashBalance += amount;
    }

    // ✅ Add or update stock holdings
    public void addHolding(String symbol, int quantity, double averagePrice) {
        if (holdings.containsKey(symbol)) {
            Holding existingHolding = holdings.get(symbol);
            int newQuantity = existingHolding.getQuantity() + quantity;
            double newAveragePrice = ((existingHolding.getQuantity() * existingHolding.getAveragePrice()) +
                    (quantity * averagePrice)) / newQuantity;
            existingHolding.setQuantity(newQuantity);
            existingHolding.setAveragePrice(newAveragePrice);
        } else {
            holdings.put(symbol, new Holding(quantity, averagePrice, averagePrice)); // ✅ Set initial currentPrice
        }
    }

    // ✅ Update stock price (called when fetching market prices)
    public void updateHoldingPrice(String symbol, double currentPrice) {
        if (holdings.containsKey(symbol)) {
            holdings.get(symbol).setCurrentPrice(currentPrice);
        }
    }

    public void removeHolding(String symbol, int quantity) {
        if (holdings.containsKey(symbol)) {
            Holding existingHolding = holdings.get(symbol);
            if (existingHolding.getQuantity() > quantity) {
                existingHolding.setQuantity(existingHolding.getQuantity() - quantity);
            } else {
                holdings.remove(symbol);
            }
        }
    }

    public void resetPortfolio() {
        this.holdings.clear();
        this.cashBalance = 0.0;
        this.initialInvestment = 0.0;
        this.watchlist.clear();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getCashBalance() { return cashBalance; }
    public void setCashBalance(double cashBalance) { this.cashBalance = cashBalance; }

    public double getInitialInvestment() { return initialInvestment; }
    public void setInitialInvestment(double initialInvestment) { this.initialInvestment = initialInvestment; }

    public List<String> getWatchlist() { return watchlist; }
    public void setWatchlist(List<String> watchlist) { this.watchlist = watchlist; }

    public Map<String, Holding> getHoldings() { return holdings; }
    public void setHoldings(Map<String, Holding> holdings) { this.holdings = holdings; }

    public void setTotalPortfolioValue(double totalPortfolioValue) {
        this.totalPortfolioValue = totalPortfolioValue;
    }

    // ✅ Updated Holding class with `currentPrice`
    public static class Holding {
        private int quantity;
        private double averagePrice;
        private double currentPrice; // ✅ NEW: Track market price

        public Holding() {}

        public Holding(int quantity, double averagePrice, double currentPrice) {
            this.quantity = quantity;
            this.averagePrice = averagePrice;
            this.currentPrice = currentPrice;
        }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getAveragePrice() { return averagePrice; }
        public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }

        public double getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

        // ✅ Get P&L for this stock
        public double getPnl() {
            return (currentPrice - averagePrice) * quantity;
        }

        // ✅ Get P&L percentage for this stock
        public double getPnlPercentage() {
            if (averagePrice == 0) return 0;
            return ((currentPrice - averagePrice) / averagePrice) * 100;
        }
    }
}
