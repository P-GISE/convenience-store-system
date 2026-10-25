# 편의점 시스템 운영 배포

대상은 Java 21 / Spring Boot 3.4의 **루트 프로젝트**다. 루트 `src/`가 활성 앱이며 `ordersystem2/`는 이전 복사본으로 이미지에 포함하지 않는다. 아래 최초 설치 절차는 새 환경을 위한 설명이다. 이미 운영 중인 DB에 스키마 초기화를 다시 실행하지 않는다.

## 현재 운영 구성 — 2026-10-25

| 항목 | 실제 구성 |
| --- | --- |
| 공개 주소 | https://store.pigs0516.com |
| 운영 브랜치 | `main` |
| 접속 경로 | Cloudflare HTTPS → S2 터널 → `http://127.0.0.1:3503` → 앱8080 |
| AWS 경로·Compose 프로젝트 | `/opt/convenience-store` · `convenience-store` |
| 저장 경계 | 전용 MySQL DB·사용자·Docker network·`convenience-store_database` volume |
| 비밀 주입 | 서버 root0600 `.env`와 Compose 환경값, GitHub·이미지에는 운영 비밀 없음 |
| 자동배포 | `five-sites-deploy@convenience-store.timer`, 약2분 간격·최대30초 무작위 지연 |

방문자는 Tailscale 없이 공개 HTTPS로 접속한다. Tailscale은 서버 관리 접속에 사용할 수 있으며 사이트 접속·배포 전송의 필수 경로가 아니다. 같은 AWS 서버의 다른 프로젝트와 실행·저장 경계는 분리하지만 서버 장애·용량은 공유한다.

최초 배포 커밋 `ae0df215faa65a9161090b506d1f07abb1946713`의 CI와 공개 HTTPS 합성 검증을 통과했다. 고객2·점주2의 가입/로그인/쿠키/주문 소유 경계, 타 점주 변경404, CSRF403, 중복 상품400을 확인하고 합성 행을 모두 정리했다. **실제 운영 점주·본부 계정과 상품은 아직 등록하지 않았다.** 공개 고객 가입이 운영 권한을 부여하지 않으며 운영 계정·상품은 아래 명시적 초기화 절차로 별도 등록한다.

## GitHub 변경이 반영되는 과정

1. PR을 검토하고 `main`에 반영한다. 임의 브랜치 업데이트는 운영 배포가 아니다.
2. GitHub Actions가 테스트·격리 MySQL 회귀 검증·이미지 빌드에 성공하면 해당 SHA의 불변 이미지 아티팩트를 게시한다.
3. AWS 타이머가 저장소·브랜치·정확 SHA·성공 CI·아티팩트 digest·이미지 checksum·OCI revision을 확인한다. 다른 배포와 겹치거나 메모리/디스크가 부족하면 적용하지 않는다.
4. **서버를 재부팅하지 않고 앱 컨테이너만 교체**한다. DB·volume·다른 프로젝트 컨테이너는 유지한다. 짧은 앱 재시작 동안 요청에 영향이 있을 수 있다.
5. health와 다른 컨테이너 보존을 확인하고 현재/이전 SHA·이미지를 `/var/lib/five-sites-deploy/convenience-store/deployed.json`에 기록한다. 실패하면 이전 앱 이미지·환경값 복구를 시도하며 미완료 표식이 있으면 자동 반복을 중단한다.

DB·모델·마이그레이션·Compose·CI·배포 스크립트·`src/main/resources/`(템플릿 포함) 등의 보호 경로 변경은 일반 자동배포에서 거절한다. 이 경우 별도 검토·백업·승인된 기준 갱신이 필요하며 자동 마이그레이션이나 seed를 실행하지 않는다. 문서나 보호 경로 밖 앱 코드 변경도 CI가 통과해야 반영된다. 타이머 활성화와 CI 성공만으로 반영 완료를 판정하지 않고 배포 상태 SHA와 실행 이미지 revision까지 비교한다.

운영 확인은 타이머/서비스 상태, 고정 상태 로그, `deployed.json`, loopback health와 공개 HTTPS를 함께 확인한다. `.env`·DB 비밀번호·쿠키·전체 응답 본문은 출력하지 않는다. 롤백에는 기록된 이전 이미지와 현재 스키마의 호환성을 확인하고 앱만 되돌린다. DB·volume 삭제나 서버 재부팅을 롤백으로 사용하지 않는다.

## 설치 전 조건

- Linux Docker Engine과 Compose v2 (`up --wait` 지원), 충분한 MySQL 저장공간, 별도 백업 대상.
- 검토된 커밋의 CI 통과 및 `deployment-evidence` 확인. H2 단위 테스트는 MySQL 운영 영속성 검증을 대신하지 않는다.
- 승인된 호스트 포트와 HTTPS reverse proxy. 앱은 loopback에만 공개하고 DB 포트는 공개하지 않는다. DB 연결은 프로젝트 내부 Docker network 안에서만 평문이며 원격 DB를 사용하려면 별도 TLS 구성을 검토한다.
- 공개 회원가입은 CUSTOMER로 고정된다. 고객은 본인 주문만 보고, 점주는 담당 상품의 고객 주문과 본인 발주만 처리한다. HEADQUARTERS는 재고·본부 발주 업무를 관리하며 고객 주문 개인정보 화면은 점주 전용이다. 점주·본부 계정과 담당 상품은 아래 서버 운영 초기화로 명시적으로 등록한다. 자동 관리자·상품 seed는 없다.

