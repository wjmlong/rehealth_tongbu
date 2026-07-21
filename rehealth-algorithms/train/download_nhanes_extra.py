# -*- coding: utf-8 -*-
"""
Download additional NHANES biomarker files for enhanced CVD prediction.

New files:
  - GHB (Glycohemoglobin = HbA1c)
  - HSCRP (High-sensitivity CRP)
  - BIOPRO (Biochemistry Profile: creatinine, albumin, uric acid, etc.)
  - CBC (Complete Blood Count: WBC, RBC, etc.)
  - INS (Insulin)
  - FERTIN (Ferritin)
"""

import sys
from pathlib import Path
import requests

BASE_URL = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public"

EXTRA_FILES = {
    "2015-2016": {
        "year": "2015",
        "files": [
            "GHB_I.xpt",      # HbA1c
            "HSCRP_I.xpt",    # CRP
            "BIOPRO_I.xpt",   # Biochemistry (creatinine, albumin, uric acid, BUN, etc.)
            "CBC_I.xpt",      # Complete Blood Count
        ],
    },
    "2017-2018": {
        "year": "2017",
        "files": [
            "GHB_J.xpt",
            "HSCRP_J.xpt",
            "BIOPRO_J.xpt",
            "CBC_J.xpt",
        ],
    },
    "2021-2023": {
        "year": "2021",
        "files": [
            "GHB_L.xpt",
            "HSCRP_L.xpt",
            "BIOPRO_L.xpt",
            "CBC_L.xpt",
        ],
    },
}


def download_file(url, dest):
    headers = {"User-Agent": "Mozilla/5.0 (ReHealth Research)"}
    resp = requests.get(url, headers=headers, timeout=60)
    if resp.status_code == 200 and not resp.text.startswith("<!"):
        dest.write_bytes(resp.content)
        return True
    return False


def main():
    data_dir = Path("data/nhanes")

    for cycle, cfg in EXTRA_FILES.items():
        cycle_dir = data_dir / cycle
        if not cycle_dir.exists():
            print(f"[skip] {cycle}: directory not found")
            continue

        print(f"\n[{cycle}]")
        for fname in cfg["files"]:
            dest = cycle_dir / fname
            if dest.exists():
                print(f"  {fname}: already exists ({dest.stat().st_size:,} bytes)")
                continue

            url = f"{BASE_URL}/{cfg['year']}/DataFiles/{fname}"
            print(f"  {fname}: downloading from {url}...")
            if download_file(url, dest):
                print(f"    OK ({dest.stat().st_size:,} bytes)")
            else:
                # Try uppercase
                url2 = f"{BASE_URL}/{cfg['year']}/DataFiles/{fname.upper()}"
                print(f"    Trying {url2}...")
                if download_file(url2, dest):
                    print(f"    OK ({dest.stat().st_size:,} bytes)")
                else:
                    print(f"    FAILED")

    print("\nDone!")


if __name__ == "__main__":
    main()
