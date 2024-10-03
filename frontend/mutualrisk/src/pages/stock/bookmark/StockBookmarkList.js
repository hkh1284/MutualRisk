import { useState } from 'react';
import { Stack, Typography } from '@mui/material';
import { colors } from 'constants/colors';
import Select from 'react-select';
import StockSearchModal from './StockSearchModal';
import RemoveCircleIcon from '@mui/icons-material/RemoveCircle';
import StockBookmarkListItem from './StockBookmarkListItem';
import SuccessSnackbar from 'components/snackbar/SuccessSnackbar';
import StockSkeletonCard from 'components/card/StockSkeletonCard';
import { removeBookmark } from 'utils/apis/interest';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import AddStockButton from './AddBookmarkButton';

const options = [
	{ value: 'NAME', label: '이름' },
	{ value: 'RETURN', label: '수익률' },
	{ value: 'PRICE', label: '가격' },
];

const StockBookmarkList = ({ isLoading, assetList }) => {
	const [selectedOption, setSelectedOption] = useState(options[0]);
	const [openSearchModal, setOpenSearchModal] = useState(false);
	const [bookmarkListEdit, setBookmarkListEdit] = useState(false);

	const [openAddSnackbar, setOpenAddSnackbar] = useState(false); // 성공했을 경우 열리는 Snackbar 상태
	const [openRemoveSnackbar, setOpenRemoveSnackbar] = useState(false); // 성공했을 경우 열리는 Snackbar 상태

	const handleAddSnackbarClose = (event, reason) => {
		if (reason === 'clickaway') {
			return;
		}
		setOpenAddSnackbar(false);
	};

	const handleRemoveSnackbarClose = (event, reason) => {
		if (reason === 'clickaway') {
			return;
		}
		setOpenRemoveSnackbar(false);
	};

	const queryClient = useQueryClient();

	// 북마크 추가를 처리하는 Mutation
	const removeMutation = useMutation({
		mutationFn: removeBookmark,
		onSuccess: () => {
			// 북마크 추가 후에 북마크 리스트를 다시 가져오기
			queryClient.invalidateQueries('bookmark');
			setOpenAddSnackbar(true);
		},
		// Optimistic 처리를 위해 onMutate 사용
		onMutate: assetId => {
			const queryAssetList = queryClient.getQueryData(['bookmark']); // queryClient 내 저장되어 있는 assetList 값
			queryAssetList.assets = queryAssetList.assets.filter(asset => {
				return asset !== assetId;
			});
			return () => queryClient.setQueryData(() => queryAssetList);
		},
	});

	const handleRemoveBookmark = assetId => {
		removeMutation.mutate(assetId);
	};

	// 정렬을 처리하는 Mutation
	const orderMutation = useMutation({
		onMutate: () => {
			// 로딩이 아직 완료되지 않았다면 정렬 필요 X
			if (isLoading) {
				return;
			}
			const queryAssetList = queryClient.getQueryData(['bookmark']); // queryClient 내 저장되어 있는 assetList 값

			if (selectedOption.value === 'NAME') {
				queryAssetList.assets = queryAssetList.assets.sort(
					(a, b) => a.name > b.name
				);
			} else if (selectedOption.value === 'RETURN') {
				queryAssetList.assets = queryAssetList.assets.sort(
					(a, b) =>
						parseFloat(b.dailyPriceChangeRate) -
						parseFloat(a.dailyPriceChangeRate)
				);
			} else if (selectedOption.value === 'PRICE') {
				queryAssetList.assets = queryAssetList.assets.sort(
					(a, b) => parseInt(b.price) - parseInt(a.price)
				);
			}

			return queryClient.setQueryData(() => queryAssetList);
		},
	});

	const handleSelectedOptionChange = () => {
		orderMutation.mutate();
	};

	return (
		<Stack
			spacing={1}
			sx={{
				backgroundColor: colors.background.white,
				padding: '15px',
				borderRadius: '10px',
				border: `solid 1px ${colors.point.stroke}`,
			}}>
			<Stack
				direction="row"
				sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
				<Typography
					sx={{ textDecoration: 'underline', cursor: 'pointer' }}
					onClick={() => setBookmarkListEdit(!bookmarkListEdit)}>
					{bookmarkListEdit ? '저장' : '편집'}
				</Typography>
				<Select
					defaultValue={selectedOption}
					value={selectedOption}
					options={options}
					onChange={newOption => {
						setSelectedOption(newOption);
						handleSelectedOptionChange();
					}}
				/>
			</Stack>
			<AddStockButton setOpenSearchModal={setOpenSearchModal} />

			{isLoading &&
				Array(10)
					.fill(0)
					.map((_, index) => {
						return <StockSkeletonCard key={index} />;
					})}

			{!isLoading &&
				assetList.map((asset, index) => {
					return (
						<Stack
							key={index}
							direction="row"
							sx={{
								justifyContent: 'flex-end',
								alignItems: 'center',
								width: '100%',
							}}>
							{bookmarkListEdit && (
								<Stack
									sx={{
										justifyContent: 'center',
										alignItems: 'center',
										padding: '10px',
										opacity: bookmarkListEdit ? 1 : 0, // opacity에 따라 부드럽게 보이도록
										transform: bookmarkListEdit
											? 'scale(1)'
											: 'scale(0.8)', // scale로 크기 애니메이션
										transition: 'all 0.3s ease', // transition 적용
									}}>
									<RemoveCircleIcon
										onClick={() =>
											handleRemoveBookmark(asset.assetId)
										}
										sx={{
											color: colors.point.red,
											'&:hover': { color: colors.point.redHover },
											transition: '0.3s',
											cursor: 'pointer',
										}}
									/>
								</Stack>
							)}
							<StockBookmarkListItem asset={asset} />
						</Stack>
					);
				})}

			<StockSearchModal
				open={openSearchModal}
				handleClose={() => {
					setOpenSearchModal(false);
				}}
				setOpenAddSnackbar={setOpenAddSnackbar}
				setOpenRemoveSnackbar={setOpenRemoveSnackbar}
				assetList={assetList}
			/>
			<SuccessSnackbar
				message="북마크에 추가하였습니다"
				openSnackbar={openAddSnackbar}
				// openSnackbar={true}
				handleSnackbarClose={handleAddSnackbarClose}
			/>
			<SuccessSnackbar
				message="북마크에서 제거하였습니다"
				openSnackbar={openRemoveSnackbar}
				// openSnackbar={true}
				handleSnackbarClose={handleRemoveSnackbarClose}
			/>
		</Stack>
	);
};

export default StockBookmarkList;