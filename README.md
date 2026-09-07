# 편의점 재고·발주 관리 시스템

편의점 운영자의 재고 확인, 발주, 입출고, 고객 주문 처리를 돕는 Spring Boot 기반 웹 애플리케이션입니다.

재고 부족과 유통기한 관리처럼 매장 운영에서 반복되는 업무를 화면과 스케줄러로 관리할 수 있게 구성했습니다.

## 한눈에 보기

| 항목 | 내용 |
| --- | --- |
| 한국어 프로젝트명 | 편의점 관리 시스템 |
| 저장소 성격 | Spring Boot 기반 매장 재고/발주/주문 관리 웹앱 |
| 주요 사용자 | 편의점 관리자, 직원, 고객 주문 담당자 |
| 실행 형태 | Gradle bootRun 또는 JAR 빌드 |
| DB | MySQL 기본, H2 demo profile 지원 |
| 화면 방식 | Thymeleaf 서버 렌더링 |
| 보안 | 세션 인증, 역할 기반 접근 제어, CSRF 토큰 |

## 서비스 사용 흐름

1. 사용자가 회원가입 후 로그인합니다.
2. 관리자는 상품 재고를 등록하고 수량, 가격, 유통기한, 상태를 관리합니다.
3. 재고 부족 상품은 발주 화면에서 주문을 생성해 보충합니다.
4. 입고/출고/폐기 같은 재고 변동은 거래 내역으로 기록됩니다.
5. 고객 주문 화면에서 상품 주문을 생성하고 처리 상태를 변경합니다.
6. scheduler가 유통기한과 재고 상태를 주기적으로 점검합니다.

## 주요 기능

- 회원가입, 로그인, 세션 기반 사용자 인증
- 역할 기반 화면 접근 제어
- 상품 재고 등록, 수정, 폐기 처리
- 재고 상태와 유통기한 기반 관리
- 발주 생성, 수정, 상태 변경
- 입출고 거래 내역 조회
- 고객 주문 생성과 처리 상태 관리
- 데모 상품/사용자 초기화
- 자동 폐기와 재고 상태 갱신 scheduler
- CSRF 토큰 기반 폼 요청 보호

## 기술 스택

- Java 21
- Spring Boot 3.4
- Spring MVC
- Spring Data JPA
- Thymeleaf
- Spring Session JDBC
- MySQL
- H2
- Gradle
- Lombok

## 프로젝트 구조

```text
편의점 시스템/
  src/main/java/com/example/ordersystem2/
    config/          CSRF, 초기 데이터, MVC 설정
    controller/      인증, 대시보드, 재고, 발주, 고객 주문
    dto/             요청/응답 DTO
    exception/       도메인 예외
    model/           JPA entity와 enum
    repository/      Spring Data JPA repository
    scheduler/       자동 폐기, 재고 상태 갱신
    service/         도메인 비즈니스 로직
  src/main/resources/
    templates/       Thymeleaf 화면
    static/css/      스타일
    application.yml
  build.gradle
```

## 실행 방법

필수 환경:

- Java 21
- MySQL 또는 H2

개발 실행:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

MySQL 없이 빠르게 화면과 기능을 확인하려면 H2 demo profile을 사용할 수 있습니다.

```bash
SPRING_PROFILES_ACTIVE=demo ./gradlew bootRun
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="demo"
.\gradlew.bat bootRun
```

MySQL을 사용할 때는 환경변수로 DB 정보를 지정합니다.

```text
DB_URL=jdbc:mysql://localhost:3306/ordersystem2?useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=
```

기본 접속 주소:

```text
http://localhost:8080
```

## 주요 화면

- `/`: 홈
- `/login`, `/signup`: 로그인과 회원가입
- `/dashboard`: 운영 요약
- `/inventory`: 재고 목록
- `/inventory/add`, `/inventory/edit`, `/inventory/dispose`: 재고 등록/수정/폐기
- `/orders`: 발주 목록과 상태 관리
- `/stock-transactions`: 입출고 거래 내역
- `/customer-orders`: 고객 주문 처리
- `/customer-shop`: 고객용 주문 화면

## 구현 포인트

- 재고, 발주, 입출고, 고객 주문을 별도 도메인으로 분리했습니다.
- 재고 부족과 폐기 대상 같은 운영 상태를 scheduler로 자동 갱신합니다.
- 폼 기반 Thymeleaf 애플리케이션에서 CSRF 토큰을 인터셉터와 ModelAdvice로 주입합니다.
- 서비스 계층에서 재고 부족, 주문 없음, 재고 없음 같은 도메인 예외를 분리했습니다.
- 데모 데이터 초기화 클래스를 두어 로컬 시연 환경을 빠르게 만들 수 있습니다.

## 주요 도메인

| 도메인 | 설명 |
| --- | --- |
| `Inventory` | 상품 재고, 수량, 가격, 유통기한, 상태 |
| `Order` | 발주 요청과 처리 상태 |
| `StockTransaction` | 입고, 출고, 폐기 같은 재고 변동 기록 |
| `CustomerOrder` | 고객 주문과 수령/처리 방식 |
| `User` | 로그인 사용자와 역할 |

## 검증

```bash
./gradlew test
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## GitHub 업로드 전 주의

- DB 계정과 비밀번호는 환경변수로 관리하고 커밋하지 않습니다.
- `.gradle`, `build`, `.idea`, 로컬 백업 폴더, 검증 스크린샷 산출물은 업로드 대상에서 제외합니다.
