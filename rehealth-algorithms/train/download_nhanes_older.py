# -*- coding: utf-8 -*-
"""Download NHANES 2011-2012 and 2013-2014 cycles (base + biomarker files)."""

from pathlib import Path
import requests

BASE_URL = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public"

CYCLES = {
    "2011-2012": {
        "year": "2011",
        "suffix": "G",
        "files": [
            "DEMO_G.xpt", "BMX_G.xpt", "BPX_G.xpt", "GLU_G.xpt",
            "TCHOL_G.xpt", "HDL_G.xpt", "TRIGLY_G.xpt",
            "BPQ_G.xpt", "DIQ_G.xpt", "MCQ_G.xpt",
            "SMQ_G.xpt", "ALQ_G.xpt", "PAQ_G.xpt",
            "GHB_G.xpt", "HSCRP_G.xpt", "BIOPRO_G.xpt", "CBC_G.xpt",
        ],
    },
    "2013-2014": {
        "year": "2013",
        "suffix": "H",
        "files": [
            "DEMO_H.xpt", "BMX_H.xpt", "BPX_H.xpt", "GLU_H.xpt",
            "TCHOL_H.xpt", "HDL_H.xpt", "TRIGLY_H.xpt",
            "BPQ_H.xpt", "DIQ_H.xpt", "MCQ_H.xpt",
            "SMQ_H.xpt", "ALQ_H.xpt", "PAQ_H.xpt",
            "GHB_H.xpt", "HSCRP_H.xpt", "BIOPRO_H.xpt", "CBC_H.xpt",
        ],
    },
}


def download_file(url, dest):
    headers = {"User-Agent": "Mozilla/5.0 (ReHealth Research)"}
    try:
        resp = requests.get(url, headers=headers, timeout=60)
        if resp.status_code == 200 and not resp.text.startswith("<!"):
            dest.write_bytes(resp.content)
            return True
    except Exception as e:
        print(f"    Error: {e}")
    return False


def main():
    data_dir = Path("data/nhanes")

    for cycle, cfg in CYCLES.items():
        cycle_dir = data_dir / cycle
        cycle_dir.mkdir(parents=True, exist_ok=True)

        print(f"\n[{cycle}]")
        for fname in cfg["files"]:
            dest = cycle_dir / fname
            if dest.exists():
                print(f"  {fname}: exists ({dest.stat().st_size:,} bytes)")
                continue

            url = f"{BASE_URL}/{cfg['year']}/DataFiles/{fname}"
            print(f"  {fname}: downloading...", end=" ", flush=True)
            if download_file(url, dest):
                print(f"OK ({dest.stat().st_size:,} bytes)")
            else:
                url2 = f"{BASE_URL}/{cfg['year']}/DataFiles/{fname.upper()}"
                print(f"retry {url2}...", end=" ", flush=True)
                if download_file(url2, dest):
                    print(f"OK ({dest.stat().st_size:,} bytes)")
                else:
                    print("FAILED")

    print("\nDone!")


if __name__ == "__main__":
    main()
