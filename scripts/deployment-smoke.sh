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
grep -q 'Schema-validation: missing table' build/deployment-evidence/empty-schema.log
[[ "$(printf '%s\n' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='convenience';" | sql)" == 0 ]]
echo 'PASS: empty schema rejected without creating tables' | tee build/deployment-evidence/results.log
sql < deploy/schema.sql
"${compose[@]}" up -d --wait --wait-timeout 180 app
curl --fail --silent "http://127.0.0.1:${APP_PORT}/actuator/health" > build/deployment-evidence/health.json
grep -q '"status":"UP"' build/deployment-evidence/health.json
curl --fail --silent "http://127.0.0.1:${APP_PORT}/login" > build/deployment-evidence/login.html
[[ "$(printf '%s\n' 'SELECT (SELECT COUNT(*) FROM users) + (SELECT COUNT(*) FROM order_types);' | sql)" == 0 ]]
echo 'PASS: initialized schema healthy; no automatic users or products' | tee -a build/deployment-evidence/results.log
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
