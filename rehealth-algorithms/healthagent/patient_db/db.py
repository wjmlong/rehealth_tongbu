# encoding: utf-8
"""
Patient Database Module for HealthAgent
SQLite-based patient record management with full medical indicators
"""
import sqlite3
import json
from datetime import datetime
from pathlib import Path

DB_PATH = Path(r"D:\deer-flow-main\backend\data\patients.db")

def _get_conn():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = _get_conn()
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS patients (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            age INTEGER,
            gender TEXT DEFAULT '未知',
            bmi REAL DEFAULT 22.0,
            height REAL DEFAULT 0.0,
            weight REAL DEFAULT 0.0,
            stress_level TEXT DEFAULT 'medium',
            sleep_hours REAL DEFAULT 7.0,
            primary_issue TEXT DEFAULT '',
            medical_history TEXT DEFAULT '',
            systolic_bp INTEGER DEFAULT 120,
            diastolic_bp INTEGER DEFAULT 80,
            fasting_glucose REAL DEFAULT 5.0,
            postprandial_glucose REAL DEFAULT 7.0,
            created_at TEXT,
            updated_at TEXT
        );
        CREATE TABLE IF NOT EXISTS daily_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER NOT NULL,
            record_date TEXT NOT NULL,
            systolic_bp INTEGER,
            diastolic_bp INTEGER,
            fasting_glucose REAL,
            postprandial_glucose REAL,
            weight REAL,
            bmi REAL,
            steps INTEGER,
            exercise_minutes INTEGER,
            mood_score INTEGER,
            sleep_hours REAL,
            notes TEXT DEFAULT '',
            FOREIGN KEY(patient_id) REFERENCES patients(id)
        );
        CREATE TABLE IF NOT EXISTS simulations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER,
            run_at TEXT,
            days INTEGER,
            params TEXT,
            result TEXT,
            FOREIGN KEY(patient_id) REFERENCES patients(id)
        );
    """)
    conn.commit()
    conn.close()

def add_patient(name, age, gender="未知", bmi=22.0, stress_level="medium",
                sleep_hours=7.0, primary_issue="", medical_history="",
                height=0.0, weight=0.0,
                systolic_bp=120, diastolic_bp=80,
                fasting_glucose=5.0, postprandial_glucose=7.0) -> dict:
    init_db()
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    conn = _get_conn()
    cur = conn.execute(
        """INSERT INTO patients
        (name,age,gender,bmi,height,weight,stress_level,sleep_hours,
         primary_issue,medical_history,systolic_bp,diastolic_bp,
         fasting_glucose,postprandial_glucose,created_at,updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
        (name, age, gender, bmi, height, weight, stress_level, sleep_hours,
         primary_issue, medical_history, systolic_bp, diastolic_bp,
         fasting_glucose, postprandial_glucose, now, now)
    )
    conn.commit()
    pid = cur.lastrowid
    conn.close()
    return {"status": "ok", "patient_id": pid, "message": f"患者 {name} 档案已创建，ID={pid}"}

def list_patients() -> list:
    init_db()
    conn = _get_conn()
    rows = conn.execute(
        "SELECT id,name,age,gender,primary_issue,systolic_bp,diastolic_bp,fasting_glucose,created_at FROM patients ORDER BY id DESC"
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]

def get_patient(patient_id: int) -> dict:
    init_db()
    conn = _get_conn()
    row = conn.execute("SELECT * FROM patients WHERE id=?", (patient_id,)).fetchone()
    conn.close()
    if row is None:
        return {"error": f"找不到 ID={patient_id} 的患者"}
    return dict(row)

def add_daily_record(patient_id: int, record_date: str = None,
                     systolic_bp: int = None, diastolic_bp: int = None,
                     fasting_glucose: float = None, postprandial_glucose: float = None,
                     weight: float = None, bmi: float = None,
                     steps: int = None, exercise_minutes: int = None,
                     mood_score: int = None, sleep_hours: float = None,
                     notes: str = "") -> dict:
    init_db()
    if record_date is None:
        record_date = datetime.now().strftime("%Y-%m-%d")
    conn = _get_conn()
    conn.execute(
        """INSERT INTO daily_records
        (patient_id,record_date,systolic_bp,diastolic_bp,fasting_glucose,
         postprandial_glucose,weight,bmi,steps,exercise_minutes,mood_score,sleep_hours,notes)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""",
        (patient_id, record_date, systolic_bp, diastolic_bp, fasting_glucose,
         postprandial_glucose, weight, bmi, steps, exercise_minutes,
         mood_score, sleep_hours, notes)
    )
    conn.commit()
    conn.close()
    return {"status": "ok", "message": f"患者ID={patient_id}的{record_date}健康记录已保存"}

