import { create } from 'zustand';

const useAssetStore = create(set => ({
	tempAssets: new Set(),
	assets: [], // 자산 정보를 저장할 배열
	totalCash: 0, // 총 자산을 저장할 변수

	toggleTempAsset: asset =>
		set(state => {
			const newTempAssets = new Set(state.tempAssets);

			if (newTempAssets.has(asset)) {
				newTempAssets.delete(asset); // asset이 이미 있으면 제거
			} else {
				newTempAssets.add(asset); // asset이 없으면 추가
			}

			return { tempAssets: newTempAssets };
		}),

	resetTempAsset: () =>
		set(() => ({
			tempAssets: new Set(),
		})),

	addAsset: asset =>
		set(state => {
			// assets 배열에 동일한 asset이 있는지 확인
			const assetExists = state.assets.some(
				existingAsset => existingAsset.id === asset.id
			);

			// asset이 이미 존재하면 그대로, 존재하지 않으면 추가
			return assetExists
				? state
				: {
						assets: [...state.assets, asset],
				  };
		}),

	addAssetList: asset_list =>
		set(state => {
			const newAssets = asset_list.filter(
				newAsset =>
					!state.assets.some(
						existingAsset => existingAsset.assetId === newAsset.assetId
					)
			);

			if (newAssets.length === 0) {
				return state; // 상태 변경 없음
			}

			return {
				assets: [...state.assets, ...newAssets],
			};
		}),

	removeAsset: assetId =>
		set(state => ({
			assets: state.assets.filter(asset => asset.id !== assetId),
		})),

	// userAssets 배열을 받아 전체 assets 상태를 업데이트하는 함수
	updateAsset: userAssets =>
		set(() => ({
			assets: userAssets,
		})),

	addTotalCash: cash =>
		set(state => ({
			totalCash: state.totalCash + cash,
		})),

	updateTotalCash: cash =>
		set(() => ({
			totalCash: cash,
		})),
}));

export default useAssetStore;
