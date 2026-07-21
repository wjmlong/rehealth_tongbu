import hashlib, json, time, urllib.request, datetime

SECRET = "dd05f1c54d63749eda95f9fa6d49v442a"
BASE = "http://127.0.0.1:8080/jeecg-boot"

def md5(s: str) -> str:
    return hashlib.md5(s.encode("utf-8")).hexdigest().upper()

def ts14():
    return datetime.datetime.now().strftime("%Y%m%d%H%M%S")

def ts_ms():
    return str(int(time.time() * 1000))

body = {"mobile": "13800138000", "smsmode": "1"}

# Candidate signed-strings (the JSON that is MD5'd with the secret)
candidates = {
    "no_space_sorted": json.dumps(body, sort_keys=True, separators=(",", ":")),
    "space_sorted":    json.dumps(body, sort_keys=True, indent=None),
    "no_space_insert": json.dumps(body, separators=(",", ":")),  # insertion order mobile,smsmode
    "empty":           "{}",
}

def call(signed_json, ts, label):
    hdr_sign = md5(signed_json + SECRET)
    req = urllib.request.Request(
        BASE + "/sys/sms",
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "X-Sign": hdr_sign, "X-Timestamp": ts},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=8) as r:
            resp = r.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        resp = e.read().decode("utf-8")
    print(f"[{label}] signed_json={signed_json!r}")
    print(f"        X-Sign={hdr_sign}  -> {resp}\n")

for label, sj in candidates.items():
    call(sj, ts14(), label)

# also try unix-ms timestamp with the no_space_sorted candidate
call(candidates["no_space_sorted"], ts_ms(), "no_space_sorted__unixms")
