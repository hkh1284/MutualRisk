import React, { useState } from 'react';
import DetailContainer from 'pages/portfolio/rebalance/result/detail/DetailContainer';
import {
	BarChart,
	Bar,
	XAxis,
	YAxis,
	CartesianGrid,
	Tooltip,
	ResponsiveContainer,
} from 'recharts';
import { colors } from 'constants/colors';
import PortfolioAssetList from '../../main/piechart/PortfolioAssetList';

const RebalanceAfter = ({ rebalanceData }) => {
	const [highlightedStockIndex, setHighlightedStockIndex] = useState(null);

	// 리밸런싱 후 포트폴리오 자산 정보
	const newAssets =
		rebalanceData?.data?.newPortfolioAssetInfoList?.map(asset => ({
			name: asset.name,
			value: asset.weight * 100, // weight 비율을 퍼센트로 변환
			code: asset.code,
			market: asset.market,
			weight: asset.weight,
		})) || [];

	return (
		<DetailContainer title={'리밸런싱 후 포트폴리오'}>
			<div style={{ display: 'flex', justifyContent: 'space-between' }}>
				<ResponsiveContainer width="60%" height={300}>
					<BarChart
						data={newAssets}
						layout="vertical"
						margin={{
							top: 5,
							right: 30,
							left: 20,
							bottom: 5,
						}}
						barSize={20}
						onMouseMove={state => {
							if (state.isTooltipActive) {
								const index = state.activeTooltipIndex; // 마우스가 위치한 바의 인덱스를 가져옴
								setHighlightedStockIndex(index);
							} else {
								setHighlightedStockIndex(null);
							}
						}}>
						<XAxis type="number" />
						<YAxis type="category" dataKey="name" />
						<Tooltip />
						<CartesianGrid strokeDasharray="3 3" />
						<Bar
							dataKey="value"
							name="비율"
							fill={colors.main.primary500}
							background={{ fill: '#eee' }}
						/>
					</BarChart>
				</ResponsiveContainer>

				{/* PortfolioAssetList로 UI 대체 */}
				<PortfolioAssetList
					assets={newAssets}
					hoveredIndex={highlightedStockIndex} // 강조될 항목의 인덱스 전달
				/>
			</div>
		</DetailContainer>
	);
};

export default RebalanceAfter;
