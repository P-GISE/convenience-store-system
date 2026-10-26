"""Enforce recognized runtime Java packages and no High/Critical; print package metadata only."""
from pathlib import Path
import collections
import json
import sys

report = Path(sys.argv[1])
status = int(sys.argv[2])
if not report.exists() or status not in {0, 1}:
    raise SystemExit('Built Java scanner did not complete; blocking release')
data = json.loads(report.read_text(encoding='utf-8'))
results = [result for result in data.get('Results', []) if result.get('Type') == 'jar']
packages = sum(len(result.get('Packages') or []) for result in results)
findings = [entry for result in results for entry in (result.get('Vulnerabilities') or [])]
counts = collections.Counter(entry.get('Severity') for entry in findings)
print(json.dumps({'coverage': 'built Java runtime archives only', 'jar_targets': len(results), 'runtime_packages': packages,
                  'critical': counts['CRITICAL'], 'high': counts['HIGH']}, ensure_ascii=True))
for entry in findings:
    print(json.dumps({key: entry.get(key) for key in ['VulnerabilityID', 'PkgName', 'InstalledVersion', 'FixedVersion', 'Severity']}, ensure_ascii=True))
if packages == 0:
    raise SystemExit('Built Java metadata is UNASSESSED; zero recognized packages must not pass')
sys.exit(1 if status != 0 or findings else 0)
