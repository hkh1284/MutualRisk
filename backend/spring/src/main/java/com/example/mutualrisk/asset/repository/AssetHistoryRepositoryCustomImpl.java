package com.example.mutualrisk.asset.repository;

import com.example.mutualrisk.asset.entity.Asset;
import com.example.mutualrisk.asset.entity.AssetHistory;
import com.example.mutualrisk.common.repository.Querydsl4RepositorySupport;

import java.time.LocalDate;
import java.util.Optional;

import static com.example.mutualrisk.asset.entity.QAssetHistory.assetHistory;

public class AssetHistoryRepositoryCustomImpl extends Querydsl4RepositorySupport implements AssetHistoryRepositoryCustom{
    @Override
    public Optional<AssetHistory> findRecentAssetHistory(Asset asset) {
        return Optional.ofNullable(select(assetHistory)
            .from(assetHistory)
            .where(assetHistory.asset.eq(asset))
            .orderBy(assetHistory.date.desc())
            .fetchFirst());
    }
}