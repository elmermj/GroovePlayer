#!/usr/bin/env python3
"""Resolve the local debug version from the Distribute workflow.

Prints "<versionName> <versionCode>" on stdout. Warnings go to stderr.

A commit GitHub already built is 1.0.<run number>, the same version App Tester
shows. A commit with no Distribute run uses 1.0.<latest run on this branch>-local,
plus -dirty when the worktree has uncommitted changes. If GitHub cannot be
reached, the name is 1.0.0-local-<short sha>.
"""

import json
import re
import subprocess
import sys

GH_RUN_LIST = [
    "gh",
    "run",
    "list",
    "--workflow",
    "distribute-apk.yml",
    "--json",
    "number,headSha,headBranch,status,conclusion",
    "--limit",
    "100",
]


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def choose_version(runs, head, branch, short, dirty):
    """Pick (versionName, versionCode, warning or None) from workflow runs.

    runs is None when GitHub could not be reached.
    """
    if runs is None:
        name = f"1.0.0-local-{short}"
        return (
            name,
            0,
            f"warning: could not reach GitHub to look up the Distribute run; using {name}",
        )

    head_key = head.lower()
    matches = [
        run
        for run in runs
        if str(run.get("headSha") or "").lower() == head_key and run.get("number") is not None
    ]
    if matches:
        number = max(int(run["number"]) for run in matches)
        return f"1.0.{number}", number, None

    branch_runs = [
        run
        for run in runs
        if run.get("headBranch") == branch and run.get("number") is not None
    ]
    if branch_runs:
        number = max(int(run["number"]) for run in branch_runs)
        name = f"1.0.{number}-local"
        if dirty:
            name += "-dirty"
        return name, number, None

    name = f"1.0.0-local-{short}"
    return (
        name,
        0,
        f"warning: no Distribute run for this commit or branch; using {name}",
    )


def fetch_runs():
    """Return Distribute runs, or None when GitHub cannot be reached."""
    try:
        gh = subprocess.run(GH_RUN_LIST, check=False, capture_output=True, text=True)
    except FileNotFoundError:
        gh = None
    if gh is not None and gh.returncode == 0:
        try:
            payload = json.loads(gh.stdout)
        except json.JSONDecodeError:
            payload = None
        if isinstance(payload, list):
            return payload
    return fetch_runs_via_api()


def fetch_runs_via_api():
    try:
        token_proc = subprocess.run(
            ["gh", "auth", "token"], check=False, capture_output=True, text=True
        )
    except FileNotFoundError:
        return None
    if token_proc.returncode != 0:
        return None
    token = token_proc.stdout.strip()
    if not token:
        return None

    remote = subprocess.run(
        ["git", "remote", "get-url", "origin"], check=False, capture_output=True, text=True
    )
    if remote.returncode != 0:
        return None
    match = re.search(
        r"github\.com[:/](?P<owner>[^/]+)/(?P<repo>[^/]+?)(?:\.git)?$",
        remote.stdout.strip(),
    )
    if not match:
        return None

    url = (
        "https://api.github.com/repos/"
        f"{match.group('owner')}/{match.group('repo')}"
        "/actions/workflows/distribute-apk.yml/runs?per_page=100"
    )
    try:
        curl = subprocess.run(
            [
                "curl",
                "-fsSL",
                "-H",
                f"Authorization: Bearer {token}",
                "-H",
                "Accept: application/vnd.github+json",
                "-H",
                "X-GitHub-Api-Version: 2022-11-28",
                url,
            ],
            check=False,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        return None
    if curl.returncode != 0:
        return None
    try:
        payload = json.loads(curl.stdout)
    except json.JSONDecodeError:
        return None

    runs = []
    for item in payload.get("workflow_runs") or []:
        number = item.get("run_number")
        if number is None:
            continue
        runs.append(
            {
                "number": number,
                "headSha": item.get("head_sha") or "",
                "headBranch": item.get("head_branch") or "",
                "status": item.get("status") or "",
                "conclusion": item.get("conclusion") or "",
            }
        )
    return runs


def main():
    head = git("rev-parse", "HEAD")
    short = git("rev-parse", "--short", "HEAD")
    branch = git("rev-parse", "--abbrev-ref", "HEAD")
    dirty = git("status", "--porcelain") != ""
    try:
        runs = fetch_runs()
    except Exception:
        runs = None
    name, code, warning = choose_version(runs, head, branch, short, dirty)
    if warning:
        print(warning, file=sys.stderr)
    print(f"{name} {code}")


if __name__ == "__main__":
    main()
