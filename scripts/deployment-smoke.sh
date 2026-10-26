#!/usr/bin/env bash
set -euo pipefail
# CI-only disposable database: never run against an installed stack.
[[ "${CI:-}" == true && "${COMPOSE_PROJECT_NAME:-}" == convenience-ci-* ]] || {
  echo 'This smoke test requires an isolated convenience-ci-* project in CI' >&2
  exit 1
}
compose=(docker compose -f compose.production.yaml)
mkdir -p build/deployment-evidence
sql() {
  "${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B convenience' "$@"
}
"${compose[@]}" up -d --wait db
set +e
timeout 90 "${compose[@]}" run --rm -T app > build/deployment-evidence/empty-schema.log 2>&1
result=$?
set -e
[[ "$result" != 0 && "$result" != 124 ]]
grep -Eq 'Schema[- ]validation: missing table' build/deployment-evidence/empty-schema.log
[[ "$(printf '%s\n' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='convenience';" | sql)" == 0 ]]
echo 'PASS: empty schema rejected without creating tables' | tee build/deployment-evidence/results.log
sql < deploy/schema.sql
"${compose[@]}" up -d --wait --wait-timeout 180 app
curl --fail --silent "http://127.0.0.1:${APP_PORT}/actuator/health" > build/deployment-evidence/health.json
grep -q '"status":"UP"' build/deployment-evidence/health.json
curl --fail --silent "http://127.0.0.1:${APP_PORT}/login" > build/deployment-evidence/login.html
[[ "$(printf '%s\n' 'SELECT (SELECT COUNT(*) FROM users) + (SELECT COUNT(*) FROM order_types);' | sql)" == 0 ]]
echo 'PASS: initialized schema healthy; no automatic users or products' | tee -a build/deployment-evidence/results.log
printf '%s' '{"operation":"create-user","username":"synthetic-store","password":"synthetic-ci-only-password","role":"STORE_OWNER"}' |
  "${compose[@]}" run --rm -T --no-deps -e STORE_BOOTSTRAP=true app > build/deployment-evidence/operator-user.log 2>&1
grep -q 'operator_bootstrap status=created' build/deployment-evidence/operator-user.log
printf '%s' '{"operation":"create-product","name":"synthetic-public-item","storeOwnerUsername":"synthetic-store","quantity":7}' |
  "${compose[@]}" run --rm -T --no-deps -e STORE_BOOTSTRAP=true app > build/deployment-evidence/operator-product.log 2>&1
grep -q 'operator_bootstrap status=created' build/deployment-evidence/operator-product.log
curl --fail --silent "http://127.0.0.1:${APP_PORT}/shop" > build/deployment-evidence/public-catalog.html
grep -q 'synthetic-public-item' build/deployment-evidence/public-catalog.html
[[ "$(printf '%s\n' "SELECT COUNT(*) FROM order_types p JOIN users u ON p.store_owner_id=u.id JOIN inventory i ON i.order_type_id=p.id WHERE u.username='synthetic-store' AND u.role='STORE_OWNER' AND i.quantity=7;" | sql)" == 1 ]]
if printf '%s' '{"operation":"create-user","username":"synthetic-store","password":"synthetic-replacement-password","role":"HEADQUARTERS"}' |
  "${compose[@]}" run --rm -T --no-deps -e STORE_BOOTSTRAP=true app > build/deployment-evidence/operator-duplicate.log 2>&1; then
  echo 'Operator bootstrap unexpectedly overwrote an account' >&2
  exit 1
fi
[[ "$(printf '%s\n' "SELECT role FROM users WHERE username='synthetic-store';" | sql)" == STORE_OWNER ]]
! grep -q 'synthetic-replacement-password' build/deployment-evidence/operator-duplicate.log
echo 'PASS: stdin operator bootstrap creates scoped catalog and refuses account overwrite without logging passwords' | tee -a build/deployment-evidence/results.log
if printf '%s\n' 'CREATE TABLE forbidden_ddl (id BIGINT);' | "${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u convenience convenience' > build/deployment-evidence/ddl-denied.log 2>&1; then
  echo 'Runtime user unexpectedly has DDL permission' >&2
  exit 1
fi
grep -q 'CREATE command denied' build/deployment-evidence/ddl-denied.log
printf '%s\n' "INSERT INTO order_types(name) VALUES ('synthetic-persistence-sentinel');" | sql
"${compose[@]}" restart app
"${compose[@]}" up -d --wait --wait-timeout 180 app
[[ "$(printf '%s\n' "SELECT COUNT(*) FROM order_types WHERE name='synthetic-persistence-sentinel';" | sql)" == 1 ]]
echo 'PASS: runtime DDL denied and existing data survives restart' | tee -a build/deployment-evidence/results.log
python3 scripts/platform-upgrade-check.py
