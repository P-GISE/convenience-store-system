"""Fail closed for both actual resolved runtime and test dependency scopes."""
import json
import sys
from pathlib import Path


def check(data, status):
    if status not in (0, 1): raise ValueError('Resolved Gradle scanner error')
    summary = []
    for scope in ('runtimeClasspath', 'testRuntimeClasspath'):
        rows = [row for row in data.get('Results', []) if row.get('Type') == 'gradle'
                and scope in Path(row.get('Target', '')).parts]
        packages = sum(len(row.get('Packages') or []) for row in rows)
        if packages == 0: raise ValueError('Resolved Gradle scope UNASSESSED')
        findings = [entry for row in rows for entry in row.get('Vulnerabilities') or []]
        summary.append({'scope': scope, 'packages': packages,
                        'critical': sum(row.get('Severity') == 'CRITICAL' for row in findings),
                        'high': sum(row.get('Severity') == 'HIGH' for row in findings)})
        if findings: raise ValueError('Resolved Gradle High/Critical blocks release')
    if status: raise ValueError('Resolved Gradle scanner nonzero blocks release')
    return summary


if __name__ == '__main__':
    try:
        print(json.dumps(check(json.loads(Path(sys.argv[1]).read_text()), int(sys.argv[2]))))
    except (OSError, ValueError, KeyError, TypeError):
        raise SystemExit('Resolved Gradle runtime/test gate rejected; no release packaging allowed')
