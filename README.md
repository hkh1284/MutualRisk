
# 자산 관리 종류별 유동 추천 서비스

## 기획 배경
* 코로나 이후 개인투자자가 큰 폭으로 증가하였다.
* 개인투자자는 분산투자를 잘 하지 않는 경향이 있어, 다른 투자자들에 비해 불필요한 리스크를 감수하기 쉽다
* 유저의 투자 자산 목록을 받아서, 최적 포트폴리오(주어진 수익률 하에서, 위험을 최소화하는 포트폴리오)를 구해 주자

## 세부 사항

1. 기본적으로 유저의 투자 자산 목록 기반으로 최적 포트폴리오를 만들어 준다.
이 포트폴리오를, 우리 DB에 저장 중인 자산들의 수익률과 비교해서,공분산이 가장 낮은 자산을 추천해 준다.
    - 일반적으로 공분산이 낮은 자산을 포트폴리오에 추가했을 때 리스크 감소 효과가 가장 크다

    - 너가 지금 담은 포트폴리오는 주식 위주인데, 여기에 금을 추가해서 담으면 분산 투자 효과가 크게 증가할 거 같아서 추천해주겠다

2. 특정 시간마다 유저로 하여금 자산을 리밸런싱하게 하여 고평가된 자산을 팔고 저평가된 자산을 사게 하는 것이다.
    - 유저의 포트폴리오를 기준으로, 리밸런싱을 통해 얼마의 이익을 얻었는지를 지속적으로 계산하여 산출해 준다

    - (현재 포트폴리오의 자산 가치) - (리밸런싱을 하지 않은 상황을 가정하였을 때 포트폴리오의 자산 가치)

3. Form 13F에서, 연간 1억 달러 이상을 굴리는 기관 투자자들이 분기별로 제출한 보유 자산 현황을 받아올 수 있다.
    - 이를 요약해서 가공해, 사용자들에게 종목 선정에 도움을 준다

## Todo List
* 최적 포트폴리오를 구하는 여러 방법론들이 존재함
    * MVO(Mean-Variance Optimization)과 RB(Risk Budgeting) 두 방법 중에서 적절한 방법론을 고민 중
* 최적 포트폴리오를 구할 때, 유저의 개인적인 insight에 따라 여러 조건을 걸어줄 수 있도록 생각 중임
    * ex) 삼성전자의 비중이 20%를 넘도록 추천해 줘
    * 조건을 주었을 때, 최적해를 구할 수 있는 경우와 없는 경우가 있는 것 같은데, 어느 경우에 구할 수 있는지 조사 중에 있음
* 최종적으로 추천해 주는 포트폴리오는 샤프 비율(위험 대비 수익률의 비율)을 최대로 하는 포트폴리오임
    * 우리 서비스가 타겟팅하는 사람들은 일반적으로 샤프 비율이 무엇인지, 포트폴리오에 투자했을 때 위험이 정확히 얼마가 되는지 이해하기 쉽지 않을 것
    * 포트폴리오를 추천할 때 직관적으로 이해하기 쉽도록 UI/UX적으로 고민이 필요
