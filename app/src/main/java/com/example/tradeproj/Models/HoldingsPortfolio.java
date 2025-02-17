package com.example.tradeproj.Models;

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
