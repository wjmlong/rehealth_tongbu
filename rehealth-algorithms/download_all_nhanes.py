# -*- coding: utf-8 -*-
"""
Comprehensive NHANES downloader for F3 retrain workstream.

Downloads all 5 NHANES cycles plus daily accelerometer summaries.
Reuses the file list from train/train_v7_wearable.py NHANES_CYCLES so the
V7/V8 data loader can consume the files directly.

Output layout:
  data/nhanes/2011-2012/<FILE>.xpt
  data/nhanes/2013-2014/<FILE>.xpt
  ...
  data/nhanes/2021-2023/<FILE>.xpt
"""

from pathlib import Path

import requests

BASE_URL = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public"
HEADERS = {"User-Agent": "Mozilla/5.0 (ReHealth Research)"}

CYCLES = {
    "2011-2012": {
        "year": "2011",
        "suffix": "G",
        "base_files": [
            "DEMO_G.xpt", "BMX_G.xpt", "BPX_G.xpt", "GLU_G.xpt",
            "TCHOL_G.xpt", "HDL_G.xpt", "TRIGLY_G.xpt", "BPQ_G.xpt",
            "DIQ_G.xpt", "MCQ_G.xpt", "SMQ_G.xpt", "ALQ_G.xpt", "PAQ_G.xpt",
        ],
        "extra_files": ["GHB_G.xpt", "HSCRP_G.xpt", "BIOPRO_G.xpt", "CBC_G.xpt"],
        "accel": "PAXDAY_G.xpt",
    },
    "2013-2014": {
        "year": "2013",
        "suffix": "H",
        "base_files": [
            "DEMO_H.xpt", "BMX_H.xpt", "BPX_H.xpt", "GLU_H.xpt",
            "TCHOL_H.xpt", "HDL_H.xpt", "TRIGLY_H.xpt", "BPQ_H.xpt",
            "DIQ_H.xpt", "MCQ_H.xpt", "SMQ_H.xpt", "ALQ_H.xpt", "PAQ_H.xpt",
        ],
        "extra_files": ["GHB_H.xpt", "HSCRP_H.xpt", "BIOPRO_H.xpt", "CBC_H.xpt"],
        "accel": "PAXDAY_H.xpt",
    },
    "2015-2016": {
        "year": "2015",
        "suffix": "I",
        "base_files": [
            "DEMO_I.xpt", "BMX_I.xpt", "BPX_I.xpt", "GLU_I.xpt",
            "TCHOL_I.xpt", "HDL_I.xpt", "TRIGLY_I.xpt", "BPQ_I.xpt",
            "DIQ_I.xpt", "MCQ_I.xpt", "SMQ_I.xpt", "ALQ_I.xpt", "PAQ_I.xpt",
        ],
        "extra_files": ["GHB_I.xpt", "HSCRP_I.xpt", "BIOPRO_I.xpt", "CBC_I.xpt"],
        "accel": None,
    },
    "2017-2018": {
        "year": "2017",
        "suffix": "J",
        "base_files": [
            "DEMO_J.xpt", "BMX_J.xpt", "BPX_J.xpt", "GLU_J.xpt",
            "TCHOL_J.xpt", "HDL_J.xpt", "TRIGLY_J.xpt", "BPQ_J.xpt",
            "DIQ_J.xpt", "MCQ_J.xpt", "SMQ_J.xpt", "ALQ_J.xpt", "PAQ_J.xpt",
        ],
        "extra_files": ["GHB_J.xpt", "HSCRP_J.xpt", "BIOPRO_J.xpt", "CBC_J.xpt"],
        "accel": "PAXDAY_J.xpt",
    },
    "2021-2023": {
        "year": "2021",
        "suffix": "L",
        "base_files": [
            "DEMO_L.xpt", "BMX_L.xpt", "BPX_L.xpt", "GLU_L.xpt",
            "TCHOL_L.xpt", "HDL_L.xpt", "TRIGLY_L.xpt", "BPQ_L.xpt",
            "DIQ_L.xpt", "MCQ_L.xpt", "SMQ_L.xpt", "ALQ_L.xpt", "PAQ_L.xpt",
        ],
        "extra_files": ["GHB_L.xpt", "HSCRP_L.xpt", "BIOPRO_L.xpt", "CBC_L.xpt"],
        "accel": None,
    },
}


def try_download(url, dest):
    """Returns (ok, message)."""
    try:
        resp = requests.get(url, headers=HEADERS, timeout=180, stream=True)
        if resp.status_code != 200:
            return False, f"HTTP {resp.status_code}"
        ctype = resp.headers.get("Content-Type", "")
        if "html" in ctype.lower():
            first = next(resp.iter_content(chunk_size=8), b"")
            if first.startswith(b"<") or first.startswith(b"\n<!"):
                return False, "got HTML"
            with open(dest, "wb") as f:
                f.write(first)
                for chunk in resp.iter_content(chunk_size=65536):
                    f.write(chunk)
            return True, f"{dest.stat().st_size:,} bytes"
        with open(dest, "wb") as f:
            for chunk in resp.iter_content(chunk_size=65536):
                f.write(chunk)
        return True, f"{dest.stat().st_size:,} bytes"
    except Exception as e:
        if dest.exists():
            dest.unlink()
        return False, f"Error: {e}"


def download_one(cycle_dir, fname, year):
    dest = cycle_dir / fname
    if dest.exists() and dest.stat().st_size > 5000:
        return True, f"exists ({dest.stat().st_size:,} bytes)"

    variants = [fname, fname.upper()]
    for attempt, name in enumerate(set(variants)):
        url = f"{BASE_URL}/{year}/DataFiles/{name}"
        ok, msg = try_download(url, dest)
        if ok:
            return True, msg

    if dest.exists() and dest.stat().st_size > 5000:
        return True, f"{dest.stat().st_size:,} bytes (after retry)"
    return False, "all variants failed"


def main():
    data_dir = Path("data/nhanes")
    data_dir.mkdir(parents=True, exist_ok=True)

    grand_ok, grand_fail = 0, 0
    grand_skip = 0

    for cycle_name, cfg in CYCLES.items():
        cycle_dir = data_dir / cycle_name
        cycle_dir.mkdir(parents=True, exist_ok=True)
        year = cfg["year"]

        all_files = list(cfg["base_files"]) + list(cfg["extra_files"])
        if cfg.get("accel"):
            all_files.append(cfg["accel"])

        print(f"\n{'='*60}")
        print(f"NHANES {cycle_name} → {cycle_dir}")
        print(f"{'='*60}")

        cyc_ok, cyc_fail = 0, 0
        for fname in all_files:
            label = fname.split("_")[0]
            ok, msg = download_one(cycle_dir, fname, year)
            status = "OK" if ok else "FAIL"
            print(f"  [{label:7s}] {fname}: {status} - {msg}")
            if ok:
                cyc_ok += 1
            else:
                cyc_fail += 1

        grand_ok += cyc_ok
        grand_fail += cyc_fail
        print(f"  {cycle_name}: {cyc_ok} ok, {cyc_fail} fail")

    print(f"\n{'='*60}")
    print(f"Grand total: {grand_ok} ok, {grand_fail} fail")
    print(f"{'='*60}")

    if grand_fail > 0:
        print("\nNote: some files failed. Non-core extras (HSCRP/BIOPRO/CBC/GHB)")
        print("may be missing for some cycles due to CDC file naming differences.")
        print("The training pipeline handles missing extras gracefully.")
    return 0 if grand_fail == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
