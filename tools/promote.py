"""Promote the newest release on one Google Play track to another (default: internal -> production).

No rebuild and no re-upload: the same versionCode that testers already used is assigned to the
target track through the Play Developer API (edits.tracks). Used by .github/workflows/promote.yml.

Env: PLAY_SERVICE_ACCOUNT_JSON (service account key, JSON text)
Args: --package mk.vesti.app --from internal --to production --rollout 0.2
"""
import argparse
import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--package", required=True)
    p.add_argument("--from", dest="src", default="internal")
    p.add_argument("--to", dest="dst", default="production")
    p.add_argument("--rollout", type=float, default=1.0, help="share of users, 0 < x <= 1")
    a = p.parse_args()
    if not 0 < a.rollout <= 1:
        sys.exit("--rollout must be in (0, 1]")

    creds = service_account.Credentials.from_service_account_info(
        json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"]),
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    edits = build("androidpublisher", "v3", credentials=creds, cache_discovery=False).edits()

    edit_id = edits.insert(packageName=a.package, body={}).execute()["id"]
    src = edits.tracks().get(packageName=a.package, editId=edit_id, track=a.src).execute()

    # Newest release on the source track that actually has a build in it.
    releases = [r for r in src.get("releases", []) if r.get("versionCodes")]
    if not releases:
        sys.exit(f"No release with a build on the '{a.src}' track")
    latest = max(releases, key=lambda r: max(int(v) for v in r["versionCodes"]))

    release = {
        "name": latest.get("name"),
        "versionCodes": latest["versionCodes"],
        "releaseNotes": latest.get("releaseNotes", []),
        "status": "completed" if a.rollout == 1 else "inProgress",
    }
    if a.rollout < 1:
        release["userFraction"] = a.rollout

    edits.tracks().update(
        packageName=a.package, editId=edit_id, track=a.dst,
        body={"track": a.dst, "releases": [release]},
    ).execute()
    edits.commit(packageName=a.package, editId=edit_id).execute()

    print(f"Promoted {release['name']} (versionCodes {release['versionCodes']}) "
          f"{a.src} -> {a.dst}, rollout {a.rollout:.0%}. Google review may still apply.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
