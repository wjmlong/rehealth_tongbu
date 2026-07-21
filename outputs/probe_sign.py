import hashlib, json, time, urllib.request, datetime

SECRET = "dd05f1c54d63749eda95f9fa6d49v442a"
BASE = "http://127.0.0.1:8080/jeecg-boot"

def sign(params: dict) -> str:
    # fastjson serializes TreeMap<String,String> with NO spaces, keys sorted alphabetically
    s = json.dumps(params, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.md5((s + SECRET).encode("utf-8")).hexdigest().upper()

def ts14():
    return datetime.datetime.now().strftime("%Y%m%d%H%M%S")

# --- probe /sys/sms (register mode, smsmode=1) ---
body = {"mobile": "13800138000", "smsmode": "1"}
hdr_sign = sign(body)
hdr_ts = ts14()
req = urllib.request.Request(
    BASE + "/sys/sms",
    data=json.dumps(body).encode("utf-8"),
    headers={
        "Content-Type": "application/json",
        "X-Sign": hdr_sign,
        "X-Timestamp": hdr_ts,
    },
    method="POST",
)
print("X-Sign:", hdr_sign)
print("X-Timestamp:", hdr_ts)
print("signed-json:", json.dumps(body, sort_keys=True, separators=(",", ":")))
try:
    with urllib.request.urlopen(req, timeout=8) as r:
        print("HTTP", r.status, "->", r.read().decode("utf-8"))
except urllib.error.HTTPError as e:
    print("HTTP", e.code, "->", e.read().decode("utf-8"))
except Exception as e:
    print("ERR", e)
