package com.example.mutualrisk.portfolio.dto;

import java.util.List;

import com.example.mutualrisk.asset.entity.Asset;
import com.example.mutualrisk.portfolio.dto.PortfolioResponse.RecommendAssetInfo;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Setter;

public record PortfolioRequest() {


	@Builder
	@Schema(name = "포트폴리오 제작 데이터를 담는 DTO",description = "유저가 포트폴리오 제작 시 입력한"
		+ "정보를 담는다")
	public record PortfolioInitDto(

		@Schema(name = "유저가 설정한 포트폴리오 이름입니다",
			example = "포트폴리오 1번"
		)
		String name,

		@Schema(name = "유저가 설정한 투자 금액을 입력합니다",
			example="1,500,000"
		)
		Double totalCash,
		@ArraySchema(schema = @Schema(
			description = "유저가 담은 자산의 id를 담은 리스트여야 합니다.",
			example = "1,2,3,4,5",
			implementation = Integer.class
		))
		List<Integer> assetIds,
		@ArraySchema(schema = @Schema(
			description = "유저가 설정한 자산별 최소 제약조건 입니다.",
			example = "0.05,0.03,0.08,0.2,0.02",
			implementation = Double.class
		))
		@JsonProperty("lower_bounds")
		List<Double> lowerBounds,
		@ArraySchema(schema = @Schema(
			description = "유저가 설정한 자산별 최대 제약조건 입니다.",
			example = "0.5,0.5,0.5,0.5,0.5",
			implementation = Double.class
		))
		@JsonProperty("upper_bounds")
		List<Double> upperBounds,

		@JsonProperty("exact_proportion")
		@ArraySchema(schema = @Schema(
		description = "유저가 설정한 자산별 확정비율",
		example = "null,0.2,null,0.3,null",
		implementation = Double.class
		))
		List<Double> exactProportion
	){
	}

	/**
	 * 리밸런싱 포트폴리오를 제작하는 api를 호출할 때 필요한 body 데이터
	 */
	@Builder
	@Schema(name = "리밸런싱 api 호출용 request dto")
	public record RebalancePortfolioInitDto(
		String name,
		Integer extraAssetId,
		Integer extraCash
	) {

	}

	@Builder
	@Schema(name = "계산된 비중을 기반으로 새로운 자산추천을 요청하는 DTO")
	public record RecommendAssetRequestDto(
		@JsonProperty("totalCash")
		Double totalCash,
		@JsonProperty("lower_bounds")
		List<Double> lowerBounds,
		@JsonProperty("upper_bounds")
		List<Double> upperBounds,
		@JsonProperty("exact_proportion")
		List<Double> exactProportion,

		List<RecommendAssetInfo> newPortfolioAssetInfoList
	){

	}

	/**
	 * fastAPI
	 */
	@Builder
	@Schema(name = "포트폴리오 제작 fastapi를 호출할 때 필요한 요청 정보를 넣는다")
	public record PortfolioRequestDto(
		@JsonProperty("expected_returns")
		List<Double> expectedReturns,
		@JsonProperty("cov_matrix")
		Double[][] covMatrix,
		@JsonProperty("lower_bounds")
		List<Double> lowerBounds,
		@JsonProperty("upper_bounds")
		List<Double> upperBounds,
		@JsonProperty("exact_proportion")
		List<Double> exactProportion
	) {

	}

	/**
	 * hadoop 추천 api에 보낼 때 사용하는 request dto
	 */
	@Builder
	@Schema(name = "하둡 추천 api용 request dto")
	public record HadoopRecommendAssetRequestDto(

		@JsonProperty("existing_assets")
		List<Integer> existingAssets,

		@JsonProperty("new_assets")
		List<Integer> newAssets,

		@JsonProperty("lower_bounds")
		List<Double> lowerBounds,

		@JsonProperty("upper_bounds")
		List<Double> upperBounds,

		@JsonProperty("exact_proportion")
		List<Double> exactProportion
	) {

	}
}
