#!/usr/bin/env python3
"""CI-only MySQL/serialized-session old -> candidate -> old compatibility gate."""
import hashlib
import http.cookiejar
import io
import json
import os
import re
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser
from pathlib import Path

BASELINE = "064f63b44ed714c189b1984c4a6e6cdd599db249"
PASSWORD = "synthetic-compat-password-123"
ROOT = Path(__file__).resolve().parents[1]


def require(condition, label):
    if not condition:
        raise RuntimeError(label)


class Form(HTMLParser):
    def __init__(self, html):
        super().__init__()
        self.csrf = None
        self.product = None
        self.select = None
        self.in_order_id = False
        self.orders = set()
        self.feed(html)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "input" and attrs.get("name") == "_csrf":
            self.csrf = attrs.get("value")
        if tag == "select":
            self.select = attrs.get("name")
        if tag == "option" and self.select == "orderTypeId" and attrs.get("value"):
            self.product = self.product or attrs["value"]
        if tag == "th" and attrs.get("scope") == "row" and attrs.get("data-label") == "주문 ID":
            self.in_order_id = True

    def handle_endtag(self, tag):
        if tag == "select":
            self.select = None
        if tag == "th":
            self.in_order_id = False

    def handle_data(self, data):
        if self.in_order_id and data.strip().isdigit():
            self.orders.add(int(data.strip()))


class LoopbackCookiePolicy(http.cookiejar.DefaultCookiePolicy):
    def return_ok_secure(self, cookie, request):
        # Test server-session persistence over the CI-only loopback origin.
        # The app's Secure/HttpOnly/SameSite response flags are checked below.
        return urllib.parse.urlsplit(request.full_url).hostname == "127.0.0.1"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, newurl):
        return None


