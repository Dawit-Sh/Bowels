"""Fetches the Android signing key the Expo (EAS) builds used, with the repo's EXPO_TOKEN.

The 2.x app was signed by EAS. Android only installs an update signed with the same key, so
the Kotlin releases must use that keystore. It stays on Expo; this script fetches it on the CI
runner at build time and nothing secret is printed (passwords are masked in the log).

    python eas_keystore.py check   # list the keystores Expo holds, by fingerprint only
    python eas_keystore.py write   # write bowels-release.jks and keystore.properties

The release key's fingerprint is pinned below, so a different key can never be used.
"""

import base64
import json
import os
import sys
import urllib.request

PROJECT_ID = "4748c888-d2f8-43e5-a524-ff9e359cfeee"  # "extra.eas.projectId" from the Expo app.json
PACKAGE = "com.d_ave.bowels"
# SHA-256 of the certificate that signed the v2.1.2 release APK.
EXPECTED_SHA256 = "bf81329e7616b0c1074ed627bb2263813e6907c43f14b3ecbb58e70cee6a0004"

QUERY = """
query BowelsKeystores($appId: String!, $package: String!) {
  app {
    byId(appId: $appId) {
      fullName
      androidAppCredentials(filter: { applicationIdentifier: $package }) {
        applicationIdentifier
        androidAppBuildCredentialsList {
          name
          isDefault
          androidKeystore {
            type
            keyAlias
            keystore
            keystorePassword
            keyPassword
            sha256CertificateFingerprint
          }
        }
      }
    }
  }
}
"""


def normalize(fingerprint):
    return (fingerprint or "").replace(":", "").strip().lower()


def fetch():
    token = os.environ.get("EXPO_TOKEN")
    if not token:
        sys.exit("::error::EXPO_TOKEN is not set.")
    request = urllib.request.Request(
        "https://api.expo.dev/graphql",
        data=json.dumps({"query": QUERY, "variables": {"appId": PROJECT_ID, "package": PACKAGE}}).encode(),
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + token},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        body = json.load(response)
    if body.get("errors"):
        messages = "; ".join(e.get("message", "?") for e in body["errors"])
        sys.exit("::error::Expo API: " + messages)
    app = body["data"]["app"]["byId"]
    keystores = []
    for credentials in app.get("androidAppCredentials") or []:
        for build in credentials.get("androidAppBuildCredentialsList") or []:
            keystore = build.get("androidKeystore")
            if keystore:
                keystores.append((build, keystore))
    return app.get("fullName"), keystores


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "check"
    project, keystores = fetch()
    print(f"Expo project: {project}; keystores found: {len(keystores)}")
    for build, keystore in keystores:
        for secret in (keystore.get("keystorePassword"), keystore.get("keyPassword")):
            if secret:
                print(f"::add-mask::{secret}")
        fingerprint = normalize(keystore.get("sha256CertificateFingerprint"))
        match = "MATCHES v2.1.2" if fingerprint == EXPECTED_SHA256 else "different key"
        print(f"- {build.get('name')!r} default={build.get('isDefault')} type={keystore.get('type')} sha256={fingerprint} -> {match}")

    chosen = next((k for _, k in keystores if normalize(k.get("sha256CertificateFingerprint")) == EXPECTED_SHA256), None)
    if chosen is None:
        sys.exit("::error::Expo has no keystore matching the v2.1.2 signing certificate.")
    if mode == "check":
        print("OK: the release key is available.")
        return

    with open("bowels-release.jks", "wb") as f:
        f.write(base64.b64decode(chosen["keystore"]))
    with open("keystore.properties", "w") as f:
        f.write("storeFile=bowels-release.jks\n")
        f.write(f"storePassword={chosen['keystorePassword']}\n")
        f.write(f"keyAlias={chosen['keyAlias']}\n")
        f.write(f"keyPassword={chosen['keyPassword']}\n")
    print("Wrote bowels-release.jks and keystore.properties.")


if __name__ == "__main__":
    main()
