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

    private double totalPortfolioValue; // ✅ Add missing field

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

    public double getTotalPortfolioValue() {
        double total = cashBalance;
        for (Holding holding : holdings.values()) {
            total += holding.getQuantity() * holding.getAveragePrice();
        }
        return total;
    }

    public void addToWatchlist(String symbol) {
        if (!watchlist.contains(symbol)) {
            watchlist.add(symbol);
        }
    }


    public void setTotalPortfolioValue(double totalPortfolioValue) {
        this.totalPortfolioValue = totalPortfolioValue;
    }

    public void removeFromWatchlist(String symbol) {
        watchlist.remove(symbol);
    }

    public void updateCashBalance(double amount) {
        this.cashBalance += amount;
    }

    public void addHolding(String symbol, int quantity, double averagePrice) {
        if (holdings.containsKey(symbol)) {
            Holding existingHolding = holdings.get(symbol);
            int newQuantity = existingHolding.getQuantity() + quantity;
            double newAveragePrice = ((existingHolding.getQuantity() * existingHolding.getAveragePrice()) +
                    (quantity * averagePrice)) / newQuantity;
            existingHolding.setQuantity(newQuantity);
            existingHolding.setAveragePrice(newAveragePrice);
        } else {
            holdings.put(symbol, new Holding(quantity, averagePrice));
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

    public static class Holding {
        private int quantity;
        private double averagePrice;

        public Holding() {}

        public Holding(int quantity, double averagePrice) {
            this.quantity = quantity;
            this.averagePrice = averagePrice;
        }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getAveragePrice() { return averagePrice; }
        public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }
    }
}