## 설정 계약

`.env.production.example`을 `.env.production`으로 복사하여 값만 로컬에 채운다. 저장소·이미지에 넣지 않고 파일 접근을 운영자에게 제한한다. `IMAGE_TAG`는 검토된 commit SHA, `APP_PORT`는 승인된 미사용 loopback 포트, `DB_PASSWORD`와 `MYSQL_ROOT_PASSWORD`는 서로 다른 강한 비밀이다. 빈 값은 Compose 단계에서 실패한다. 비밀이 출력될 수 있는 `docker compose config` 대신 `config -q`로 검사한다.

Compose는 앱에 `DB_URL=jdbc:mysql://db:3306/convenience?...`, `DB_USERNAME=convenience`, `DB_PASSWORD`를 전달한다. 직접 JAR를 실행할 때도 세 키가 모두 필요하다. Docker entrypoint는 `prod`를 강제한다. 운영 검사기는 DB 연결 생성 전에 누락/공백 설정, H2, demo 병용, Hibernate update/create, SQL 자동 초기화, JDBC 세션 자동 초기화를 거부한다. `prod`에서는 JPA `validate`, SQL `never`, session schema `never`를 사용한다. 기본 개발 및 demo profile은 개발용으로 유지한다.

운영 세션 쿠키는 Secure/HttpOnly/SameSite=Lax다. 브라우저 로그인은 HTTPS proxy 뒤에서 검증해야 하며 HTTP loopback health 확인만으로 로그인 성공을 주장하지 않는다. `/actuator/health`만 노출하며 DB 연결까지 확인하고 상세 정보는 반환하지 않는다.

## 최초 설치: 앱 실행과 분리된 스키마 준비

아래 명령은 **승인 후 새 전용 DB에만** 운영자가 실행한다. 기존 DB에는 적용하지 않는다. Compose 기본 프로젝트명 `convenience-store`는 전용 volume/network를 만든다. 동일 머신에 여러 설치가 있으면 일관된 `--project-name`을 정해 모든 명령에 사용한다.

```bash
docker compose --env-file .env.production -f compose.production.yaml config -q
docker compose --env-file .env.production -f compose.production.yaml build app
docker compose --env-file .env.production -f compose.production.yaml up -d --wait db
```

DB 컨테이너는 빈 database/user만 만든다. 스키마를 자동 마운트하지 않으며 앱을 먼저 시작하면 missing table 오류로 실패한다. `deploy/schema.sql`은 신규 DB 전용 baseline이다. 테이블 존재 여부를 먼저 확인하고, 하나라도 있으면 중단하여 별도 마이그레이션/백업 계획을 검토한다. DDL은 MySQL에서 부분 적용될 수 있으므로 실패 시 무조건 재실행하지 않는다.

```bash
docker compose --env-file .env.production -f compose.production.yaml exec -T db \
  sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=\"convenience\";"'
# 위 결과가 0인 새 전용 DB임을 확인하고 SQL을 검토한 후에만:
docker compose --env-file .env.production -f compose.production.yaml exec -T db \
  sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot convenience' < deploy/schema.sql
docker compose --env-file .env.production -f compose.production.yaml up -d --wait app
```

SQL은 JPA 6개 테이블과 Spring Session 2개 테이블을 만들고 앱 계정의 DDL 권한을 제거한다. `users.role`에 CUSTOMER가 있으며, `order_types.store_owner_id`와 `customer_orders.customer_id/store_owner_id` 외래 키로 소유 경계를 저장한다. seed/DROP/자동 ALTER는 없다. 이 baseline을 기존 DB에 다시 실행하지 않는다. DB 비밀번호 환경값을 바꾸는 것만으로 기존 MySQL 계정 비밀번호가 회전하지는 않는다.

## 운영 계정과 매장 상품 최초 등록

웹 회원가입으로 점주·본부 권한을 얻을 수 없다. 서버 운영자가 root 전용 0600 JSON 파일을 준비한 뒤 **표준입력**으로 전달한다. 비밀번호를 명령 인자·환경변수·GitHub·로그에 넣지 않는다. `STORE_BOOTSTRAP=true`는 일회용 컨테이너에서만 설정하며, 일반 앱에는 설정하지 않는다. 이 모드는 HTTP 서버를 열지 않고 한 작업을 처리한 뒤 종료한다.

입력 필드:

