# 편의점 시스템 배포 준비

이 문서는 설치 계획이며 운영 공개·DNS·기존 DB 연결 승인이 아니다. 대상은 Java 21 / Spring Boot 3.4의 **루트 프로젝트**다. README의 소스 검토 기준이 루트 `src/`를 활성 앱으로 지정하고, 루트 `settings.gradle`에 하위 프로젝트 include가 없으며 보안·고객 주문·동시성 회귀 테스트도 루트에 있다. `ordersystem2/`는 이전 복사본으로 이미지에 포함하지 않는다.

## 설치 전 조건

- Linux Docker Engine과 Compose v2 (`up --wait` 지원), 충분한 MySQL 저장공간, 별도 백업 대상.
- 검토된 커밋의 CI 통과 및 `deployment-evidence` 확인. H2 단위 테스트는 MySQL 운영 영속성 검증을 대신하지 않는다.
- 승인된 호스트 포트와 HTTPS reverse proxy. 앱은 loopback에만 공개하고 DB 포트는 공개하지 않는다. DB 연결은 프로젝트 내부 Docker network 안에서만 평문이며 원격 DB를 사용하려면 별도 TLS 구성을 검토한다.
- 현재 앱은 공개 회원가입으로 STORE_OWNER 권한을 준다. 인터넷 공개 전에 승인된 조직 접근 제어(VPN 또는 인증 proxy 등)를 적용해야 한다. HEADQUARTERS 계정/상품의 최초 등록과 권한 부여는 별도 운영 절차가 필요하다. 이 패키지는 기본 관리자나 데모 상품을 생성하지 않는다.

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

SQL은 JPA 6개 테이블과 Spring Session 2개 테이블을 만들고 앱 계정의 DDL 권한을 제거한다. seed/DROP/자동 ALTER는 없다. 신규 상품/조직 계정 등록은 승인된 별도 업무다. DB 비밀번호 환경값을 바꾸는 것만으로 기존 MySQL 계정 비밀번호가 회전하지는 않는다.

## 검증과 일반 배포

```bash
bash gradlew --no-daemon test bootJar
docker compose --env-file .env.production -f compose.production.yaml ps
# 승인한 APP_PORT로 변경:
curl --fail http://127.0.0.1:<APP_PORT>/actuator/health
```

health가 `UP`이고 `/login` HTML이 반환되는지 확인한다. HTTPS 경로에서 실제 로그인·세션 지속성과 역할별 접근은 승인된 테스트 계정으로 별도 확인한다. CI의 `scripts/deployment-smoke.sh`는 격리된 MySQL에서 빈 스키마 시작 거부/테이블 미생성, 명시적 bootstrap 후 health/login, seed 없음, 앱 계정 DDL 거부, 재시작 후 합성 데이터 보존을 검증한다. 이 스크립트는 기존 설치에서 실행하지 않는다.

일반 앱 배포 전 DB 백업과 복구 가능성을 확인하고 이전 이미지 태그를 보관한다. 새 `IMAGE_TAG`로 build한 뒤 `up -d --no-deps --wait app`만 실행한다. `deploy/schema.sql` 재실행, `ddl-auto=update`, 자동 seed, `down -v`는 일반 배포 절차에 없다. 모델이 변경되어 validate가 실패하면 별도 검토된 스키마 변경이 필요하다.

## 롤백 및 데이터 경계

- 앱만 문제가 있으면 이전 `IMAGE_TAG`로 되돌리고 `up -d --no-deps --wait app`을 실행한다. 이미지가 없으면 해당 검토 커밋에서 다시 build한다.
- MySQL 데이터는 `<project>_database` volume에 있다. 컨테이너 교체와 앱 restart는 이를 지우지 않는다. 운영에서 `down -v`/volume 삭제는 금지한다.
- 스키마 변경 후에는 이전 코드가 현재 스키마와 호환되는지 먼저 확인한다. 앱 롤백은 DB 롤백이 아니며 DB 복구에는 검증된 별도 백업이 필요하다.
- PR/소스 브랜치 CI에는 운영 배포, 운영 비밀, 외부 계정 생성, DNS 변경이 없다. MySQL image/베이스 OS 버전 고정 digest와 조직별 보안 업데이트 정책은 운영 승인 단계에서 확정한다.
