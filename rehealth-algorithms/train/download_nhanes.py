# -*- coding: utf-8 -*-
"""
下载多个 NHANES 周期的 XPT 数据文件 (CDC 公开数据)

支持周期:
  - 2015-2016 (后缀 _I) — 你已有模型的训练数据
  - 2017-2018 (后缀 _J) — 额外训练数据
  - 2021-2023 (后缀 _L) — 最新数据 (注意: 血压文件改为 BPXO)

Usage:
    python train/download_nhanes.py                          # 下载全部3个周期
    python train/download_nhanes.py --cycles 2017-2018       # 只下载 2017-2018
    python train/download_nhanes.py --cycles 2017-2018 2021-2023  # 下载两个周期
"""

import argparse
import sys
from pathlib import Path

import requests

BASE_URL = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
}

# 每个周期的文件映射: 标准名 → 实际CDC文件名
# 标准名用于统一识别，实际文件名因周期而异
CYCLES = {
    "2015-2016": {
        "year": "2015",
        "files": {
            "DEMO": "DEMO_I.xpt",
            "BMX": "BMX_I.xpt",
            "BPX": "BPX_I.xpt",
            "BPQ": "BPQ_I.xpt",
            "GLU": "GLU_I.xpt",
            "TCHOL": "TCHOL_I.xpt",
            "HDL": "HDL_I.xpt",
            "TRIGLY": "TRIGLY_I.xpt",
            "SMQ": "SMQ_I.xpt",
            "ALQ": "ALQ_I.xpt",
            "DIQ": "DIQ_I.xpt",
            "PAQ": "PAQ_I.xpt",
            "MCQ": "MCQ_I.xpt",
        },
    },
    "2017-2018": {
        "year": "2017",
        "files": {
            "DEMO": "DEMO_J.xpt",
            "BMX": "BMX_J.xpt",
            "BPX": "BPX_J.xpt",
            "BPQ": "BPQ_J.xpt",
            "GLU": "GLU_J.xpt",
            "TCHOL": "TCHOL_J.xpt",
            "HDL": "HDL_J.xpt",
            "TRIGLY": "TRIGLY_J.xpt",
            "SMQ": "SMQ_J.xpt",
            "ALQ": "ALQ_J.xpt",
            "DIQ": "DIQ_J.xpt",
            "PAQ": "PAQ_J.xpt",
            "MCQ": "MCQ_J.xpt",
        },
    },
    "2021-2023": {
        "year": "2021",
        "files": {
            "DEMO": "DEMO_L.xpt",
            "BMX": "BMX_L.xpt",
            "BPX": "BPXO_L.xpt",   # 注: 2021-2023 血压改为 oscillometric
            "BPQ": "BPQ_L.xpt",
            "GLU": "GLU_L.xpt",
            "TCHOL": "TCHOL_L.xpt",
            "HDL": "HDL_L.xpt",
            "TRIGLY": "TRIGLY_L.xpt",
            "SMQ": "SMQ_L.xpt",
            "ALQ": "ALQ_L.xpt",
            "DIQ": "DIQ_L.xpt",
            "PAQ": "PAQ_L.xpt",
            "MCQ": "MCQ_L.xpt",
        },
    },
}


def download_cycle(cycle_name: str, output_base: str = "data/nhanes"):
    cycle = CYCLES[cycle_name]
    year = cycle["year"]
    out = Path(output_base) / cycle_name
    out.mkdir(parents=True, exist_ok=True)

    print(f"\n{'='*50}")
    print(f"NHANES {cycle_name} → {out}")
    print(f"{'='*50}")

    success = 0
    for std_name, fname in cycle["files"].items():
        dest = out / fname
        if dest.exists() and dest.stat().st_size > 10000:
            print(f"  [skip] {fname} ({dest.stat().st_size // 1024} KB)")
            success += 1
            continue

        url = f"{BASE_URL}/{year}/DataFiles/{fname}"
        print(f"  [{std_name:6s}] {fname} ...", end=" ", flush=True)
        try:
            resp = requests.get(url, headers=HEADERS, timeout=120, stream=True)
            resp.raise_for_status()

            content_type = resp.headers.get("Content-Type", "")
            if "html" in content_type.lower():
                print(f"SKIP (got HTML)")
                continue

            with open(dest, "wb") as f:
                for chunk in resp.iter_content(chunk_size=65536):
                    f.write(chunk)

            size_kb = dest.stat().st_size // 1024
            print(f"OK ({size_kb} KB)")
            success += 1
        except Exception as e:
            print(f"FAIL: {e}")
            if dest.exists():
                dest.unlink()

    total = len(cycle["files"])
    print(f"\n  {success}/{total} files downloaded for {cycle_name}")
    return success == total


def main():
    parser = argparse.ArgumentParser(description="Download NHANES XPT files (multi-cycle)")
    parser.add_argument("--output-dir", default="data/nhanes")
    parser.add_argument("--cycles", nargs="*", default=list(CYCLES.keys()),
                        choices=list(CYCLES.keys()),
                        help="Which cycles to download (default: all)")
    args = parser.parse_args()

    print("NHANES Multi-Cycle Downloader")
    print(f"Cycles: {args.cycles}")

    all_ok = True
    for cycle_name in args.cycles:
        ok = download_cycle(cycle_name, args.output_dir)
        all_ok = all_ok and ok

    print("\n" + "=" * 50)
    if all_ok:
        print("All downloads complete!")
    else:
        print("Some files failed — check output above")
    print("=" * 50)


if __name__ == "__main__":
    main()
