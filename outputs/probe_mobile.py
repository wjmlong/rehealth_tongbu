import json, urllib.request, urllib.error

BASE = "http://127.0.0.1:8080"
def post(path, body, token=None):
    url = BASE + path
    data = json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("X-Access-Token", token)
    try:
        r = urllib.request.urlopen(req, timeout=10)
        return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

def get(path, token=None):
    url = BASE + path
    req = urllib.request.Request(url, method="GET")
    if token:
        req.add_header("X-Access-Token", token)
    try:
        r = urllib.request.urlopen(req, timeout=10)
        return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

st, txt = post("/jeecg-boot/sys/mLogin", {"username": "13507007999", "password": "Test@123456"})
print("mLogin:", st)
token = None
try:
    token = json.loads(txt)["result"]["token"]
    print("  token:", token[:20], "...")
except Exception as e:
    print("  parse fail:", txt[:200])

if token:
    for ep in ["/jeecg-boot/rehealth/mobile/health",
               "/jeecg-boot/rehealth/mobile/config",
               "/jeecg-boot/rehealth/mobile/risk/latest",
               "/jeecg-boot/rehealth/mobile/interventions/today"]:
        st, txt = get(ep, token)
        print(f"GET {ep}: {st}")
        print("   ", txt[:300].replace("\n", " "))
    vec = {"requestId": "probe1", "age": 45, "gender": "male", "systolic": 121,
           "diastolic": 78, "heartRate": 72, "spo2": 98, "bmi": 24.3, "smoking": False,
           "drinking": False, "diabetesHistory": False, "hypertensionHistory": True,
           "familyHistory": True, "avgSteps": 7000, "avgSleepMinutes": 450,
           "restingHr": 72, "stress": 35}
    st, txt = post("/jeecg-boot/rehealth/mobile/features/evaluate", vec, token)
    print(f"POST features/evaluate: {st}")
    print("   ", txt[:500].replace("\n", " "))
else:
    print("NO TOKEN - cannot probe authed endpoints")
