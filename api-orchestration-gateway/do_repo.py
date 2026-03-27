#!/usr/bin/env python3
import json
import os
import shutil
import subprocess
import sys
import urllib.error
import urllib.request

USERNAME = "vakkalakulalokesh"
REPO_NAME = "fullstack-system-design-projects"
WORK = "/Users/vakkalakula.lokesh/api-orchestration-gateway/fullstack-gh-bundle2"
README_JSON = "/Users/vakkalakula.lokesh/api-orchestration-gateway/readme_payload.json"

GITIGNORE = 'target/\nnode_modules/\n.DS_Store\n*.class\n.env\n.idea/\n*.iml\ndist/\n'

RSYNC_EXCLUDES = [
    "--exclude=target/",
    "--exclude=.git",
    "--exclude=.git/",
    "--exclude=.DS_Store",
    "--exclude=node_modules/",
    "--exclude=*.class",
]


def run_git(args, cwd):
    r = subprocess.run(["git"] + args, cwd=cwd, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stdout, r.stderr, file=sys.stderr)
        raise SystemExit(r.returncode)
    return r


def github_post_create(token):
    import json as _json
    body = _json.dumps(
        {
            "name": REPO_NAME,
            "description": "Full-stack system design projects with Java Spring Boot, RabbitMQ, Angular and React - Job Scheduler and API Orchestration Gateway",
            "public": True,
            "auto_init": False,
        }
    ).encode()
    req = urllib.request.Request(
        "https://api.github.com/user/repos",
        data=body,
        method="POST",
    )
    req.add_header("Authorization", f"token {token}")
    req.add_header("Accept", "application/vnd.github.v3+json")
    req.add_header("User-Agent", "do_repo.py")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status, _json.loads(resp.read().decode() or "null")
    except urllib.error.HTTPError as e:
        err = e.read().decode()
        if e.code == 422 and "already exists" in err:
            return 409, {"message": err}
        print(f"HTTP {e.code}: {err}", file=sys.stderr)
        raise


def main():
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        print("Missing GITHUB_TOKEN", file=sys.stderr)
        sys.exit(1)

    readme = json.load(open(README_JSON, encoding="utf-8"))

    shutil.rmtree(WORK, ignore_errors=True)
    os.makedirs(WORK, exist_ok=True)
    subprocess.run(["git", "init"], cwd=WORK, check=True)

    subprocess.run(
        ["rsync", "-av"]
        + RSYNC_EXCLUDES
        + ["/Users/vakkalakula.lokesh/job-scheduling-platform/", os.path.join(WORK, "job-scheduling-platform/")],
        check=True,
    )
    subprocess.run(
        ["rsync", "-av"]
        + RSYNC_EXCLUDES
        + ["/Users/vakkalakula.lokesh/api-orchestration-gateway/", os.path.join(WORK, "api-orchestration-gateway/")],
        check=True,
    )

    nested = os.path.join(WORK, "api-orchestration-gateway", "fullstack-gh-bundle2")
    if os.path.isdir(nested):
        shutil.rmtree(nested)
    for junk in (
        "github-fullstack-bundle",
        "fullstack-system-design-projects-staging",
        "fullstack-gh-bundle",
        "_build_fullstack_repo.py",
        "do_repo.py",
        "readme_payload.json",
    ):
        jp = os.path.join(WORK, "api-orchestration-gateway", junk)
        if os.path.exists(jp):
            if os.path.isdir(jp):
                shutil.rmtree(jp)
            else:
                os.remove(jp)

    open(os.path.join(WORK, "README.md"), "w", encoding="utf-8").write(readme)
    open(os.path.join(WORK, ".gitignore"), "w", encoding="utf-8").write(GITIGNORE)

    run_git(["config", "user.name", "lokesh"], cwd=WORK)
    run_git(["config", "user.email", "lokeshvakkalakula619@gmail.com"], cwd=WORK)
    run_git(["add", "."], cwd=WORK)
    msg = """Add full-stack system design projects with RabbitMQ

- Job Scheduling Platform: Spring Boot + RabbitMQ (priority queues, DLQ,
  delayed retry, fan-out) + Redis (distributed locks, heartbeat) + Angular 17
  dashboard with real-time log streaming (48 Java + 54 Angular files)
- API Orchestration Gateway: Spring Boot + RabbitMQ (topic routing, correlation
  IDs, saga compensation, circuit breaker) + React 18 visual flow builder
  with React Flow and real-time execution tracing (56 Java + 49 React files)"""
    run_git(["commit", "-m", msg], cwd=WORK)
    r = run_git(["ls-files"], cwd=WORK)
    file_count = len([x for x in r.stdout.splitlines() if x.strip()])
    print(f"FILE_COUNT={file_count}")

    status, body = github_post_create(token)
    print(f"CREATE_REPO_HTTP={status}")
    if isinstance(body, dict) and body.get("html_url"):
        print(f"REPO_HTML_URL={body['html_url']}")

    authed = f"https://{USERNAME}:{token}@github.com/{USERNAME}/{REPO_NAME}.git"
    clean = f"https://github.com/{USERNAME}/{REPO_NAME}.git"
    rr = subprocess.run(["git", "remote"], cwd=WORK, capture_output=True, text=True)
    if "origin" in rr.stdout.split():
        run_git(["remote", "set-url", "origin", authed], cwd=WORK)
    else:
        run_git(["remote", "add", "origin", authed], cwd=WORK)
    run_git(["branch", "-M", "main"], cwd=WORK)
    run_git(["push", "-u", "origin", "main"], cwd=WORK)
    run_git(["remote", "set-url", "origin", clean], cwd=WORK)
    print("PUSH_OK=1")
    print(f"FINAL_URL=https://github.com/{USERNAME}/{REPO_NAME}")


if __name__ == "__main__":
    main()
