# Authentication abuse limits

The active root src application limits POST /login and /signup before account lookup, password hashing or registration. Existing CSRF/session protections remain enabled.

- Per process: 300 authentication requests/minute globally, 60/minute per resolved client.
- Signup: 3 attempts/hour per normalized, SHA-256-hashed username, including invalid forms.
- Login: after 5 authentication failures per username within 15 minutes, requests receive HTTP429 and Retry-After (30s, then 60s, doubling to at most15min). Successful login resets that account's failure counter. Other accounts sharing the same client remain usable within the client budget.
- Counters have at most4096 entries. Expired entries are reclaimed; exhaustion rejects new buckets instead of allowing unlimited work. These process-local limits reset after application restart; replicas need a shared limiter before scaling. They complement edge controls and do not claim distributed/global enforcement.

## Ingress contract

Default AUTH_TRUSTED_PROXY_ADDRESSES is empty: no forwarding header is trusted. Production disables automatic forwarded-header rewriting. Only an explicitly configured exact immediate TCP peer may supply one numeric CF-Connecting-IP. X-Forwarded-For and comma-separated or non-numeric values are ignored. Never use wildcard trust, a whole Docker/private subnet, or a user-controlled header as the peer identity.

Current infrastructure metadata shows loopback127.0.0.1:3503 mapped to app8080 and Docker gateway172.25.0.1. This is a candidate, not a measured TCP peer. Before setting AUTH_TRUSTED_PROXY_ADDRESSES=172.25.0.1, the operator must confirm the actual application socket peer and that only the local Cloudflare connector can reach this published origin with provider-overwritten CF-Connecting-IP. This source PR does not change live env, DNS, edge/access policy or DB. Without that confirmation, keep the default; shared gateway traffic intentionally shares the60/min client budget. Health and ordinary application requests are outside this limiter.

## Verification and rollback

MockMvc reproduces the prior failure to return429, then verifies signup blocking and repeated failed-login cooldown while another account remains usable. Clock-driven tests verify expiry/progressive cooldown and exact-proxy header handling. Full H2-backed suite and bootJar are required before release. No public login attempts or production accounts are used.

Rollback is the previous tested image, retaining DB/data and ingress policy. It restores the missing application throttling risk. Prefer a forward fix; do not disable CSRF or publicly bind the origin to troubleshoot. No schema migration or new paid resource is needed.
