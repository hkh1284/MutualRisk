package com.example.mutualrisk.portfolio.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PortfolioAsset {
    private Integer assetId;
    private String code;
    private Integer totalPurchaseQuantity;
    private Double totalPurchasePrice;
}