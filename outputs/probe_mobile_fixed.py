import json, urllib.request, urllib.error

BASE = "http://127.0.0.1:8080"

def post(path, body, token=None):
    data = json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("X-Access-Token", token)
    try:
        r = urllib.request.urlopen(req, timeout=15)
        return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

def get(path, token=None):
    req = urllib.request.Request(BASE + path, method="GET")
    if token:
        req.add_header("X-Access-Token", token)
    try:
        r = urllib.request.urlopen(req, timeout=15)
        return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

# 1) Login
st, txt = post("/jeecg-boot/sys/mLogin", {"username": "13507007999", "password": "Test@123456"})
print("mLogin:", st)
token = None
try:
    token = json.loads(txt)["result"]["token"]
    print("  token:", token[:20], "...")
except Exception as e:
    print("  parse fail:", txt[:300])

if not token:
    raise SystemExit("NO TOKEN - cannot probe authed endpoints")

# 2) App-shaped feature vector (exactly what CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest emits)
feature_quality = {
    "age": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
    "gender": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
    "bmi": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
    "sbp": {"status": "VALID", "source": "REAL_DEVICE", "observedAt": 1783540800000, "reason": "latest ring blood pressure"},
    "dbp": {"status": "VALID", "source": "REAL_DEVICE", "observedAt": 1783540800000, "reason": "latest ring blood pressure"},
    "fasting_glucose": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
    "total_cholesterol": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
    "ldl": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
    "hdl": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
    "triglycerides": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
    "exercise_days": {"status": "VALID", "source": "DERIVED", "observedAt": 1783540800000, "reason": "7-day activity summary"},
    "smoking": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
    "drinking": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
    "diabetes_history": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
    "hypertension_history": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
    "family_history": {"status": "VALID", "source": "USER_REPORTED", "observedAt": 1783540800000, "reason": "profile"},
}
feature_vector = {
    "age": 52, "gender": 1, "bmi": 27.4, "sbp": 136.0, "dbp": 86.0,
    "fasting_glucose": None, "total_cholesterol": None, "ldl": None, "hdl": None, "triglycerides": None,
    "exercise_days": 3, "smoking": 0, "drinking": 0, "diabetes_history": 0,
    "hypertension_history": 1, "family_history": 1,
    "featureQuality": feature_quality,
}
body = {"featureVector": feature_vector, "requestId": "probe_fixed_1"}

st, txt = post("/jeecg-boot/rehealth/mobile/features/evaluate", body, token)
print(f"POST features/evaluate (App-shaped): {st}")
print("   ", txt[:900].replace("\n", " "))

# 3) risk/latest & interventions/today
for ep in ["/jeecg-boot/rehealth/mobile/risk/latest",
           "/jeecg-boot/rehealth/mobile/interventions/today"]:
    st, txt = get(ep, token)
    print(f"GET {ep}: {st}  {txt[:300].replace(chr(10), ' ')}")
