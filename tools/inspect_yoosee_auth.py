#!/usr/bin/env python3
"""Reproduce a static auth-contract inventory of the supplied Yoosee source ZIP.

Reads archives without extracting them or contacting any endpoint. Emits method
and field names, observed route annotations and hashes, never constant key values
or method bodies. This is an inspection tool, not a working camera integration.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path
from zipfile import ZipFile


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def read_suffix(archive, suffix):
    names = [n for n in archive.namelist() if n.endswith("/" + suffix)]
    if len(names) != 1:
        raise ValueError(f"Expected one {suffix}; found {len(names)}")
    data = archive.read(names[0])
    return names[0], data.decode("utf-8"), hashlib.sha256(data).hexdigest()


def methods(source):
    return re.findall(r"(?ms)^\.method [^\n]+\n.*?^\.end method", source)


def method_name(block):
    return block.splitlines()[0].split("(")[0].split()[-1]


def inspect(source_zip, original_apk=None):
    specs = {
        "legacy_http": ("com/libhttp/http/HttpService.smali", {"login", "getDeviceList"}),
        "iot_http": ("com/tencentcs/iotvideo/accountmgr/HttpInterface.smali",
                     {"accountLogin", "deviceList", "getAccessToken", "getConfigNetToken", "deviceBind"}),
        "login_data": ("com/libhttp/entity/LoginResult$DataBean.smali", set()),
        "login_handler": ("m8/a.smali", {"a"}),
        "iot_registration": ("com/jwkj/c.smali", {"g"}),
        "native_registration": ("com/jwkj/iotvideo/init/IoTVideoInnerInitializer.smali", {"register", "nativeRegister"}),
        "request_signing": ("com/tencentcs/iotvideo/http/interceptor/AddBaseParamsInterceptor.smali", {"addSignatureHeader"}),
        "signature_adapter": ("smali_classes7/ao/c.smali", {"sha1WithBase256"}),
    }
    result = {"kind": "static_evidence_not_live_integration", "source_zip_sha256": digest(source_zip),
              "sources": {}, "http_contracts": [], "flow_markers": {}}
    contents = {}
    with ZipFile(source_zip) as archive:
        for label, (suffix, wanted) in specs.items():
            path, source, sha = read_suffix(archive, suffix)
            contents[label] = source
            result["sources"][label] = {"archive_path": path, "sha256": sha}
            if label in ("legacy_http", "iot_http"):
                for block in methods(source):
                    if method_name(block) not in wanted:
                        continue
                    # These interfaces contain route/parameter annotations only.
                    values = re.findall(r'value = "([A-Za-z0-9_./-]+)"', block)
                    result["http_contracts"].append({
                        "source": label, "method": method_name(block),
                        "parameters": [v for v in values if "/" not in v],
                        "routes": [v for v in values if "/" in v],
                    })
    result["login_response_fields"] = re.findall(
        r"(?m)^\.field private ([A-Za-z0-9_]+):", contents["login_data"])
    # Presence in selected methods is evidence to inspect, not execution proof.
    checks = {
        "login_handler_reads_access_id": ("login_handler", "a", "LoginResult$DataBean;->getAccessId()"),
        "login_handler_reads_access_token": ("login_handler", "a", "LoginResult$DataBean;->getAccessToken()"),
        "registration_reads_account_q": ("iot_registration", "g", "Lyb/a;->q:"),
        "registration_reads_account_r": ("iot_registration", "g", "Lyb/a;->r:"),
        "registration_calls_iot_register": ("iot_registration", "g", "IoTVideoInitializer;->register("),
        "registration_sets_account_access_info": ("iot_registration", "g", "AccountMgr;->setAccessInfo("),
        "native_registration_present": ("native_registration", "nativeRegister", "nativeRegister("),
        "signed_requests_use_account_token": ("request_signing", "addSignatureHeader", "AccountMgr;->getAccessToken()"),
        "signature_delegates_to_p2p_algorithm": ("signature_adapter", "sha1WithBase256", "IP2PAlgorithm;->sha1WithBase256("),
    }
    for name, (label, method, marker) in checks.items():
        selected = [m for m in methods(contents[label]) if method_name(m) == method]
        result["flow_markers"][name] = any(marker in m for m in selected)
    if original_apk:
        markers = ["Users/LoginCheck.ashx", "/openapi/app/user/login/account",
                   "/openapi/app/user/device/listDevice", "/openapi/app/user/reGenUsrAcceccToken",
                   "Lcom/jwkj/iotvideo/init/IoTVideoInnerInitializer;", "sha1WithBase256"]
        found = {m: False for m in markers}
        with ZipFile(original_apk) as archive:
            for name in archive.namelist():
                if not re.fullmatch(r"classes\d*\.dex", name):
                    continue
                data = archive.read(name)
                for marker in markers:
                    found[marker] |= marker.encode("ascii") in data
        result["original_apk"] = {"sha256": digest(original_apk), "dex_string_presence": found,
                                  "limitation": "String presence does not verify execution or source equivalence."}
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_zip", type=Path)
    parser.add_argument("--original-apk", type=Path)
    args = parser.parse_args()
    try:
        result = inspect(args.source_zip, args.original_apk)
    except (ValueError, OSError) as exc:
        parser.error(str(exc))
    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
