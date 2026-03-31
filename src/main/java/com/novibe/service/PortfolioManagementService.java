package com.novibe.service;

import com.novibe.dto.CashFlowRequest;
import com.novibe.dto.CreatePortfolioRequest;
import com.novibe.dto.PortfolioInfoDTO;
import com.novibe.dto.TransactionHistoryDTO;
import com.novibe.entity.Portfolio;
import com.novibe.entity.PortfolioTransaction;
import com.novibe.repository.PortfolioRepository;
import com.novibe.repository.PortfolioTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioManagementService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioTransactionRepository portfolioTransactionRepository;
    private final TransactionService transactionService;

    @Transactional
    public PortfolioInfoDTO createPortfolio(CreatePortfolioRequest request) {
        String newId = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        
        // 1. Create the portfolio with 0 balance initially
        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioId(newId);
        portfolio.setName(request.getName() != null && !request.getName().trim().isEmpty() ? request.getName() : "My Portfolio");
        portfolio.setCashBalance(BigDecimal.ZERO);
        
        portfolio = portfolioRepository.save(portfolio);

        // 2. Determine initial deposit
        if (request.getInitialDeposit() == null || request.getInitialDeposit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("An initial deposit greater than zero is required to open a portfolio account. (必须存入一笔大于0的初始资金)");
        }
        BigDecimal initialDeposit = request.getInitialDeposit();

        // 3. Funnel deposit through TransactionService so it gets logged properly
        CashFlowRequest depositReq = new CashFlowRequest();
        depositReq.setType("DEPOSIT");
        depositReq.setAmount(initialDeposit);
        
        transactionService.processCashFlow(newId, depositReq);

        // 4. Return info
        return PortfolioInfoDTO.builder()
                .portfolioId(newId)
                .name(portfolio.getName())
                .cashBalance(initialDeposit) // Already applied
                .build();
    }

    public List<TransactionHistoryDTO> getTransactionHistory(String portfolioId) {
        // Validate portfolio exists implicitly or explicitly. FindBy is fine
        List<PortfolioTransaction> transactions = portfolioTransactionRepository.findByPortfolioPortfolioIdOrderByTransactionDateDesc(portfolioId);
        
        return transactions.stream().map(pt -> TransactionHistoryDTO.builder()
                .transactionId(pt.getTransactionId())
                .transactionType(pt.getTransactionType())
                .tickerSymbol(pt.getCompanyInfo() != null ? pt.getCompanyInfo().getTickerSymbol() : null)
                .quantity(pt.getQuantity())
                .pricePerUnit(pt.getPricePerUnit())
                .transactionDate(pt.getTransactionDate())
                .build()
        ).collect(Collectors.toList());
    }
}
