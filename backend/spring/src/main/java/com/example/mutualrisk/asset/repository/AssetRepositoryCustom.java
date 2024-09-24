package com.example.mutualrisk.asset.repository;

import com.example.mutualrisk.asset.entity.Asset;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepositoryCustom {
    List<Asset> searchByKeyword(@Param("keyword") String keyword);
}