import React, { useEffect, useState } from 'react';
import WidgetContainer from 'components/container/WidgetConatiner';
import Title from 'components/title/Title';
import {
	LineChart,
	Line,
	XAxis,
	YAxis,
	CartesianGrid,
	Tooltip,
	Legend,
	ResponsiveContainer,
} from 'recharts';
import { Stack } from '@mui/material';
import StockMenuButton from 'pages/stock/detail/StockMenuButton';
import { colors } from 'constants/colors';
import {
	fetchBackTestByPortfolioId,
	fetchPortfolioList,
} from 'utils/apis/analyze';
import { useQuery } from '@tanstack/react-query';

const BackTesting = ({
	portfolioId,
	timeInterval = 'day',
	measure = 'profit',
}) => {
	const [mergedData, setMergedData] = useState([]);
	const [tabMenu, setTabMenu] = useState(timeInterval);
	const [error, setError] = useState(null);

	// Use react-query to fetch portfolio list and cache it
	const { data: portfolioListData } = useQuery({
		queryKey: ['portfolioList'],
		queryFn: fetchPortfolioList,
		staleTime: 300000, // Cache for 5 minutes
	});

	// Find the latest portfolioId from cached portfolio list
	const latestPortfolioId = portfolioListData?.portfolioList?.[0]?.id;

	useEffect(() => {
		const fetchData = async () => {
			try {
				// Fetch data for selected portfolio
				const selectedResponse = await fetchBackTestByPortfolioId(
					portfolioId,
					tabMenu,
					measure
				);
				let selectedPortfolioPerformances =
					selectedResponse.performances.map(item => ({
						time: new Date(item.time).toISOString().split('T')[0],
						selectedValuation: item.valuation,
					}));

				let latestPortfolioPerformances = [];

				// Fetch data for latest portfolio if it's different from selected
				if (latestPortfolioId && latestPortfolioId !== portfolioId) {
					const latestResponse = await fetchBackTestByPortfolioId(
						latestPortfolioId,
						tabMenu,
						measure
					);
					latestPortfolioPerformances = latestResponse.performances.map(
						item => ({
							time: new Date(item.time).toISOString().split('T')[0],
							latestValuation: item.valuation,
						})
					);
				}

				// Slice for yearly data if needed
				if (tabMenu === 'year') {
					selectedPortfolioPerformances =
						selectedPortfolioPerformances.slice(-10);
					latestPortfolioPerformances =
						latestPortfolioPerformances.slice(-10);
				}

				// Merge data based on time
				const merged = selectedPortfolioPerformances.reduce(
					(acc, selectedItem) => {
						const matchingLatestItem = latestPortfolioPerformances.find(
							latestItem => latestItem.time === selectedItem.time
						);
						acc.push({
							time: selectedItem.time,
							selectedValuation: selectedItem.selectedValuation,
							latestValuation: matchingLatestItem
								? matchingLatestItem.latestValuation
								: null,
						});
						return acc;
					},
					[]
				);

				setMergedData(merged);
			} catch (error) {
				console.error('Error fetching backtesting data:', error);
				setError('데이터를 가져오는 중 오류가 발생했습니다.');
			}
		};

		if (portfolioId) {
			fetchData();
		}
	}, [portfolioId, tabMenu, measure, latestPortfolioId]);

	if (error) {
		return <div>{error}</div>;
	}

	const minValue = Math.min(
		...mergedData.map(item => item.selectedValuation),
		...mergedData.map(item => item.latestValuation || Infinity)
	);
	const maxValue = Math.max(
		...mergedData.map(item => item.selectedValuation),
		...mergedData.map(item => item.latestValuation || -Infinity)
	);

	const formatNumber = value =>
		value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	const formatCurrency = value => `${formatNumber(Math.floor(value))} 원`;

	return (
		<WidgetContainer>
			<Title text="백테스팅" />
			<Stack
				direction="row"
				spacing={1}
				sx={{
					backgroundColor: colors.background.primary,
					width: 'fit-content',
					borderRadius: '100px',
					border: `1px solid ${colors.point.stroke}`,
				}}>
				<StockMenuButton
					label="일"
					value="day"
					onChange={() => setTabMenu('day')}
					selected={tabMenu === 'day'}
				/>
				<StockMenuButton
					label="주"
					value="week"
					onChange={() => setTabMenu('week')}
					selected={tabMenu === 'week'}
				/>
				<StockMenuButton
					label="월"
					value="month"
					onChange={() => setTabMenu('month')}
					selected={tabMenu === 'month'}
				/>
				<StockMenuButton
					label="년"
					value="year"
					onChange={() => setTabMenu('year')}
					selected={tabMenu === 'year'}
				/>
			</Stack>

			<ResponsiveContainer width="100%" height={400}>
				<LineChart
					data={mergedData}
					margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
					<CartesianGrid strokeDasharray="3 3" />
					<XAxis dataKey="time" />
					<YAxis
						domain={[minValue * 0.9, maxValue * 1.1]}
						tickCount={6}
						tickFormatter={formatNumber}
					/>
					<Tooltip formatter={value => `${formatCurrency(value)}`} />
					<Legend />
					{latestPortfolioId === portfolioId ? (
						<Line
							type="monotone"
							dataKey="selectedValuation"
							name="현재 포트폴리오 평가 가치"
							stroke="#82ca9d"
						/>
					) : (
						<>
							<Line
								type="monotone"
								dataKey="selectedValuation"
								name="과거 포트폴리오 평가 가치"
								stroke="#8884d8"
							/>
							<Line
								type="monotone"
								dataKey="latestValuation"
								name="현재 포트폴리오 평가 가치"
								stroke="#82ca9d"
							/>
						</>
					)}
				</LineChart>
			</ResponsiveContainer>
		</WidgetContainer>
	);
};

export default BackTesting;