# 편의점 재고·발주 관리 시스템

편의점 운영자의 재고 확인, 발주, 입출고, 고객 주문 처리를 돕는 Spring Boot 기반 웹 애플리케이션입니다.

재고 부족과 유통기한 관리처럼 매장 운영에서 반복되는 업무를 화면과 스케줄러로 관리할 수 있게 구성했습니다.

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

MySQL을 사용할 때는 환경변수로 DB 정보를 지정합니다.

```text
DB_URL=jdbc:mysql://localhost:3306/ordersystem2?useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=
```

## 구현 포인트

- 재고, 발주, 입출고, 고객 주문을 별도 도메인으로 분리했습니다.
- 재고 부족과 폐기 대상 같은 운영 상태를 scheduler로 자동 갱신합니다.
- 폼 기반 Thymeleaf 애플리케이션에서 CSRF 토큰을 인터셉터와 ModelAdvice로 주입합니다.
- 서비스 계층에서 재고 부족, 주문 없음, 재고 없음 같은 도메인 예외를 분리했습니다.
- 데모 데이터 초기화 클래스를 두어 로컬 시연 환경을 빠르게 만들 수 있습니다.

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