def get_daily_records(patient_id: int, days: int = 30) -> list:
    init_db()
    conn = _get_conn()
    rows = conn.execute(
        """SELECT * FROM daily_records WHERE patient_id=?
        ORDER BY record_date DESC LIMIT ?""",
        (patient_id, days)
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]

def get_health_trend(patient_id: int) -> dict:
    init_db()
    records = get_daily_records(patient_id, days=30)
    if not records:
        return {"error": "暂无健康记录数据"}
    bp_records = [r for r in records if r["systolic_bp"]]
    glucose_records = [r for r in records if r["fasting_glucose"]]
    weight_records = [r for r in records if r["weight"]]
    steps_records = [r for r in records if r["steps"]]
    mood_records = [r for r in records if r["mood_score"]]
    trend = {"patient_id": patient_id, "record_count": len(records)}
    if bp_records:
        trend["blood_pressure"] = {
            "latest": f"{bp_records[0]['systolic_bp']}/{bp_records[0]['diastolic_bp']} mmHg",
            "avg_systolic": round(sum(r["systolic_bp"] for r in bp_records) / len(bp_records), 1),
            "avg_diastolic": round(sum(r["diastolic_bp"] for r in bp_records) / len(bp_records), 1),
            "status": _bp_status(bp_records[0]["systolic_bp"], bp_records[0]["diastolic_bp"])
        }
    if glucose_records:
        trend["glucose"] = {
            "latest_fasting": f"{glucose_records[0]['fasting_glucose']} mmol/L",
            "avg_fasting": round(sum(r["fasting_glucose"] for r in glucose_records) / len(glucose_records), 2),
            "status": _glucose_status(glucose_records[0]["fasting_glucose"])
        }
    if weight_records:
        trend["weight"] = {
            "latest": f"{weight_records[0]['weight']} kg",
            "change": round(weight_records[0]["weight"] - weight_records[-1]["weight"], 1)
        }
    if steps_records:
        trend["activity"] = {
            "avg_steps": round(sum(r["steps"] for r in steps_records) / len(steps_records)),
            "status": "达标" if sum(r["steps"] for r in steps_records)/len(steps_records) >= 8000 else "不足"
        }
    if mood_records:
        trend["mood"] = {
            "avg_score": round(sum(r["mood_score"] for r in mood_records) / len(mood_records), 1),
            "status": _mood_status(sum(r["mood_score"] for r in mood_records) / len(mood_records))
        }
    return trend

def _bp_status(systolic, diastolic):
    if systolic < 120 and diastolic < 80: return "正常"
    if systolic < 130 and diastolic < 80: return "正常偏高"
    if systolic < 140 or diastolic < 90: return "高血压1级"
    return "高血压2级"

def _glucose_status(fasting):
    if fasting < 6.1: return "正常"
    if fasting < 7.0: return "空腹血糖受损"
    return "糖尿病范围"

def _mood_status(score):
    if score >= 8: return "良好"
    if score >= 6: return "一般"
    if score >= 4: return "较差"
    return "很差"

def save_simulation(patient_id: int, days: int, params: dict, result: dict) -> dict:
    init_db()
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    conn = _get_conn()
    conn.execute(
        "INSERT INTO simulations (patient_id,run_at,days,params,result) VALUES (?,?,?,?,?)",
        (patient_id, now, days, json.dumps(params, ensure_ascii=False), json.dumps(result, ensure_ascii=False))
    )
    conn.commit()
    conn.close()
    return {"status": "ok", "message": f"模拟结果已保存至患者ID={patient_id}的档案"}

def get_patient_history(patient_id: int) -> list:
    init_db()
    conn = _get_conn()
    rows = conn.execute(
        "SELECT id,run_at,days,params,result FROM simulations WHERE patient_id=? ORDER BY run_at DESC",
        (patient_id,)
    ).fetchall()
    conn.close()
    history = []
    for r in rows:
        item = dict(r)
        item["params"] = json.loads(item["params"])
        item["result"] = json.loads(item["result"])
        history.append(item)
    return history
