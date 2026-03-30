package com.novibe.service;

import com.novibe.dto.CashFlowRequest;
import com.novibe.dto.TradeRequest;
import com.novibe.entity.CompanyInfo;
import com.novibe.entity.Portfolio;
import com.novibe.entity.PortfolioTransaction;
import com.novibe.entity.Position;
import com.novibe.exception.InsufficientFundsException;
import com.novibe.exception.InsufficientStockException;
import com.novibe.exception.ResourceNotFoundException;
import com.novibe.repository.CompanyInfoRepository;
import com.novibe.repository.PortfolioRepository;
import com.novibe.repository.PortfolioTransactionRepository;
import com.novibe.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioTransactionRepository portfolioTransactionRepository;
    private final CompanyInfoRepository companyInfoRepository;

    @Transactional
    public String processAssetTrade(String portfolioId, TradeRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));

        CompanyInfo companyInfo = companyInfoRepository.findById(request.getTickerSymbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found for ticker: " + request.getTickerSymbol()));

        BigDecimal totalAmount = request.getQuantity().multiply(request.getPricePerUnit());

        // Find existing position if any hhrer
        Position position = positionRepository.findByPortfolioPortfolioId(portfolioId).stream()
                .filter(p -> p.getCompanyInfo().getTickerSymbol().equals(request.getTickerSymbol()))
                .findFirst()
                .orElse(null);

        if ("BUY".equalsIgnoreCase(request.getTransactionType())) {
            if (portfolio.getCashBalance().compareTo(totalAmount) < 0) {
                throw new InsufficientFundsException("Not enough cash to process buy. Required: " + totalAmount
                        + ", Available: " + portfolio.getCashBalance());
            }

            // Deduct cash
            portfolio.setCashBalance(portfolio.getCashBalance().subtract(totalAmount));

            // Update || create Position
            if (position == null) {
                position = new Position();
                position.setPositionId(UUID.randomUUID().toString());
                position.setPortfolio(portfolio);
                position.setCompanyInfo(companyInfo);
                position.setTotalQuantity(request.getQuantity());
                position.setAverageCost(request.getPricePerUnit());
            } else {
                BigDecimal oldTotalValue = position.getTotalQuantity().multiply(position.getAverageCost());
                BigDecimal newTotalQuantity = position.getTotalQuantity().add(request.getQuantity());
                BigDecimal newAverageCost = oldTotalValue.add(totalAmount).divide(newTotalQuantity, 4,
                        RoundingMode.HALF_UP);

                position.setTotalQuantity(newTotalQuantity);
                position.setAverageCost(newAverageCost);
            }
            positionRepository.save(position);

        } else if ("SELL".equalsIgnoreCase(request.getTransactionType())) {
            if (position == null || position.getTotalQuantity().compareTo(request.getQuantity()) < 0) {
                throw new InsufficientStockException("Not enough stock quantity to sell.");
            }

            // Add cash
            portfolio.setCashBalance(portfolio.getCashBalance().add(totalAmount));

            // Deduct stock quantity
            BigDecimal newQuantity = position.getTotalQuantity().subtract(request.getQuantity());
            if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                positionRepository.delete(position);
            } else {
                position.setTotalQuantity(newQuantity);
                positionRepository.save(position);
            }
        } else {
            throw new IllegalArgumentException("Unknown trade type: " + request.getTransactionType());
        }

        portfolioRepository.save(portfolio);

        // Record Transaction
        PortfolioTransaction transaction = new PortfolioTransaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setPortfolio(portfolio);
        transaction.setCompanyInfo(companyInfo);
        transaction.setTransactionType(request.getTransactionType().toUpperCase());
        transaction.setQuantity(request.getQuantity());
        transaction.setPricePerUnit(request.getPricePerUnit());
        portfolioTransactionRepository.save(transaction);
        return transaction.getTransactionId();
    }

    @Transactional
    public String processCashFlow(String portfolioId, CashFlowRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for: " + portfolioId));

        if ("WITHDRAW".equalsIgnoreCase(request.getType())) {
            if (portfolio.getCashBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds for withdrawal. Available: " + portfolio.getCashBalance());
            }
            portfolio.setCashBalance(portfolio.getCashBalance().subtract(request.getAmount()));
        } else if ("DEPOSIT".equalsIgnoreCase(request.getType())) {
            portfolio.setCashBalance(portfolio.getCashBalance().add(request.getAmount()));
        } else {
            throw new IllegalArgumentException("Unknown cash flow type: " + request.getType());
        }

        portfolioRepository.save(portfolio);

        PortfolioTransaction transaction = new PortfolioTransaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setPortfolio(portfolio);
        transaction.setCompanyInfo(null);
        transaction.setTransactionType(request.getType().toUpperCase());
        transaction.setQuantity(request.getAmount());
        transaction.setPricePerUnit(BigDecimal.ONE);
        portfolioTransactionRepository.save(transaction);
        return transaction.getTransactionId();
    }
}
