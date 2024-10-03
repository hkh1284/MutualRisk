import axiosInstance from './axiosInstance';

// 포트폴리오 분석 관련 API
// React-Query 단에서 Mutate가 요구되는 API들(POST, DELETE)은 Promise를 반환하도록
// 그 외, fetch만 필요한 API들(GET)은 실제 API 응답의 data 맴버 내 실제 데이터를 반환

/**
 * 유저 전체 포트폴리오 조회
 * @returns {Object} - Response의 data 내 정보 반환, API 문서 참조
 */
export const fetchPortfolioList = async () => {
	const response = await axiosInstance.get('/portfolio/my');
	return response.data.data;
};

/**
 * 유저 포트폴리오 조회
 * @param {Number} portfolioId - portfolioId
 * @returns {Object} - Response 내 data 객체, API 문서 참조
 */
export const fetchPortfolioByPorfolioId = async portfolioId => {
	const response = await axiosInstance.get(`/portfolio/detail?portfolioId=${portfolioId}`);
	return response.data.data;
};

/**
 * 포트폴리오 백테스팅 결과 조회
 * @typedef {String} timeInterval - 
 * @typedef {String} measure - 
 * @typedef {Number} portfolioId
 * @param {{}} assetId - assetId
 * @returns {Object} - Response 내 data 객체, API 문서 참조
 */
export const fetchStockDetailByAssetId = async assetId => {
	const response = await axiosInstance.get(
		`/portfolio/backtest?assetId=${assetId}`
	);
	return response.data.data;
};

/**
 * ETF 상세정보 조회
 * @param {Number} assetId - assetId
 * @returns {Object} - Response 내 data 객체, API 문서 참조
 */
export const fetchEtfByAssetId = async assetId => {
	const response = await axiosInstance.get(
		`/asset/detail/etf?assetId=${assetId}`
	);
	return response.data.data;
};

/**
 * 자산의 기간 내 종가 조회
 * @typedef {Number} assetId
 * @typedef {Number} period - 조회 기간 (일 단위)
 * @param {{assetId, period}}
 * @returns {Object} - Response 내 data 객체, API 문서 참조
 */
export const fetchAssetHistoryByAssetId = async ({assetId, period}) => {
	const response = await axiosInstance.get(
		`/asset/history/${assetId}?period=${period}`
	);
	return response.data.data;
};