# Spring Boot 4 플랫폼 보안 갱신

상태: 소스 구현·로컬 검증 완료, 격리 Linux/MySQL 호환성 CI 확인 전. 운영 적용 없음.

## 변경 범위

- Java 21을 유지하면서 Spring Boot 3.4.0 → 4.0.9, Gradle 8.14.6, dependency-management 1.1.7을 사용한다.
- Boot 4 모듈형 MVC·Session JDBC·MVC 테스트 starter와 Jackson 3의 대응 API로 전환한다. 불필요한 javax.servlet-api와 중복 Session core 직접 선언을 제거한다.
- 업무 Entity, MySQL 스키마, 공개 URL/API, 비밀번호 BCrypt 형식, 고객·점주 권한, 세션 쿠키 정책, Compose를 변경하지 않는다.
- 운영의 `ddl-auto=validate`, SQL 초기화 `never`, Session 스키마 초기화 `never`를 유지한다. 자동 DDL을 추가하지 않는다.

## 검증

공식 권고에 따라 최신 3.5.16을 중간 점검한 뒤 4.0.9로 전환했다. 각 단계의 기존 53개 테스트와 bootJar 빌드가 통과했다. 최종 Gradle runtimeClasspath 잠금 스캔은 103개 패키지, 실제 bootJar rootfs 스캔은 90개 패키지를 인식했고, 2026-10-26 로컬 Trivy DB 기준 전체 심각도 취약점 0이었다. 빈 대상 스캔 결과로 대체하지 않는다.

`scripts/deployment-smoke.sh`가 추가 실행하는 `platform-upgrade-check.py`는 GitHub Actions/Linux/고유 `convenience-ci-*` 프로젝트에서만 동작한다. 기존 소스 보안 PR의 고정 커밋 `064f63b44ed714c189b1984c4a6e6cdd599db249` 이미지를 빌드해 같은 **일회용 CI MySQL**에서 old → new → old → new 순으로 앱만 교체한다.

게이트는 다음을 실제 HTTP·DB로 확인한다.

- information_schema의 컬럼·ENUM·기본값·인덱스·FK fingerprint 불변
- 기존 BCrypt 비밀번호 로그인·해시 불변과 신규 비밀번호의 구버전 로그인
- 기존/신규 Spring Session JDBC 직렬화 세션과 CSRF 토큰의 양방향 호환
- 고객별 주문 조회 분리, 점주별 주문 조회 분리 및 타 점주 상태 변경 404
- 픽업 주문 행/상태 보존, 잘못된 CSRF 차단 및 로그아웃

테스트 데이터는 합성 계정·상품·주문뿐이다. Secure 쿠키의 실제 응답 플래그를 검사하면서 CI loopback HTTP 전송에서만 테스트 CookieJar가 이를 돌려준다. 운영 쿠키 설정을 완화하지 않는다. 쿠키·토큰·비밀번호 해시·업무 본문은 증거로 출력하지 않고 결과 boolean만 저장한다.

## 적용과 롤백

CI와 독립 검토를 통과한 정확한 커밋의 이미지로 앱 하나만 교체한다. DB DDL·데이터 이전·새 유료 자원은 필요 없다. 기존 이미지와 환경설정을 보존하고 이상 시 이전 이미지로 앱만 복귀한다. old→new→old 게이트 통과 전에는 세션/DB 호환 완료 또는 운영 적용 가능으로 판정하지 않는다. 플랫폼 변경으로 인한 외부 브라우저 장애 해결을 주장하지 않는다.

근거: [Boot 4 요구사항](https://docs.spring.io/spring-boot/4.0/system-requirements.html), [공식 마이그레이션 안내](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).
