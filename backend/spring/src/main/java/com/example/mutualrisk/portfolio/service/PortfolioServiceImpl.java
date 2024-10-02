package com.example.mutualrisk.portfolio.service;

import com.example.mutualrisk.asset.dto.AssetResponse.*;
import com.example.mutualrisk.asset.entity.Asset;
import com.example.mutualrisk.asset.entity.AssetHistory;
import com.example.mutualrisk.asset.repository.AssetHistoryRepository;
import com.example.mutualrisk.asset.repository.AssetRepository;
import com.example.mutualrisk.asset.service.AssetHistoryService;
import com.example.mutualrisk.asset.service.AssetService;
import com.example.mutualrisk.common.dto.CommonResponse.*;
import com.example.mutualrisk.common.enums.PerformanceMeasure;
import com.example.mutualrisk.common.email.dto.EmailMessage;
import com.example.mutualrisk.common.email.service.EmailService;
import com.example.mutualrisk.common.enums.Region;
import com.example.mutualrisk.common.enums.TimeInterval;
import com.example.mutualrisk.common.exception.ErrorCode;
import com.example.mutualrisk.common.exception.MutualRiskException;
import com.example.mutualrisk.common.fastapi.FastApiService;
import com.example.mutualrisk.common.repository.ExchangeRatesRepository;
import com.example.mutualrisk.common.util.DateUtil;
import com.example.mutualrisk.fund.dto.FundResponse.*;
import com.example.mutualrisk.fund.dto.FundResponse.SectorInfo;
import com.example.mutualrisk.portfolio.dto.PortfolioRequest;
import com.example.mutualrisk.portfolio.dto.PortfolioRequest.PortfolioInitDto;
import com.example.mutualrisk.portfolio.dto.PortfolioResponse.*;
import com.example.mutualrisk.portfolio.entity.*;
import com.example.mutualrisk.portfolio.repository.PortfolioRepository;
import com.example.mutualrisk.sector.entity.Sector;
import com.example.mutualrisk.user.entity.User;
import com.example.mutualrisk.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService{
    private final AssetService assetService;
    private final AssetHistoryService assetHistoryService;

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final ExchangeRatesRepository exchangeRatesRepository;
    private final UserRepository userRepository;

    private final FastApiService fastApiService;


    private final DateUtil dateUtil;

    // 메일발송을 위한 서비스
    private final EmailService emailService;


    @Override
    public ResponseWithData<PortfolioResultDto> getPortfolioInfo(Integer userId, String portfolioId) {

        // 1. userId를 이용해서, mongoDB에서 데이터를 검색해 가져온다
        Portfolio portfolio = getMyPortfolioById(userId, portfolioId);

        // 2-1. userId에 해당하는 포트폴리오가 없을 경우
        if (portfolio == null) {
            return buildPortfolioResponse();
        }
        // 2-2. userId에 해당하는 포트폴리오가 존재할 경우
        List<PortfolioAsset> portfolioAssetList = portfolio.getAsset();
        List<Asset> assetList = getAssetsFromPortfolio(portfolioAssetList);
        // 자산들의 구매 금액을 저장하는 리스트
        List<Double> assetValuationList = getAssetValuationList(portfolioAssetList, assetList);
        // 포트폴리오에 속해 있는 자산 리스트

        double totalValuation = assetValuationList.stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        // 3. 각 값의 비율을 계산해서 weights 리스트에 추가
        List<Double> weights = calculateWeights(assetValuationList, totalValuation);

        List<PortfolioAssetInfo> portfolioAssetInfoList = getPortfolioAssetInfos(userId, portfolioId);

        PortfolioPerformance portfolioPerformance = getPortfolioPerformance(assetList, weights, totalValuation);

        return buildPortfolioResponse(portfolio, portfolioAssetInfoList, portfolioPerformance);
    }

    private Portfolio getMyPortfolioById(Integer userId, String portfolioId) {
        Portfolio portfolio = portfolioRepository.getPortfolioById(portfolioId);
        if (portfolio == null || !portfolio.getUserId().equals(userId)) throw new MutualRiskException(ErrorCode.PARAMETER_INVALID);
        return portfolio;
    }

    /**
     * 전체 유저를 대상으로 포트폴리오의 비중을 재계산하여, +-10%p 이상의 변동이 있거나
     * 유저가 설정한 상한,하한을 초과한 종목이 있을경우 유저에게 메일을 보낸다
     * @return
     */
    @Override
    public ResponseWithMessage sendRefreshMail() {

        // 전체 유저 목록을 가지고온다
        List<User> users = userRepository.findAll();

        // 각 유저의 포트폴리오를 가지고온다
        for(User user: users){
            Portfolio curPortfolio = getCurrentPortfolio(user);
            // 유저의 포트폴리오가 없는경우 패스
            if(ObjectUtils.isEmpty(curPortfolio))continue;

            List<PortfolioAsset> assets = curPortfolio.getAsset();

            // 오늘일자 기준 포트폴리오 자산의 (자산코드,총가격)
            Map<String, Double> recentAssetPrice = getTodayValueOfHoldings(assets);

            // for(Entry<String,Double> entry: recentAssetPrice.entrySet()){
            //     log.warn("CODE1 : {}", entry.getKey());
            //     log.warn("PRICE: {}", entry.getValue());
            // }

            // 오늘일자 기준 총 자산 가치 계산
            Double totalRecentValueOfHolding = recentAssetPrice.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

            // log.warn("TOTAL RECENT VALUE : {}", totalRecentValueOfHolding);

            // 오늘일자 기준 (종목, 비중) 계산
            Map<String, Double> recentAssetWeights = getWeights(recentAssetPrice, totalRecentValueOfHolding);

            // for(Entry<String,Double> recentAssetWeightEntry: recentAssetWeights.entrySet()){
            //
            //     log.warn("CODE2 : {}", recentAssetWeightEntry.getKey());
            //     log.warn("WEIGHT : {}", recentAssetWeightEntry.getValue());
            //
            // }
            /**
             * 구매 당시 비중과 오늘날의 비중을 비교하여
             * 1. 포트폴리오의 lower bound 를 넘은 종목, upper bound를 넘은 종목을 찾는다
             * 2. +-10%p의 차이가 존재하는 자산을 찾는다
             */

            // 종목 코드와 비중을 비교하기 위해 자산 리스트와 포트폴리오 정보를 매핑
            List<Integer> assetIds = assets.stream()
                .map(PortfolioAsset::getAssetId)
                .toList();

            List<String> assetCodes = assetRepository.findAllById(assetIds)
                .stream()
                .map(Asset::getCode)
                .toList();

            // lowerBound, upperBound, weights와 최근 비중을 비교

            List<String> lowerBoundExceededAssets = new ArrayList<>();
            List<String> upperBoundExceededAssets = new ArrayList<>();
            List<String> increasedWeightAssets = new ArrayList<>();
            List<String> decreasedWeightAssets = new ArrayList<>();

            for (int i = 0; i < assetCodes.size(); i++) {
                String code = assetCodes.get(i);
                Double recentWeight = recentAssetWeights.get(code); // 최근 비중
                Double lowerBound = curPortfolio.getLowerBound().get(i); // 포트폴리오의 하한선
                Double upperBound = curPortfolio.getUpperBound().get(i); // 포트폴리오의 상한선
                Double originWeight = curPortfolio.getWeights().get(i); // 포트폴리오의 기존 비중

                // 1. lower bound와 upper bound를 넘는 종목 찾기
                if (recentWeight < lowerBound) {
                    // log.warn(code+"의 비중이 lower bound를 넘었습니다.");
                    lowerBoundExceededAssets.add(code);
                } else if (recentWeight > upperBound) {
                    // log.warn(code+"의 비중이 upper bound를 넘었습니다.");
                    upperBoundExceededAssets.add(code);
                }

                if((recentWeight - originWeight) > 10.0){
                    // code 종목의 비중이 10% 상승한것
                    // log.info(code + "의 비중이 10% 상승하였습니다.");
                    increasedWeightAssets.add(code);
                }
                else if((originWeight - recentWeight) > 10.0){
                    // code 종목의 비중이 -10% 감소한것
                    // log.info(code + "의 비중이 10% 감소하였습니다.");
                    decreasedWeightAssets.add(code);
                }
            }
            // 알람 메일을 보내야할 종목에 대해 메일을 발송한다
            if (!lowerBoundExceededAssets.isEmpty() || !upperBoundExceededAssets.isEmpty() ||
                !increasedWeightAssets.isEmpty() || !decreasedWeightAssets.isEmpty()) {

                // 메일 발송 로직
                StringBuilder mailContent = new StringBuilder("포트폴리오 자산 비중 경고:\n");

                if (!lowerBoundExceededAssets.isEmpty()) {
                    mailContent.append("하한선을 넘은 자산: ").append(lowerBoundExceededAssets).append("\n");
                }
                if (!upperBoundExceededAssets.isEmpty()) {
                    mailContent.append("상한선을 넘은 자산: ").append(upperBoundExceededAssets).append("\n");
                }
                if (!increasedWeightAssets.isEmpty()) {
                    mailContent.append("비중이 10% 상승한 자산: ").append(increasedWeightAssets).append("\n");
                }
                if (!decreasedWeightAssets.isEmpty()) {
                    mailContent.append("비중이 10% 감소한 자산: ").append(decreasedWeightAssets).append("\n");
                }

                EmailMessage emailMessage = EmailMessage.builder()
                    .to(user.getEmail())
                    .subject("포트폴리오 자산 비중 경고")
                    .message(mailContent.toString())
                    .build();

                log.warn("emailMessage: {}", emailMessage);
                // 메일 발송 함수 호출
                emailService.sendMail(emailMessage);
            }

        }
        return new ResponseWithMessage(HttpStatus.OK.value(),"메일발송에 성공하였습니다");
    }

    private Portfolio getCurrentPortfolio(User user) {
        List<Portfolio> myPortfolioList = portfolioRepository.getMyPortfolioList(user.getId());
        if (myPortfolioList.isEmpty()) return null;

        return myPortfolioList.get(0);
    }

    @Override
    public ResponseWithData<PortfolioValuationDto> getUserPortfolioPerformance(TimeInterval timeInterval, PerformanceMeasure measure, Integer userId, String portfolioId) {
        // 1. userId를 이용해서, mongoDB에서 데이터를 검색해 가져온다
        Portfolio portfolio = getMyPortfolioById(userId, portfolioId);

        // 2. AssetList 구하기
        List<PortfolioAsset> portfolioAssetList = portfolio.getAsset();

        List<Integer> assetIdList = portfolioAssetList.stream()
            .map(PortfolioAsset::getAssetId)
            .toList();

        List<Asset> assetList = assetRepository.findAllById(assetIdList);

        // 3. 포트폴리오 백테스팅 결과 저장
        LocalDateTime recentDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);;

        List<Performance> performances = new ArrayList<>();
        for (int dDate = 30; dDate >= 1; dDate--) {
            LocalDateTime targetDate = dateUtil.getPastDate(recentDate, timeInterval, dDate);
            Double valuation = getValuation(portfolioAssetList, assetList, targetDate);
            performances.add(Performance.builder()
                    .time(targetDate)
                    .valuation(valuation)
                .build());
        }

        PortfolioValuationDto data = PortfolioValuationDto.builder()
            .portfolioId(portfolio.getId())
            .timeInterval(timeInterval)
            .measure(measure)
            .performances(performances)
            .build();

        return new ResponseWithData<>(HttpStatus.OK.value(), "백테스팅 결과 조회 성공", data);
    }

    /**
     * 유저의 포트폴리오가 가진 섹터 비중을 반환하는 메서드
     *
     * @param userId
     * @param portfolioId
     * @return
     */
    @Override
    public ResponseWithData<List<SectorInfo>> getUserPortfolioSector(Integer userId, String portfolioId) {

        // 유저를 가져온다
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new MutualRiskException(ErrorCode.USER_NOT_FOUND));

        // 유저가 가진 포트폴리오를 가져온다
        Portfolio myPortfolio = getMyPortfolioById(userId, portfolioId);

        if(ObjectUtils.isEmpty(myPortfolio)){
            // 유저가 가진 포트폴리오가 없는 경우, 에러
            throw new MutualRiskException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        // 유저가 가진 포트폴리오 자산의 ID를 구한다
        List<Integer> assetIds = myPortfolio.getAsset().stream()
            .map(PortfolioAsset::getAssetId).toList();

        // 유저가 가진 포트폴리오 자산을 가지고온다
        List<Asset> assets = assetRepository.findAllById(assetIds);

        // (섹터,오늘날의 비중) 을 구해야한다
        // 오늘 비중 : 구매한 자산의 quantity * 최근 가격
        Map<Sector, Double> sectorToTotalValue = new HashMap<>();
        Double totalValueOfHolding = 0.0;

        // 환율을 가져오는 메서드
        Double recentExchangeRate = exchangeRatesRepository.getRecentExchangeRate();

        for (Asset asset : assets) {
            Sector sector = asset.getIndustry().getSector(); // 섹터를 가져옴
            log.warn("SECTOR : {}", sector.getName());
            double totalValue = 0.0;

            // 포트폴리오에서 해당 자산을 찾고, 수량 * 최근 가격을 계산
            for (PortfolioAsset pAsset : myPortfolio.getAsset()) {
                if (pAsset.getAssetId().equals(asset.getId())) {
                    if(asset.getRegion().equals("US")){
                        totalValue += pAsset.getTotalPurchaseQuantity() * asset.getRecentPrice() * recentExchangeRate; // 수량 * 최근 가격 * 환율
                    }
                    else{
                        totalValue += pAsset.getTotalPurchaseQuantity() * asset.getRecentPrice();
                    }
                }
            }

            // 이미 섹터가 존재하면 값을 더하고, 없으면 새로운 값을 추가
            sectorToTotalValue.put(sector, sectorToTotalValue.getOrDefault(sector, 0.0) + totalValue);
            // log.warn("totalValue : {}",totalValue);

            totalValueOfHolding+=totalValue;
        }

        // (섹터,누적합) 구했으니, 다시 순회하면서 누적합을 totalValueOfHolding으로 나눈 맵을 구한다
        List<SectorInfo> sectorInfos = new ArrayList<>();
        for(Entry<Sector,Double> entry:sectorToTotalValue.entrySet()){
            Sector sector = entry.getKey();
            Double weight = 0.0;
            try{
                weight = entry.getValue() * 100.0/totalValueOfHolding;
            }
            catch(ArithmeticException e){
                weight = 0.0;
            }
            SectorInfo sectorInfo = SectorInfo.of(sector,weight);
            sectorInfos.add(sectorInfo);
        }

        // 결과를 반환한다
        return new ResponseWithData<>(HttpStatus.OK.value(),"섹터 조회에 성공하였습니다",sectorInfos);
    }

    @Override
    public ResponseWithData<FrontierDto> getFrontierPoints(Integer userId, String portfolioId) {
        // 1. 유저가 가진 포트폴리오를 가져온다
        Portfolio myPortfolio = getMyPortfolioById(userId, portfolioId);

        List<FrontierPoint> frontierPoints = myPortfolio.getFrontierPoints();
        FictionalPerformance fictionalPerformance = myPortfolio.getFictionalPerformance();

        FrontierDto frontierDto = FrontierDto.builder()
            .frontierPoints(frontierPoints)
            .optimalPerformance(fictionalPerformance)
            .build();

        return new ResponseWithData<>(HttpStatus.OK.value(), "효율적 포트폴리오 곡선 데이터 정상 반환", frontierDto);
    }

    /**
     * 유저의 포트폴리오 자산 평가액 변동을 조회해서 반환하는 메서드
     * @param timeInterval : 한 틱당 시간 간격
     * @param measure : 포트폴리오 performance를 측정하는 measure (default: valuation)
     * @param userId : 유저 id
     * @param portfolioId : 포트폴리오 id
     * @return
     */
    @Override
    public ResponseWithData<PortfolioValuationDto> getHistoricalValuation(TimeInterval timeInterval, PerformanceMeasure measure, Integer userId, String portfolioId) {
        // 1. userId를 이용해서, mongoDB에서 데이터를 검색해 가져온다
        Portfolio portfolio = getMyPortfolioById(userId, portfolioId);

        // 2. 유저의 포트폴리오 리스트 받아오기
        List<Portfolio> myPortfolioList = portfolioRepository.getMyPortfolioList(userId);

        // 3. 현재 조회 중인 포트폴리오를, myPortfolioList에서 찾는다
        int idx = 0;
        while (!myPortfolioList.get(idx).getId().equals(portfolioId)) {
            idx++;
        }

        // 4. 계산을 시작할 날짜 구하기
        // 4-1. 현재 조회 중인 포트폴리오가, 가장 최신일 경우 : endDate를 현재 시간으로 설정
        // 4-2. 현재 조회 중인 포트폴리오가, 과거 포트폴리오일 경우 : endDate를 그 포트폴리오가 교체된 시간으로 설정
        LocalDateTime endDate = myPortfolioList.get(idx).getIsActive().equals(Boolean.TRUE)?
            LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0):
            myPortfolioList.get(idx).getDeletedAt();

        // 5. 각 날짜에 대해, 포트폴리오의 valuation을 구하기
        List<Performance> performances = new ArrayList<>();
        Portfolio curPortfolio;
        for (int dDate = 1; dDate <= 30; dDate++) {

            // 구하기를 원하는 날짜
            LocalDateTime targetDate = dateUtil.getPastDate(endDate, timeInterval, dDate);

            // targetDate 기준으로, 유저의 포트폴리오가 어떤 거였는지를 찾는다
            while (idx < myPortfolioList.size() && myPortfolioList.get(idx).getCreatedAt().isAfter(targetDate)) {
                idx++;
            }

            // targetDate에 유저의 포트폴리오가 존재하지 않았을 경우 : break
            if (idx == myPortfolioList.size()) break;

            curPortfolio = myPortfolioList.get(idx);

            List<PortfolioAsset> portfolioAssetList = curPortfolio.getAsset();

            List<Integer> assetIdList = portfolioAssetList.stream()
                .map(PortfolioAsset::getAssetId)
                .toList();

            List<Asset> assetList = assetRepository.findAllById(assetIdList);

            Double valuation = getHistoricValuation(portfolioAssetList, assetList, targetDate);
            performances.add(Performance.builder()
                .time(targetDate)
                .valuation(valuation)
                .build());
        }

        Collections.reverse(performances);

        PortfolioValuationDto data = PortfolioValuationDto.builder()
            .portfolioId(portfolio.getId())
            .timeInterval(timeInterval)
            .measure(measure)
            .performances(performances)
            .build();

        return new ResponseWithData<>(HttpStatus.OK.value(), "자산 평가액 조회 성공", data);

    }

    @Override
    public ResponseWithData<List<PortfolioReturnDto>> getHistoricalReturns(TimeInterval timeInterval, PerformanceMeasure measure, Integer userId, String portfolioId) {
        // 1. userId를 이용해서, mongoDB에서 데이터를 검색해 가져온다
        Portfolio portfolio = getMyPortfolioById(userId, portfolioId);

        // 2. 유저의 포트폴리오 리스트 받아오기
        List<Portfolio> myPortfolioList = portfolioRepository.getMyPortfolioList(userId);

        // 3. 현재 조회 중인 포트폴리오를, myPortfolioList에서 찾는다
        int idx = 0;
        while (!myPortfolioList.get(idx).getId().equals(portfolioId)) {
            idx++;
        }

        // 4. 계산을 시작할 날짜 구하기
        // 4-1. 현재 조회 중인 포트폴리오가, 가장 최신일 경우 : endDate를 현재 시간으로 설정
        // 4-2. 현재 조회 중인 포트폴리오가, 과거 포트폴리오일 경우 : endDate를 그 포트폴리오가 교체된 시간으로 설정
        LocalDateTime endDate = myPortfolioList.get(idx).getIsActive().equals(Boolean.TRUE)?
            LocalDateTime.now().withDayOfMonth(1):
            myPortfolioList.get(idx).getDeletedAt().withDayOfMonth(1);

        List<PortfolioReturnDto> portfolioReturnList = new ArrayList<>();
        // 30달 전, 29달 전, ..., 1달 전의 데이터를 기준으로 수익률을 구한다
        Portfolio curPortfolio;
        for (int dDate = 1; dDate <= 30; dDate++) {
            // 구하기를 원하는 날짜
            LocalDateTime targetDate = dateUtil.getPastDate(endDate, timeInterval, dDate);

            // targetDate 기준으로, 유저의 포트폴리오가 어떤 거였는지를 찾는다
            while (idx < myPortfolioList.size() && myPortfolioList.get(idx).getCreatedAt().isAfter(targetDate)) {
                idx++;
            }

            // targetDate에 유저의 포트폴리오가 존재하지 않았을 경우 : break
            if (idx == myPortfolioList.size()) break;

            curPortfolio = myPortfolioList.get(idx);

            // 2. AssetList 구하기
            List<PortfolioAsset> portfolioAssetList = curPortfolio.getAsset();

            List<Integer> assetIdList = portfolioAssetList.stream()
                .map(PortfolioAsset::getAssetId)
                .toList();

            List<Asset> assetList = assetRepository.findAllById(assetIdList);

            Double returns = getPortfolioReturn(portfolioAssetList, assetList, targetDate, timeInterval);   // 데이터 기준일을 기준으로, return 구하기
            portfolioReturnList.add(PortfolioReturnDto.builder()
                .date(targetDate)
                .portfolioReturns(returns)
                .build());
        }

        Collections.reverse(portfolioReturnList);
        return new ResponseWithData<>(HttpStatus.OK.value(), "포트폴리오 월별 수익률 조회 성공", portfolioReturnList);
    }

    @Override
    public ResponseWithData<List<SimplePortfolioDto>> getAllUserPortfolio(Integer userId) {
        List<Portfolio> myPortfolioList = portfolioRepository.getMyPortfolioList(userId);
        List<SimplePortfolioDto> data = myPortfolioList.stream()
            .map(portfolio -> SimplePortfolioDto.builder()
                .id(portfolio.getId())
                .version(portfolio.getVersion())
                .build())
            .toList();


        return new ResponseWithData<>(HttpStatus.OK.value(), "유저 전체 포트폴리오 조회 성공", data);
    }

    @Override
    public ResponseWithData<List<PortfolioAssetInfo>> getAssetInfoList(Integer userId, String portfolioId) {
        List<PortfolioAssetInfo> portfolioAssetInfoList = getPortfolioAssetInfos(userId, portfolioId);

        return new ResponseWithData<>(HttpStatus.OK.value(), "포트폴리오 종목 정보 정상 반환", portfolioAssetInfoList);

    }

    private List<PortfolioAssetInfo> getPortfolioAssetInfos(Integer userId, String portfolioId) {
        // 1. userId를 이용해서, mongoDB에서 데이터를 검색해 가져온다
        Portfolio portfolio = getMyPortfolioById(userId, portfolioId);

        // 2. 현재 유저 포트폴리오에 있는 자산 목록을 가져 온다
        List<PortfolioAsset> portfolioAssetList = portfolio.getAsset();
        List<Asset> assetList = getAssetsFromPortfolio(portfolioAssetList);

        // 3. 자산들 중, 첫번째 자산의 최근종가일 2개를 가지고온다
        // Todo : KR,US의 리스트를 따로 구분하여, 각각의 최근종가일을 구분하여 가져오기

        List<LocalDateTime> twoValidDate = assetHistoryService.getValidDate(assetList.get(0),
            LocalDateTime.now(), 2);

        // 4. 환율을 가져온다
        Double recentExchangeRate = exchangeRatesRepository.getRecentExchangeRate();

        // 5. 관심자산의 최근 종가를 가지고 온다
        List<AssetHistory> recentHistory = assetHistoryRepository.findRecentHistoryOfAssetsBetweenDates(
            assetList, twoValidDate.get(1), twoValidDate.get(0));

        // 6. AssetHistory를 Asset별로 그룹핑한다
        Map<Asset, List<AssetHistory>> assetHistoryMap = recentHistory.stream()
            .collect(Collectors.groupingBy(AssetHistory::getAsset));

        // log.warn("그룹핑 완료 : {}",assetHistoryMap);

        // 7. Asset을 각각의 AssetHistory와 매핑하여 AssetInfo 생성
        List<AssetInfo> assetInfoList = assetList.stream()
            .map(asset -> {
                // Asset에 대응하는 최근 2개의 AssetHistory를 가져온다
                return assetService.getAssetInfo(assetHistoryMap, recentExchangeRate, asset);
            })
            .toList();

        // 8. valuationList 생성하기
        List<Double> valuationList = getAssetValuationList(portfolioAssetList, assetList);
        double totalValuation = valuationList.stream().mapToDouble(Double::doubleValue).sum();

        // 각 자산의 평가액을 총합으로 나눠서 weights 리스트를 생성
        List<Double> weights = valuationList.stream()
            .map(valuation -> valuation / totalValuation)
            .toList();

        List<PortfolioAssetInfo> portfolioAssetInfoList = IntStream.range(0, assetInfoList.size())
            .mapToObj(i -> PortfolioAssetInfo.of(assetInfoList.get(i), weights.get(i), valuationList.get(i)))
            .toList();
        return portfolioAssetInfoList;
    }

    /**
     * 유저의 포트폴리오 제작 요청을 받아서 포트폴리오의 퍼포먼스,비중 및 추천 자산을 반환하는 메서드
     * @param initInfo
     * @return
     */
    @Override
    @Transactional
    public ResponseWithData<PortfolioAnalysis> initPortfolio(PortfolioInitDto initInfo) {

        // 유저가 설정한 자산 목록과 제약조건을 fastapi 서버로 보내기 위한 요청을 만들어야한다


        // 유저가 입력한 자산의 expected_return을 가지고 와야한다
        // 입력받은 순서 그대로 리스트안에 넣기 위해 정렬해야함
        List<Asset> findAssets = assetRepository.findAllById(initInfo.assetIds())
            .stream()
            .sorted(Comparator.comparing(asset -> initInfo.assetIds().indexOf(asset.getId())))
            .collect(Collectors.toList());

        // 각 자산의 예상 수익
        List<Double> expectedReturns = findAssets.stream()
            .map(Asset::getExpectedReturn)
            .toList();

        // 가져올 자산의 기간을 설정한다
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusYears(2);

        // 기간 내의 가격 기록을 가지고온다
        List<AssetHistory> historyOfAssets = assetHistoryRepository.findRecentHistoryOfAssetsBetweenDates(
            findAssets, startDate, endDate);

        // historyOfAssets를 순회하면서, (자산,[가격리스트])의 맵을 만든다
        Map<Asset,List<Double>> assetPriceList = new HashMap<>();
        for(AssetHistory assetHistory : historyOfAssets) {
            Asset asset = assetHistory.getAsset();

            if(!assetPriceList.containsKey(asset)) {
                assetPriceList.put(asset, new ArrayList<>());
            }
            else{ // 각 자산별로 가격의 리스트를 생성한다
                assetPriceList.get(asset).add(assetHistory.getPrice());
            }
        }

        // 각 자산별 리스트를 돌면서, 가장 길이가 짧은 가격 리스트를 반환한다
        int minLen = Integer.MAX_VALUE;
        for(Entry<Asset,List<Double>> entry : assetPriceList.entrySet()) {
            List<Double> priceList = entry.getValue();

            minLen = Math.min(minLen,priceList.size());
        }

        // 각 자산별로 minLen길이만큼 잘라서 리스트에 넣는다
        List<List<Double>> pricesDataFrame = new ArrayList<>();
        for(Entry<Asset,List<Double>> entry : assetPriceList.entrySet()) {
            List<Double> priceList = entry.getValue();

            pricesDataFrame.add(priceList.subList(0,minLen));
        }

        // 이제 (expectedReturns,priceDataFrame,lowerBounds,upperBounds) 의 JSON을
        // https://j11a607.p.ssafy.io/fastapi/v1/portfolio 로 보내서 결과를 받아온다

        // JSON 요청 본문을 만든다
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("expected_returns", expectedReturns);
        requestBody.put("prices_dataFrame", pricesDataFrame);
        requestBody.put("lower_bounds", initInfo.lowerBounds());
        requestBody.put("upper_bounds", initInfo.upperBounds());

        // FastAPI 서버로 요청을 보낸다
        try {
            // FastApiService를 사용하여 요청을 보낸다
            Map<String, Object> responseBody = fastApiService.sendPortfolioData(requestBody);

            // 퍼포먼스를 가지고 온다
            Map<String, Double> fictionalPerformance = (Map<String, Double>)responseBody.get("fictionalPerformance");

            Double expectedReturn = fictionalPerformance.get("expectedReturn");
            Double volatility = fictionalPerformance.get("volatility");

            // 퍼포먼스를 저장한다
            PortfolioPerformance performance = PortfolioPerformance.builder()
                .expectedReturn(expectedReturn)
                .volatility(volatility)
                .build();

            // 각 자산의 가중치를 가지고온다
            Map<String, Double> weights = (Map<String, Double>)responseBody.get("weights");
            int totalCash = initInfo.totalCash();
            List<RecommendAssetInfo> recommendAssetInfos = new ArrayList<>();

            // 환율을 가지고온다
            Double exchangeRate = exchangeRatesRepository.getRecentExchangeRate();

            for(Entry<String, Double> entry : weights.entrySet()) {

                // 자산 비중을 가지고 온다
                Asset asset = findAssets.get(Integer.parseInt(entry.getKey()));
                Double weight = entry.getValue();

                // 구매량을 결정해야한다
                // 구매량은, (전체 현금 보유량 * 자산 비중 / 해당 자산의 가격) 을 반올림 한 값으로 한다
                int purchaseNum = 0;
                if(asset.getRegion() ==  Region.KR){
                    purchaseNum = (int)Math.round(totalCash * weight / asset.getRecentPrice());
                }
                else{
                    purchaseNum = (int)Math.round(totalCash * weight / (asset.getRecentPrice() * exchangeRate));
                }

                RecommendAssetInfo recommendAssetInfo = RecommendAssetInfo.of(asset,weight,purchaseNum);
                recommendAssetInfos.add(recommendAssetInfo);
            }

            // 기존 포트폴리오로 측정한 퍼포먼스와 추천자산 비중을 반환한다
            PortfolioAnalysis original = PortfolioAnalysis.of(performance,recommendAssetInfos);


            // 여기에는 추천종목을 고려한 DTO를 반환
            CalculatedPortfolio calculatedPortfolio = CalculatedPortfolio.builder()
                .original(original)
                //나중에 추천종목 생기면 여기에 추가해서 리턴하기
                .build();

            return new ResponseWithData(HttpStatus.OK.value(),"포트폴리오 제작 미리보기 입니다",calculatedPortfolio);
        } catch (RuntimeException e) {
            // 예외 처리
            log.error("FastAPI 서버와의 통신 중 오류가 발생했습니다.", e);
            throw new MutualRiskException(ErrorCode.SOME_ERROR_RESPONSE);
        }
    }

    /**
     *
     * @param portfolioAssetList : 포트폴리오에 담긴 asset List
     * @param assetList : 포트폴리오에 담긴 asset List(Entity 버전)
     * @param targetDate : 수익률을 계산할 날짜
     * @param timeInterval : 일 단위 수익률을 계산할지, 월 단위 수익률을 계산할지, 년 단위 수익률을 계산할지
     * @return
     */
    private Double getPortfolioReturn(List<PortfolioAsset> portfolioAssetList, List<Asset> assetList, LocalDateTime targetDate, TimeInterval timeInterval) {
        // 1. targetDate를 기준으로, 각 자산들의 보유량이 얼마였는지를 계산
        List<Integer> quantityList = portfolioAssetList.stream()
            .map(portfolioAsset -> getPastQuantity(portfolioAsset, targetDate))
            .toList();

        // 2. targetDate를 기준으로, 각 자산이 얼마였는지를 계산
        List<Double> assetPrices = assetHistoryService.getAssetHistoryList(assetList, targetDate)
            .stream()
            .map(AssetHistory::getPrice)
            .toList();

        // 3. quantityList와 assetPrices를 이용하여 targetDate 기준 포트폴리오 평가액을 계산
        Double valuation = IntStream.range(0, quantityList.size())
            .mapToDouble(i -> quantityList.get(i) * assetPrices.get(i))
            .sum();

        // 4. 다음 날짜 구하기
        LocalDateTime nextDate = dateUtil.getFutureDate(targetDate, timeInterval, 1);

        // 5. nextDate를 기준으로, 각 자산이 얼마였는지를 계산
        List<Double> nextAssetPrices = assetHistoryService.getAssetHistoryList(assetList, nextDate)
            .stream()
            .map(AssetHistory::getPrice)
            .toList();

        // 6. quantityList와 nextAssetPrices를 이용하여 nextDate 기준 포트폴리오 평가액을 계산
        Double nextValuation = IntStream.range(0, quantityList.size())
            .mapToDouble(i -> quantityList.get(i) * nextAssetPrices.get(i))
            .sum();

        // 7. 수익률 반환
        return (nextValuation - valuation) / valuation * 100;
    }

    private Double getHistoricValuation(List<PortfolioAsset> portfolioAssetList, List<Asset> assetList, LocalDateTime targetDate) {
        // 특정 날짜의 자산 가격을 가져옴
        List<Double> assetPrices = assetHistoryService.getAssetHistoryList(assetList, targetDate)
            .stream()
            .map(AssetHistory::getPrice)
            .toList();

        List<Integer> quantityList = portfolioAssetList.stream()
            .map(portfolioAsset -> getPastQuantity(portfolioAsset, targetDate))
            .toList();

        // PortfolioAsset의 totalPurchaseQuantity와 assetPrices를 곱한 값을 합산
        return IntStream.range(0, assetPrices.size())
            .mapToDouble(i -> assetPrices.get(i) * quantityList.get(i))
            .sum();
    }

    private int getPastQuantity(PortfolioAsset portfolioAsset, LocalDateTime targetDate) {
        return portfolioAsset.getTotalPurchaseQuantity();
    }

    // 백테스팅 그래프를 위한 메서드
    /**
     *
     * @param portfolioAssetList : 포트폴리오 자산 관련 정보를 담는 list. 자산 구매량 정보를 담고 있다
     * @param assetList : 자산 Entity를 담고 있는 list
     * @param targetDate : valuation 를 구하기 원하는 날짜
     * @return : targetDate 기준 포트폴리오 valuation
     */
    private Double getValuation(List<PortfolioAsset> portfolioAssetList, List<Asset> assetList, LocalDateTime targetDate) {
        // 특정 날짜의 자산 가격을 가져옴
        List<Double> assetPrices = assetHistoryService.getAssetHistoryList(assetList, targetDate)
            .stream()
            .map(AssetHistory::getPrice)
            .toList();

        // PortfolioAsset의 totalPurchaseQuantity와 assetPrices를 곱한 값을 합산
        return IntStream.range(0, assetPrices.size())
            .mapToDouble(i -> portfolioAssetList.get(i).getTotalPurchaseQuantity() * assetPrices.get(i))
            .sum();
    }

    /**
     * 오늘자 자산의 가격을 가지고온다
     * (자산코드, 자산가격) 을 반환한다
     * @param assets
     * @return
     */
    private Map<String, Double> getTodayValueOfHoldings(List<PortfolioAsset> assets) {
        return assets.stream()
            .collect(Collectors.toMap(
                PortfolioAsset::getCode,
                asset -> {
                    // 오늘 일자의 자산 가격 가져오기
                    Asset todayAsset = assetRepository.findById(
                        asset.getAssetId()).orElseThrow(()  -> new MutualRiskException(ErrorCode.ASSET_NOT_FOUND));

                    // log.warn("recentAssetPrice : {}",todayAsset.getRecentPrice());

                    double totalPurchaseQuantity = asset.getTotalPurchaseQuantity();

                    return totalPurchaseQuantity * todayAsset.getRecentPrice();
                },
                Double::sum
            ));
    }

    /**
     * 자산의 비중을 반환하는 메서드
     * (자산코드, 비중) 을 반환한다
     * @param assetPrice
     * @param totalValueOfHolding
     * @return
     */
    private static Map<String, Double> getWeights(Map<String, Double> assetPrice, Double totalValueOfHolding) {
        return assetPrice.entrySet().stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> entry.getValue() * 100.0 / totalValueOfHolding
            ));
    }

    private ResponseWithData<PortfolioResultDto> buildPortfolioResponse(Portfolio portfolio, List<PortfolioAssetInfo> portfolioAssetInfoList, PortfolioPerformance portfolioPerformance) {
        PortfolioInfo portfolioInfo = PortfolioInfo.builder()
            .portfolioId(portfolio.getId())
            .performance(portfolioPerformance)
            .assets(portfolioAssetInfoList)
            .build();

        PortfolioResultDto portfolioResultDto = PortfolioResultDto.builder()
            .hasPortfolio(Boolean.TRUE)
            .portfolio(portfolioInfo)
            .build();

        return new ResponseWithData<>(HttpStatus.OK.value(), "정상적인 응답을 반환하였습니다", portfolioResultDto);
    }

    private static List<Double> calculateWeights(List<Double> purchaseAmounts, double totalAmount) {
        return purchaseAmounts.stream()
            .map(amount -> amount / totalAmount)
            .toList();
    }

    /**
     * 각 자산별 평가액 리스트를 반환하는 함수
     * @param portfolioAssetList : 자산에 대한 정보 (List<PortfolioAsset> 형식)
     * @param assetList : 자산에 대한 정보(List<Asset> 형식)
     */
    private List<Double> getAssetValuationList(List<PortfolioAsset> portfolioAssetList, List<Asset> assetList) {
        Double recentExchangeRate = exchangeRatesRepository.getRecentExchangeRate();

        return IntStream.range(0, portfolioAssetList.size())
            .mapToObj(i -> {
                PortfolioAsset portfolioAsset = portfolioAssetList.get(i);
                Asset asset = assetList.get(i);
                double price = portfolioAsset.getTotalPurchaseQuantity() * asset.getRecentPrice();
                if (asset.getRegion().equals(Region.US)) {
                    price *= recentExchangeRate;
                }
                return price;
            })
            .collect(Collectors.toList());
    }

    private List<Asset> getAssetsFromPortfolio(List<PortfolioAsset> portfolioAssetList) {
        return portfolioAssetList.stream()
            .map(portfolioAsset -> assetRepository.findById(portfolioAsset.getAssetId())
                .orElseThrow(() -> new MutualRiskException(ErrorCode.ASSET_NOT_FOUND)))
            .collect(Collectors.toList());
    }

    // 포트폴리오가 없을 경우 응답을 생성하는 메서드
    private ResponseWithData<PortfolioResultDto> buildPortfolioResponse() {
        PortfolioResultDto portfolioResultDto = PortfolioResultDto.builder()
            .hasPortfolio(Boolean.FALSE)
            .build();
        return new ResponseWithData<>(HttpStatus.OK.value(), "유저가 포트폴리오를 아직 만들지 않은 상태입니다", portfolioResultDto);
    }

    private PortfolioPerformance getPortfolioPerformance(List<Asset> assetList, List<Double> weights, double totalAmount) {
        // todo: expected_return, covariance_matrix가 데이터 적재가 덜 되어서 구하는 로직을 추가하지 않음. 추후 추가 필요

        return PortfolioPerformance.builder()
            .valuation(totalAmount)
            .build();
    }
}