import React, { useState, useRef, useEffect } from 'react';
import { useLocation, useNavigate, Outlet } from 'react-router-dom';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import { colors } from 'constants/colors';
import logo from 'assets/images/logo.png';

const MainLayout = () => {
	const location = useLocation();
	const navigate = useNavigate();
	const isHomePage = location.pathname === '/'; // HomePage 여부 확인

	const headerRef = useRef(null); // 헤더 요소를 참조하기 위한 ref
	const [headerHeight, setHeaderHeight] = useState(0); // 헤더 높이 저장

	// 헤더 높이를 계산하여 상태에 설정
	useEffect(() => {
		if (headerRef.current) {
			setHeaderHeight(headerRef.current.offsetHeight);
		}
	}, []);

	return (
		<>
			<Stack
				direction="row"
				ref={headerRef} // ref를 Stack에 연결
				sx={{
					position: 'fixed',
					top: 0,
					left: 0,
					width: '100%',
					backgroundColor: colors.background.primary,
					borderBottom: 'solid 1px',
					padding: '10px 80px',
					alignItems: 'center',
					justifyContent: 'space-between',
					zIndex: 1000,
					boxSizing: 'border-box',
				}}>
				{/* 로고 */}
				<Box
					component="img"
					src={logo}
					alt="logo"
					sx={{
						height: '30px',
						width: 'auto',
						cursor: 'pointer',
						padding: '8px',
					}}
					onClick={() => navigate('/')}
				/>
				{/* '로그인' 버튼 : HomePage에서만 노출 */}
				{isHomePage && (
					<Box
						sx={{
							width: '60px',
							textAlign: 'center',
							cursor: 'pointer',
							padding: '7px 15px',
							borderRadius: '5px',
							fontWeight: 'bold',
							color: colors.text.sub1,
							transition: 'background-color 0.3s ease, color 0.3s ease',
							whiteSpace: 'nowrap',
							'&:hover': {
								backgroundColor: colors.background.box,
								color: colors.main.primary400,
							},
						}}
						onClick={() => navigate('/login')}>
						로그인
					</Box>
				)}
			</Stack>

			<Box
				sx={{
					width: '100%',
					height: '100%',
					padding: `${headerHeight + 20}px 20px 20px 20px`,
					backgroundColor: colors.background.primary,
				}}>
				<Outlet />
			</Box>
		</>
	);
};

export default MainLayout;
