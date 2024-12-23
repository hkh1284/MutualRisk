package com.example.mutualrisk.asset.repository;



import com.example.mutualrisk.asset.entity.Asset;
import com.example.mutualrisk.asset.entity.AssetNews;
import com.example.mutualrisk.asset.entity.News;
import com.example.mutualrisk.asset.entity.QNews;
import com.example.mutualrisk.common.repository.Querydsl4RepositorySupport;

import static com.example.mutualrisk.asset.entity.QAsset.*;
import static com.example.mutualrisk.asset.entity.QAssetNews.*;
import static com.example.mutualrisk.asset.entity.QNews.*;

import java.util.List;

public class AssetNewsRepositoryCustomImpl extends Querydsl4RepositorySupport implements AssetNewsRepositoryCustom{

    @Override
    public List<AssetNews> findByAssetIn(List<Asset> userInterestAssetList) {
        return selectFrom(assetNews)
            .join(assetNews.news, news).fetchJoin()
            .where(assetNews.asset.in(userInterestAssetList))
            .orderBy(assetNews.news.publishedAt.desc())
            .limit(25)
            .fetch();
    }

    @Override
    public List<AssetNews> findAllByNews(News news,List<Asset> userInterestAssetList) {
        return selectFrom(assetNews)
            .join(assetNews.asset, asset).fetchJoin()
            .join(assetNews.news, QNews.news).fetchJoin()
            .where(assetNews.news.eq(news).and(assetNews.asset.in(userInterestAssetList)))
            .fetch();
    }

}
