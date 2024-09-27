package com.example.mutualrisk.portfolio.dto;

import com.example.mutualrisk.asset.entity.Asset;
import com.example.mutualrisk.common.enums.Region;
import com.example.mutualrisk.portfolio.entity.PortfolioAsset;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public record PortfolioResponse() {

    @Builder
    @Schema(name = "포트폴리오 검색 결과 데이터", description = "유저의 포트폴리오 조회 시 반환되는 데이터")
    public record PortfolioResultDto(
        Boolean hasPortfolio,
        PortfolioInfo portfolio
    ) {

    }

    @Builder
    @Schema(name = "포트폴리오 상세 데이터", description = "실제 포트폴리오 정보를 담은 데이터")
    public record PortfolioInfo(
        String portfolioId,
        PortfolioPerformance performance,
        List<PortfolioAssetInfo> assets
    ) {

    }

    @Builder
    @Schema(name = "포트폴리오의 성능을 나타내는 지표")
    public record PortfolioPerformance(
        Double expectedReturn,
        Double standardDeviation,
        Double valuation
    ) {

    }

    @Builder
    @Schema(name = "포트폴리오에 담긴 개별 종목 데이터", description = "유저 포트폴리오에 담긴 종목 데이터")
    public record PortfolioAssetInfo(
        Integer assetId,
        String code,
        String name,
        Region region,
        Double weight,
        LocalDateTime purchaseTime,
        Integer purchaseNum,
        Double purchaseAmount
    ) {
        public static PortfolioAssetInfo of(PortfolioAsset portfolioAsset, Asset asset, Double weight) {
            return PortfolioAssetInfo.builder()
                .assetId(asset.getId())
                .code(asset.getCode())
                .name(asset.getName())
                .region(asset.getRegion())
                .weight(weight)
                .purchaseTime(portfolioAsset.getPurchaseDate().atStartOfDay())
                .purchaseNum(portfolioAsset.getPurchaseQuantity())
                .purchaseAmount(portfolioAsset.getPurchaseQuantity() * asset.getRecentPrice())
                .build();
        }
    }

}