class Client:
    def __init__(self, base):
        self.base = base
        self.jar = http.cookiejar.CookieJar(LoopbackCookiePolicy())
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar), NoRedirect())

    def request(self, path, values=None):
        require(path.startswith("/") and not path.startswith("//"), "non_local_request")
        data = urllib.parse.urlencode(values).encode() if values is not None else None
        request = urllib.request.Request(self.base + path, data=data)
        if data is not None:
            request.add_header("Content-Type", "application/x-www-form-urlencoded")
        try:
            response = self.opener.open(request, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read(1024 * 1024 + 1)
            require(len(body) <= 1024 * 1024, "unexpected_html_size")
            return response.status, response.headers, body.decode("utf-8")

    def form(self, path):
        status, _, html = self.request(path)
        require(status == 200, "expected_html_page")
        return Form(html)

    def signup(self, username):
        token = self.form("/signup").csrf
        status, headers, _ = self.request("/signup", {
            "_csrf": token, "username": username, "password": PASSWORD, "confirmPassword": PASSWORD,
        })
        require(status in (302, 303) and "signup_success" in headers.get("Location", ""), "signup_contract")
        self.login(username)

    def login(self, username, password=PASSWORD):
        token = self.form("/login").csrf
        status, headers, _ = self.request("/login", {"_csrf": token, "username": username, "password": password})
        require(status in (302, 303), "password_hash_login")
        cookies = headers.get_all("Set-Cookie") or []
        require(any("Secure" in value and "HttpOnly" in value and "SameSite=Lax" in value for value in cookies), "cookie_policy_changed")

    def order(self, token=None):
        form = self.form("/shop")
        require(form.product is not None and form.csrf is not None, "authenticated_customer_form")
        status, headers, _ = self.request("/shop", {
            "_csrf": token or form.csrf, "orderTypeId": form.product, "quantity": "1",
            "customerName": "Synthetic compatibility", "customerPhone": "0000000000",
            "fulfillmentType": "PICKUP", "requestMemo": "synthetic compatibility only",
        })
        require(status in (302, 303) and "success=created" in headers.get("Location", ""), "order_or_legacy_csrf_contract")
        return self.form("/shop").orders


def main():
    require(sys.platform == "linux" and os.environ.get("CI") == "true"
            and os.environ.get("GITHUB_ACTIONS") == "true", "requires_isolated_linux_actions")
    project = os.environ.get("COMPOSE_PROJECT_NAME", "")
    require(re.fullmatch(r"convenience-ci-[A-Za-z0-9_-]+", project), "requires_ci_project_namespace")
    tag = os.environ.get("IMAGE_TAG", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]+", tag), "invalid_candidate_tag")
    port = int(os.environ["APP_PORT"])
    require(1024 <= port <= 65535, "invalid_loopback_port")
    base = f"http://127.0.0.1:{port}"
    candidate = "convenience-store:" + tag
    baseline_image = "store-compat-baseline:" + project
    evidence = ROOT / "build/deployment-evidence"
    evidence.mkdir(parents=True, exist_ok=True)
    compose = ["docker", "compose", "--project-name", project, "-f", str(ROOT / "compose.production.yaml")]

    def run(args, label, data=None):
        with (evidence / (label + ".log")).open("wb") as output:
            result = subprocess.run(args, cwd=ROOT, input=data, stdout=output, stderr=subprocess.STDOUT, timeout=600)
        require(result.returncode == 0, label)

    def sql(statement):
        result = subprocess.run(compose + ["exec", "-T", "db", "sh", "-c",
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B convenience'],
            input=statement.encode(), capture_output=True, cwd=ROOT, timeout=30)
        require(result.returncode == 0, "compat_sql_failed")
        return result.stdout.decode().strip()

    def fingerprint():
        statements = [
            "SELECT TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION,COLUMN_TYPE,IS_NULLABLE,COALESCE(COLUMN_DEFAULT,'<NULL>'),EXTRA,COALESCE(COLLATION_NAME,'') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,ORDINAL_POSITION;",
            "SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,INDEX_TYPE FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;",
            "SELECT TABLE_NAME,COLUMN_NAME,CONSTRAINT_NAME,COALESCE(REFERENCED_TABLE_NAME,''),COALESCE(REFERENCED_COLUMN_NAME,'') FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION;",
        ]
        return hashlib.sha256("\n".join(sql(query) for query in statements).encode()).hexdigest()

    def password_hash(username):
        require(username in ("compat-old", "compat-new"), "unexpected_fixture_account")
        return sql("SELECT password FROM users WHERE username='" + username + "';")

    with tempfile.TemporaryDirectory(prefix="store-platform-", dir=os.environ["RUNNER_TEMP"]) as temporary:
        temporary = Path(temporary)
        checkout = temporary / "baseline"
        checkout.mkdir()
        if subprocess.run(["git", "cat-file", "-e", BASELINE + "^{commit}"], cwd=ROOT, capture_output=True).returncode:
            run(["git", "fetch", "--no-tags", "--depth=1", "origin", BASELINE], "compat-fetch-baseline")
        archive = subprocess.run(["git", "archive", BASELINE], cwd=ROOT, capture_output=True, check=True).stdout
        with tarfile.open(fileobj=io.BytesIO(archive)) as source:
            source.extractall(checkout, filter="data")
        run(["docker", "build", "--build-arg", "REVISION_SHA=" + BASELINE,
             "-t", baseline_image, str(checkout)], "compat-build-baseline")
        override = temporary / "app-image.yaml"

        def switch(image, label):
            require(image in (baseline_image, candidate), "unexpected_image")
            override.write_text("services:\n  app:\n    image: " + image + "\n")
            run(compose + ["-f", str(override), "up", "-d", "--no-deps", "--force-recreate",
                           "--wait", "--wait-timeout", "180", "app"], label)

        before_schema = fingerprint()
        switched = False
        try:
            switched = True
            switch(baseline_image, "compat-start-old")
            run(compose + ["-f", str(override), "run", "--rm", "-T", "--no-deps",
                           "-e", "STORE_BOOTSTRAP=true", "app"], "compat-second-owner",
                json.dumps({"operation": "create-user", "username": "compat-owner",
                            "password": PASSWORD, "role": "STORE_OWNER"}).encode())
            owner = Client(base)
            owner.login("synthetic-store", "synthetic-ci-only-password")
            other_owner = Client(base)
            other_owner.login("compat-owner")

            def owner_boundaries(order_ids):
                require(order_ids <= owner.form("/customer-orders").orders, "owner_session_or_visibility")
                other_form = other_owner.form("/customer-orders")
                require(not other_form.orders, "other_owner_order_leak")
                status, _, _ = other_owner.request("/customer-orders/" + str(min(order_ids)) + "/status",
                    {"_csrf": other_form.csrf, "status": "PREPARING"})
                require(status == 404, "other_owner_status_write")

            old_customer = Client(base)
            old_customer.signup("compat-old")
            old_orders = old_customer.order()
            require(len(old_orders) == 1, "legacy_order_created")
            owner_boundaries(old_orders)
            old_csrf = old_customer.form("/shop").csrf
            old_hash = password_hash("compat-old")
            require(old_hash.startswith("$2a$"), "legacy_bcrypt_format")
            require(int(sql("SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES WHERE LEFT(HEX(ATTRIBUTE_BYTES),8)='ACED0005';")) > 0,
                    "legacy_jdk_serialized_attributes")
            other_customer = Client(base)
            other_customer.signup("compat-other")
            require(not other_customer.form("/shop").orders, "customer_isolation_old")

            switch(candidate, "compat-start-new")
            require(old_orders <= old_customer.form("/shop").orders, "new_reads_legacy_session")
            require(len(old_customer.order(old_csrf)) == 2, "new_accepts_legacy_csrf")
            owner_boundaries(old_orders)
            require(not other_customer.form("/shop").orders, "customer_isolation_new")
            Client(base).login("compat-old")
            require(password_hash("compat-old") == old_hash, "legacy_hash_changed")
            new_customer = Client(base)
            new_customer.signup("compat-new")
            new_orders = new_customer.order()
            new_csrf = new_customer.form("/shop").csrf
            require(len(new_orders) == 1, "new_order_created")
            new_hash = password_hash("compat-new")
            require(fingerprint() == before_schema, "new_changed_schema")

            switch(baseline_image, "compat-rollback-old")
            require(new_orders <= new_customer.form("/shop").orders, "old_reads_new_session")
            require(len(new_customer.order(new_csrf)) == 2, "old_accepts_new_csrf")
            owner_boundaries(old_orders | new_orders)
            Client(base).login("compat-new")
            require(password_hash("compat-old") == old_hash and password_hash("compat-new") == new_hash, "rollback_hash_changed")
            require(not other_customer.form("/shop").orders, "customer_isolation_rollback")
            require(fingerprint() == before_schema, "rollback_changed_schema")
            require(int(sql("SELECT COUNT(*) FROM customer_orders WHERE fulfillment_type='PICKUP' AND status='RECEIVED';")) == 4,
                    "enum_or_business_rows_changed")
            status, _, _ = old_customer.request("/shop", {"quantity": "1"})
            require(status == 403, "csrf_guard_changed")
            status, _, _ = new_customer.request("/logout", {"_csrf": new_customer.form("/shop").csrf})
            require(status in (302, 303) and not new_customer.form("/shop").orders, "logout_contract_changed")
            result = {"schema_unchanged": True, "legacy_hash_preserved": True, "new_hash_rollback_readable": True,
                      "legacy_session_read_by_new": True, "new_session_read_by_old": True,
                      "cross_version_csrf": True, "customer_isolation": True, "owner_isolation": True,
                      "logout_preserved": True,
                      "business_enum_rows_preserved": True}
            (evidence / "platform-upgrade.json").write_text(json.dumps(result, indent=2) + "\n")
            print("PASS: isolated MySQL old/new/old schema, bcrypt, serialized sessions, CSRF, customer boundaries")
        finally:
            if switched:
                switch(candidate, "compat-restore-candidate")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        label = str(error) if type(error) is RuntimeError else "unexpected_gate_error"
        if not re.fullmatch(r"[a-z_]+", label):
            label = "unexpected_gate_error"
        print("FAIL: isolated platform compatibility gate: " + label, file=sys.stderr)
        sys.exit(1)
