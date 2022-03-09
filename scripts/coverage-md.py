#!/usr/bin/env python3
"""Regenerates COVERAGE.md from what the suites actually wrote to disk.

Java: target/site/jacoco/jacoco.csv per service, and the aggregate under build/coverage-aggregate.
Node: coverage/coverage-summary.json (jest json-summary reporter).
Python: nothing to read, and the table says so.

Run via `make coverage`. Do not edit COVERAGE.md by hand; the presenter reads the numbers off it
and they have to match the reports (PLAT-1244).
"""
import csv, json, os, sys, datetime

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
os.chdir(ROOT)

SERVICES = [
    # name, stack, target, note
    ('bff-retail',                 'Node 18 / NestJS 9',      35, ''),
    ('bff-business',               'Node 18 / NestJS 9',      30, ''),
    ('beacon-notifications',       'Java 11 / Boot 2.7.18',   25, 'no tests on `SequenceCoordinator` (PLAT-1288)'),
    ('alerts-preferences-service', 'Java 11 / Boot 2.7.18',   40, ''),
    ('txn-posting-service',        'Java 11 / Boot 2.7.18',   15, 'no reversal or idempotency edge tests (PLAT-1201)'),
    ('pii-vault-service',          'Java 11 / Boot 2.7.18',    8, 'FPE known-answer test only (RA-2022-0341)'),
    ('audit-trail-service',        'Java 11 / Boot 2.7.18',   12, 'Kafka consumer untested (PLAT-1289)'),
    ('entitlements-service',       'Java 17 / Boot 3.1.12',   55, ''),
    ('bedrock-adapter',            'Java 11 / Boot 2.7.18',   20, 'codec covered, MQ paths not'),
    ('iris-orchestrator',          'Node 18 / NestJS 9',      40, ''),
    ('documents-service',          'Node 18 / Express',       25, 'statements/tax routes via smoke only (PLAT-1877)'),
    ('statements-api',             'Python 3.11 / FastAPI',  None, 'no test framework, no tests, no CI test stage (CAB-2021-1188)'),
    ('exposure-calc',              'Python 3.11 / FastAPI',  None, 'no tests, no infrastructure'),
]

def jacoco(path):
    m = c = 0
    with open(path) as f:
        for r in csv.DictReader(f):
            m += int(r['LINE_MISSED']); c += int(r['LINE_COVERED'])
    return (100.0 * c / (m + c), m + c) if m + c else (0.0, 0)

def jest(path):
    with open(path) as f:
        t = json.load(f)['total']
    return t['lines']['pct'], t['lines']['total'], t['branches']['pct']

rows, missing, agg_lines = [], [], 0
for name, stack, target, note in SERVICES:
    jc = f'services/{name}/target/site/jacoco/jacoco.csv'
    js = f'services/{name}/coverage/coverage-summary.json'
    if stack.startswith('Java'):
        if not os.path.exists(jc):
            missing.append(name); rows.append((name, stack, target, None, None, 'report missing', note)); continue
        pct, lines = jacoco(jc)
        rows.append((name, stack, target, pct, lines, 'JaCoCo line', note))
    elif stack.startswith('Node'):
        if not os.path.exists(js):
            missing.append(name); rows.append((name, stack, target, None, None, 'report missing', note)); continue
        pct, lines, br = jest(js)
        rows.append((name, stack, target, pct, lines, f'Jest line (branches {br:.1f}%)', note))
    else:
        rows.append((name, stack, target, None, None, 'none', note))

agg = 'build/coverage-aggregate/target/site/jacoco-aggregate/jacoco.csv'
agg_pct, agg_lines = jacoco(agg) if os.path.exists(agg) else (None, 0)

measured = [(p, l) for _, _, _, p, l, _, _ in rows if p is not None]
overall = sum(p * l for p, l in measured) / sum(l for _, l in measured) if measured else 0

today = datetime.date.today().isoformat()
out = []
out.append(f'# Coverage\n')
out.append(f'Generated {today} by `make coverage` (`scripts/coverage-md.py`). Line coverage, as reported by the tools;')
out.append('the Sonar quality gate reads the same files. Numbers here are the numbers, do not round them up in')
out.append('slide decks.\n')
out.append('| Service | Stack | Target | Actual | Lines | Source | Note |')
out.append('|---|---|---|---|---|---|---|')
for name, stack, target, pct, lines, src, note in rows:
    t = f'{target}%' if target is not None else '-'
    a = f'**{pct:.1f}%**' if pct is not None else '-'
    l = str(lines) if lines else '-'
    out.append(f'| `{name}` | {stack} | {t} | {a} | {l} | {src} | {note} |')
out.append('')
out.append(f'**Overall (line-weighted across the {len(measured)} measured services): {overall:.1f}%.**')
if agg_pct is not None:
    out.append(f'JaCoCo aggregate across the seven Java services: **{agg_pct:.1f}%** over {agg_lines} lines')
    out.append('(`build/coverage-aggregate/target/site/jacoco-aggregate/index.html`).')
out.append('')
out.append('## Reading this table\n')
out.append('The compliance critical services (txn-posting, pii-vault, audit-trail) are the worst covered. That is')
out.append('not an accident of history so much as a consequence of it: they were written under deadline, their')
out.append('owners changed, and each has a risk acceptance or CAB exception standing in for the tests. The')
out.append('exceptions are listed in each service README under Known Issues. GIS have asked for a remediation')
out.append('plan (GIS-2201); there is a plan, it is a Confluence page, and it has no dates on it.\n')
out.append('The two Python services report nothing because there is nothing to report. statements-api is on the')
out.append('documents-service smoke path and is the one people worry about.\n')
out.append('Targets are the `coverageThreshold` in each Jenkinsfile and the matching `sonar-project.properties`.')
out.append('A service under its target fails its pipeline; when that happens the fix has historically been to')
out.append('lower the target (see git log for txn-posting-service, 2022) rather than to write the tests.')
if missing:
    out.append(f'\nReports missing at generation time for: {", ".join(missing)}. Run `make test` first.')
print('\n'.join(out))
