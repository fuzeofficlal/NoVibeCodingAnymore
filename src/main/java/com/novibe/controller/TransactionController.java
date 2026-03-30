package com.novibe.controller;

import com.novibe.dto.CashFlowRequest;
import com.novibe.dto.TradeRequest;
import com.novibe.dto.TransactionResponseDTO;
import com.novibe.entity.Portfolio;
import com.novibe.repository.PortfolioRepository;
import com.novibe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final PortfolioRepository portfolioRepository; // Need this just to fetch new cash balance for the response

    @PostMapping("/{portfolioId}/transactions")
    public ResponseEntity<TransactionResponseDTO> processTransaction(
            @PathVariable String portfolioId,
            @RequestBody TradeRequest request) {

        String transactionId;
        
        // Route to the correct service logic based on transaction type
        if ("DEPOSIT".equalsIgnoreCase(request.getTransactionType()) || "WITHDRAW".equalsIgnoreCase(request.getTransactionType())) {
            CashFlowRequest cfReq = new CashFlowRequest();
            cfReq.setType(request.getTransactionType());
            cfReq.setAmount(request.getQuantity() != null ? request.getQuantity() : request.getPricePerUnit()); 
            // the spec: "如果是纯现金存取，这里可传 1.0 或 null", quantity holds the amount usually, but we fallback. Actually, if they pass quantity: 10, price: 150, the UI might be passing amount in `quantity` or `amount`. 
            // In the spec: `"quantity": 10.0, "pricePerUnit": 150.00 //如果是纯现金存取，这里可传 1.0 或 null`
            // Usually amount = quantity.
            
            transactionId = transactionService.processCashFlow(portfolioId, cfReq);
        } else {
            // It's a BUY or SELL
            transactionId = transactionService.processAssetTrade(portfolioId, request);
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new RuntimeException("Portfolio not found after transaction"));

        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .message("Transaction successful")
                .transactionId(transactionId)
                .newCashBalance(portfolio.getCashBalance())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
