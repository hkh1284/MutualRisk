import React from 'react';
import { Stack } from '@mui/material';
import FundStockListItem from './FundStockListItem';
import { colors } from 'constants/colors';
import FundStockBarChart from 'pages/fund/list/FundStockBarChart';
import Title from 'components/title/Title';

const FundStockList = ({ title, data }) => {
	return (
		<Stack
			spacing={1}
			sx={{
				backgroundColor: colors.background.white,
				padding: '20px',
				borderRadius: '20px',
				border: `solid 1px ${colors.point.stroke}`,
			}}>
			<Title text={title} caption="(최근 업데이트: 2024/12/31)"></Title>
			<FundStockBarChart />
			<FundStockListItem />
			<FundStockListItem />
			<FundStockListItem />
			<FundStockListItem />
			<FundStockListItem />
			<FundStockListItem />
			<FundStockListItem />
			<FundStockListItem />
		</Stack>
	);
};

export default FundStockList;