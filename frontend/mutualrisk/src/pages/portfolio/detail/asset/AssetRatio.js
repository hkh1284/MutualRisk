import React from 'react';
import { Stack } from '@mui/material';
import AssetRatioPieChart from 'pages/portfolio/detail/asset/AssetRatioPieChart';
import AssetList from 'pages/portfolio/detail/asset/AssetList';

const AssetRatio = () => {
	return (
		<Stack spacing={2} direction="row" sx={{ width: '100%' }}>
			<AssetRatioPieChart sx={{ flex: 1 }} />
			<AssetList sx={{ flex: 1 }} />
		</Stack>
	);
};

export default AssetRatio;
