import React from 'react';
import { Box, Typography, Avatar, Stack } from '@mui/material';
import { colors } from 'constants/colors'; // 컬러 상수 불러오기
import StockItemCard from 'components/card/StockItemCard';

const stockInfoSample = {
	title: '엔비디아',
	market: 'NASDAQ',
	symbol: 'NVDA',
	price: 13.55,
	fluctuateRate: 3.2,
	fluctuatePrice: 0.66,
	imageURL:
		'https://thumb.tossinvest.com/image/resized/96x0/https%3A%2F%2Fstatic.toss.im%2Fpng-icons%2Fsecurities%2Ficn-sec-fill-NAS00208X-E0.png',
};

const StockChangeListItem = ({ stock }) => {
	return (
		<Stack
			direction={'row'}
			spacing={1}
			sx={{
				display: 'flex',
				justifyContent: 'space-between',
				alignItems: 'center',
				marginBottom: '15px',
			}}>
			{/* 좌측 주식 정보 */}
			<Stack>
				<StockItemCard
					code={stockInfoSample.symbol}
					name={stockInfoSample.title}
					market={stockInfoSample.market}
					image={stockInfoSample.imageURL}
				/>
			</Stack>

			{/* 기존 주식 가격 및 수량 */}
			<Stack
				direction="column"
				alignItems="start"
				sx={{
					flex: 1,
					justifyContent: 'center',
					backgroundColor: colors.background.box,
					borderRadius: '20px',
					padding: '10px',
				}}>
				<Typography fontSize={10}>기존 주 수</Typography>
				<Stack direction="row" alignItems="center">
					<Typography
						sx={{
							fontSize: '16px',
							color: colors.text.sub1,
							marginRight: '5px',
						}}>
						{stock.currentPrice}원
					</Typography>
					<Typography sx={{ fontSize: '16px', fontWeight: 'bold' }}>
						{stock.currentShares}주
					</Typography>
				</Stack>
			</Stack>

			{/* 리밸런싱 후 가격 및 수량 */}
			<Stack
				direction="column"
				alignItems="start"
				sx={{
					// flex: 1,
					justifyContent: 'center',
					backgroundColor: colors.background.box,
					borderRadius: '20px',
					padding: '10px',
				}}>
				<Typography fontSize={10}>리밸런싱 주 수</Typography>
				<Stack direction="row" alignItems="center">
					<Typography
						sx={{
							fontSize: '16px',
							color: colors.text.sub1,
							marginRight: '5px',
						}}>
						{stock.rebalancedPrice}원
					</Typography>
					<Typography sx={{ fontSize: '16px', fontWeight: 'bold' }}>
						{stock.rebalancedShares}주
					</Typography>
				</Stack>
			</Stack>

			{/* 보유량 변화 */}
			<Box
				sx={{
					flex: 1,
					display: 'flex',
					justifyContent: 'center',
					alignItems: 'center',
					backgroundColor: colors.main.primary200,
					borderRadius: '20px',
					padding: '10px',
				}}>
				<Typography
					sx={{
						fontSize: '16px',
						color: colors.text.sub1,
						marginRight: '5px',
					}}>
					{stock.rebalancedPrice}원
				</Typography>
				<Typography
					sx={{
						color: stock.change > 0 ? 'red' : colors.text.sub1,
						fontSize: '16px',
						fontWeight: 'bold',
					}}>
					{stock.change > 0 ? `+${stock.change}주` : `-${stock.change}주`}
				</Typography>
			</Box>
		</Stack>
	);
};

export default StockChangeListItem;