| 작업 | 필수 필드 | 제한 |
|---|---|---|
| create-user | operation, username, password, role | username 영문/숫자/`_`/`-` 3~64자, password 16자 이상·UTF-8 72바이트 이하, role STORE_OWNER 또는 HEADQUARTERS. 기존 username이면 실패하며 덮어쓰지 않음 |
| create-product | operation, name, storeOwnerUsername, quantity | 실제 STORE_OWNER 계정, 이름 1~255자, 정수 재고 0 이상. 새 상품과 해당 매장 재고를 함께 생성 |

```bash
sudo sh -c 'docker compose --project-directory /opt/convenience-store -p convenience-store \
  --env-file /opt/convenience-store/.env \
  -f /opt/convenience-store/compose.production.yaml \
  -f /opt/convenience-store/compose.image.yaml \
  run --rm -T --no-deps -e STORE_BOOTSTRAP=true app < /root/store-bootstrap.json'
```

성공은 exit 0과 `operator_bootstrap status=created`, 실패는 exit 1이다. 입력 JSON이나 비밀번호를 출력하지 않는다. DB 작업은 트랜잭션이며 실패 시 계정·상품을 부분 생성하지 않는다. create-product는 새 항목 생성이므로 같은 요청을 자동 재시도하지 않는다. 상품에 점주를 배정하지 않으면 공개 상품 목록과 고객 주문에서 제외된다. 한 주문은 한 상품에 한정되며, 서로 다른 상품 ID를 중복 전송하면 400으로 거절한다. 주문의 고객은 로그인 사용자, 점주는 상품 담당자에서 결정하고 요청에 추가된 소유자 ID는 신뢰하지 않는다.

계정·상품을 삭제하는 자동 롤백은 제공하지 않는다. 잘못된 계정/매장 정보는 운영자가 실제 사용 여부를 확인한 후 별도 수정 계획을 적용한다. 데이터베이스·볼륨을 초기화하는 방법으로 되돌리지 않는다.

## 검증과 일반 배포

```bash
bash gradlew --no-daemon test bootJar
docker compose --env-file .env.production -f compose.production.yaml ps
# 승인한 APP_PORT로 변경:
curl --fail http://127.0.0.1:<APP_PORT>/actuator/health
```

health가 `UP`이고 `/login` HTML이 반환되는지 확인한다. HTTPS 경로에서 실제 로그인·세션 지속성과 역할별 접근은 합성 테스트 계정으로 별도 확인한다. 고객 2명·점주 2명으로 본인 주문 조회, 타인 주문 상태 변경 거절, 공개 가입 권한 조작 거절, 비로그인 전화번호 조회의 주문 미노출을 확인한다. CI의 `scripts/deployment-smoke.sh`는 격리된 MySQL에서 빈 스키마 시작 거부/테이블 미생성, 명시적 bootstrap 후 health/login, 자동 seed 없음, stdin 운영 초기화와 덮어쓰기 거절, 앱 계정 DDL 거부, 재시작 후 합성 데이터 보존을 검증한다. 이 스크립트는 기존 설치에서 실행하지 않는다.

GitHub CI는 성공한 main 커밋의 이미지를 `five-sites-app-<SHA>` 아티팩트로 게시한다. AWS의 별도 검토된 pull 배포기를 설치하면 저장소·브랜치·CI 성공·SHA·체크섬을 확인한 후 앱만 교체한다. 운영 배포기의 실제 설치/활성화와 새 커밋 반영은 운영 기록에서 확인해야 한다. DB 스키마·Compose·배포 설정 변경은 자동 적용하지 않으며 검토 후 새 기준으로 등록한다. 이 권한 변경은 스키마 변경을 포함하므로 기존 DB에 자동 배포하지 않는다.

일반 앱 배포 전 DB 백업과 복구 가능성을 확인하고 이전 이미지 태그를 보관한다. 새 `IMAGE_TAG`로 build한 뒤 `up -d --no-deps --wait app`만 실행한다. `deploy/schema.sql` 재실행, `ddl-auto=update`, 자동 seed, `down -v`는 일반 배포 절차에 없다. 모델이 변경되어 validate가 실패하면 별도 검토된 스키마 변경이 필요하다.

## 롤백 및 데이터 경계

- 앱만 문제가 있으면 이전 `IMAGE_TAG`로 되돌리고 `up -d --no-deps --wait app`을 실행한다. 이미지가 없으면 해당 검토 커밋에서 다시 build한다.
- MySQL 데이터는 `<project>_database` volume에 있다. 컨테이너 교체와 앱 restart는 이를 지우지 않는다. 운영에서 `down -v`/volume 삭제는 금지한다.
- 스키마 변경 후에는 이전 코드가 현재 스키마와 호환되는지 먼저 확인한다. 앱 롤백은 DB 롤백이 아니며 DB 복구에는 검증된 별도 백업이 필요하다.
- PR/소스 브랜치 CI에는 운영 배포, 운영 비밀, 외부 계정 생성, DNS 변경이 없다. MySQL image/베이스 OS 버전 고정 digest와 조직별 보안 업데이트 정책은 운영 승인 단계에서 확정한다.
