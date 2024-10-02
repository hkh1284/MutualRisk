package com.example.mutualrisk.portfolio.controller;

import java.util.List;

import com.example.mutualrisk.asset.dto.AssetResponse.*;
import com.example.mutualrisk.common.enums.PerformanceMeasure;
import com.example.mutualrisk.common.enums.TimeInterval;
import com.example.mutualrisk.common.exception.ErrorCode;
import com.example.mutualrisk.common.exception.MutualRiskException;
import com.example.mutualrisk.common.dto.CommonResponse.ResponseWithMessage;
import com.example.mutualrisk.fund.dto.FundResponse.SectorInfo;
import com.example.mutualrisk.portfolio.dto.PortfolioRequest;
import com.example.mutualrisk.portfolio.dto.PortfolioRequest.PortfolioInitDto;
import com.example.mutualrisk.portfolio.service.PortfolioService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.mutualrisk.common.dto.CommonResponse.ResponseWithData;

import com.example.mutualrisk.portfolio.dto.PortfolioResponse.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolio")
@Slf4j
@Tag(name = "포트폴리오관련 API",
    description = "유저 포트폴리오 조회,백테스팅 Controller입니다")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "유저 포트폴리오 현황 요약", description = "버전에따른 포트폴리오 현황을 반환한다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "포트폴리오 현황조회 성공")
    })
    @GetMapping("/summary")
    public ResponseEntity<ResponseWithData<PortfolioStatusSummary>> UserPortfolioSummary(@RequestParam("ver") Integer version,HttpServletRequest request){

        Integer userId = (Integer) request.getAttribute("userId");

        ResponseWithData<PortfolioStatusSummary> portfolioSummary = portfolioService.userPortfolioSummary(userId,version);

        return ResponseEntity.status(portfolioSummary.status())
            .body(portfolioSummary);
    }

    @Operation(summary = "유저 포트폴리오 제작 미리보기", description = "포트폴리오 제작 버튼을 누르면 예상 비중을 반환한다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "포트폴리오 미리보기 성공")
    })
	@PostMapping("/init")
	public ResponseEntity<ResponseWithData<PortfolioAnalysis>> initUserPortfolio(@RequestBody PortfolioInitDto initInfo) {

		ResponseWithData<PortfolioAnalysis> portfolioInfo = portfolioService.initPortfolio(initInfo);

		return ResponseEntity.status(portfolioInfo.status())
			.body(portfolioInfo);
	}

    @Operation(summary = "유저 전체 포트폴리오 조회", description = "유저가 만든 전체 포트폴리오 내역을 조회한다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "유저 전체 포트폴리오 조회 성공")
    })
    @GetMapping("/my")
    public ResponseEntity<ResponseWithData<List<SimplePortfolioDto>>> getAllUserPortfolio(HttpServletRequest request) {
        Integer userId = (Integer)request.getAttribute("userId");

        ResponseWithData<List<SimplePortfolioDto>> allUserPortfolio = portfolioService.getAllUserPortfolio(userId);

        return ResponseEntity.status(allUserPortfolio.status()).body(allUserPortfolio);
    }

    /**
     * 포트폴리오 세부 정보를 반환하는 메서드
     */
    @Operation(summary = "포트폴리오 세부 정보 조회", description = "유저의 특정 버전에 해당하는 포트폴리오를 조회한다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "포트폴리오 세부 정보 조회 성공")
    })
    @GetMapping("/detail")
    public ResponseEntity<ResponseWithData<PortfolioResultDto>> getUserPortfolio(@RequestParam(value = "portfolioId") String portfolioId, HttpServletRequest request) {
        Integer userId = (Integer)request.getAttribute("userId");

        ResponseWithData<PortfolioResultDto> portfolioInfo = portfolioService.getPortfolioInfo(userId, portfolioId);

        return ResponseEntity.status(portfolioInfo.status()).body(portfolioInfo);
    }

    /**
     * 전체 유저를 대상으로 비중변화가 +-10%p or 유저의 상한/하한을 넘은 경우 알람을 보내는 메서드
     * 이 메서드는 일반 유저가 접근할 수 없이 ADMIN 권한의 유저만 접근할 수 있도록 설정해야 할 듯
     *
     * Todo: 메서드 접근 권한 - 사실
     * @return
     */
    @Operation(summary = "메일발송", description = "리밸런싱이 필요한 유저에게 메일을 발송하는 api")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메일 발송 성공"),
    })
    @PostMapping("/checking")
    @Hidden
    public ResponseEntity<ResponseWithMessage> checkPortfolios(){

        ResponseWithMessage responseWithMessage = portfolioService.sendRefreshMail();

        return ResponseEntity.status(responseWithMessage.status()).body(responseWithMessage);
    }

    /**
     * 포트폴리오 백테스팅 결과를 조회하는 api
     * 백테스팅 시점은, 현재로부터 특정 기간 동안의 과거 데이터를 기반으로 한다
     */
    @Operation(summary = "백테스팅 결과 조회", description = "현재 포트폴리오 자산 보유량 기준으로, 과거 평가액을 조회하는 api")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "백테스팅 결과 조회 성공")
    })
    @GetMapping("/backtest")
    public ResponseEntity<ResponseWithData<PortfolioValuationDto>> getUserPortfolioReturn(@RequestParam(value = "portfolioId") String portfolioId, @RequestParam(value = "timeInterval", required = false, defaultValue = "DAY") String timeIntervalString, @RequestParam(value = "measure", required = false, defaultValue = "PROFIT") String measureString, HttpServletRequest request) {
        Integer userId = (Integer)request.getAttribute("userId");

        // parameter(Enum) 초기화
        TimeInterval timeInterval;
        PerformanceMeasure measure;
        try {
            timeInterval = TimeInterval.valueOf(timeIntervalString.toUpperCase());
            measure = PerformanceMeasure.valueOf(measureString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MutualRiskException(ErrorCode.PARAMETER_INVALID);
        }

        ResponseWithData<PortfolioValuationDto> portfolioBacktestingResultDto = portfolioService.getUserPortfolioPerformance(timeInterval, measure, userId, portfolioId);

        return ResponseEntity.status(portfolioBacktestingResultDto.status()).body(portfolioBacktestingResultDto);
    }

    @Operation(summary = "섹터정보 조회", description = "유저 포트폴리오의 섹터 편중을 조회하는 api")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "섹터 조회 성공"),
    })
    @GetMapping("/sector")
    public ResponseEntity<ResponseWithData<List<SectorInfo>>> getUserPortfolioSector(@RequestParam(value = "portfolioId") String portfolioId, HttpServletRequest request) {

        Integer userId = (Integer)request.getAttribute("userId");

        ResponseWithData<List<SectorInfo>> userPortfolioSector = portfolioService.getUserPortfolioSector(userId, portfolioId);

        return ResponseEntity.status(userPortfolioSector.status())
            .body(userPortfolioSector);
    }

    /**
     * 현재 포트폴리오의 효율적 프론티어 곡선을 이루는 점들의 좌표를 반환하는 함수
     * (x좌표 : 포트폴리오의 분산(volatility), y좌표 : 포트폴리오의 기대 수익률(expected_return))
     */
    @Operation(summary = "효율적 프론티어 조회", description = "효율적 프론티어를 이루는 각 점들의 성과 지표를 조회하는 api")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "효율적 포트폴리오 곡선 데이터 정상 반환"),
    })
    @GetMapping("/frontier")
    public ResponseEntity<ResponseWithData<FrontierDto>> getFrontierPoints(@RequestParam(value = "portfolioId") String portfolioId, HttpServletRequest request) {
        Integer userId = (Integer)request.getAttribute("userId");

        ResponseWithData<FrontierDto> frontierDtoResponseWithData = portfolioService.getFrontierPoints(userId, portfolioId);

        return ResponseEntity.status(frontierDtoResponseWithData.status())
            .body(frontierDtoResponseWithData);
    }

    /**
     * 포트폴리오의 자산 평가액을 조회하는 api
     * 조회하는 시점은, 포트폴리오가 마지막으로 유지된 시점으로부터, 특정 기간 동안의 과거 데이터를 기반으로 한다
     */
    @Operation(summary = "포트폴리오 평가액 추이 조회", description = "각 기간별 포트폴리오의 자산 평가액을 조회하는 api")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "자산 평가액 조회 성공"),
    })
    @GetMapping("/valuation")
    public ResponseEntity<ResponseWithData<PortfolioValuationDto>> getValuation(@RequestParam(value = "portfolioId") String portfolioId, @RequestParam(value = "timeInterval", required = false, defaultValue = "DAY") String timeIntervalString, @RequestParam(value = "measure", required = false, defaultValue = "VALUATION") String measureString, HttpServletRequest request) {
        Integer userId = (Integer)request.getAttribute("userId");

        // parameter(Enum) 초기화
        TimeInterval timeInterval;
        PerformanceMeasure measure;
        try {
            timeInterval = TimeInterval.valueOf(timeIntervalString.toUpperCase());
            measure = PerformanceMeasure.valueOf(measureString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MutualRiskException(ErrorCode.PARAMETER_INVALID);
        }

        ResponseWithData<PortfolioValuationDto> portfolioValuationDtoResponseWithData = portfolioService.getHistoricalValuation(timeInterval, measure, userId, portfolioId);

        return ResponseEntity.status(portfolioValuationDtoResponseWithData.status())
            .body(portfolioValuationDtoResponseWithData);
    }

    /**
     * 포트폴리오 월별 수익률을 조회하는 api
     * 조회하는 시점은, 포트폴리오가 마지막으로 유지된 시점으로부터, 특정 기간 동안의 과거 데이터를 기반으로 한다
     */
    @Operation(summary = "포트폴리오 monthly return 조회", description = "포트폴리오의 월별 수익률을 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "자산 평가액 조회 성공"),
    })

    @GetMapping("/monthly-return")
    public ResponseEntity<ResponseWithData<List<PortfolioReturnDto>>> getMonthlyReturn(
        @RequestParam(value = "portfolioId") String portfolioId,
        @RequestParam(value = "measure", required = false, defaultValue = "PROFIT") String measureString,
        HttpServletRequest request) {
        Integer userId = (Integer)request.getAttribute("userId");

        // parameter(Enum) 초기화
        PerformanceMeasure measure;
        try {
            measure = PerformanceMeasure.valueOf(measureString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MutualRiskException(ErrorCode.PARAMETER_INVALID);
        }

        ResponseWithData<List<PortfolioReturnDto>> portfolioValuationDtoResponseWithData = portfolioService.getHistoricalReturns(TimeInterval.MONTH, measure, userId, portfolioId);

        return ResponseEntity.status(portfolioValuationDtoResponseWithData.status())
            .body(portfolioValuationDtoResponseWithData);
    }

    /**
     * 포트폴리오의 보유 자산 목록을 보여주는 api
     * 기본 자산 정보에 더해서, 자산별 보유 비중과 자산 평가액을 추가로 반환
     */
    @GetMapping("/assets")
    public ResponseEntity<ResponseWithData<List<PortfolioAssetInfo>>> getPortfolioAssetList(
        @RequestParam(value = "portfolioId") String portfolioId,
        HttpServletRequest request) {
        Integer userId = (Integer)request.getAttribute("userId");

        ResponseWithData<List<PortfolioAssetInfo>> portfolioAssetInfo = portfolioService.getAssetInfoList(userId, portfolioId);

        return ResponseEntity.status(portfolioAssetInfo.status())
            .body(portfolioAssetInfo);
    }
}
