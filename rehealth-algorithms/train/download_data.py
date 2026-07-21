# -*- coding: utf-8 -*-
"""Download Kaggle CVD 70K + NHANES accelerometer data."""

from pathlib import Path
import requests
import zipfile
import io

HEADERS = {"User-Agent": "Mozilla/5.0 (ReHealth Research)"}


def download_file(url, dest, desc=""):
    """Download a file with progress."""
    try:
        resp = requests.get(url, headers=HEADERS, timeout=120, stream=True)
        if resp.status_code == 200 and not resp.text[:5].startswith("<!"):
            dest.write_bytes(resp.content)
            size = dest.stat().st_size
            print(f"  {desc}: OK ({size:,} bytes)")
            return True
        else:
            print(f"  {desc}: HTTP {resp.status_code}")
    except Exception as e:
        print(f"  {desc}: Error - {e}")
    return False


def download_nhanes_accel():
    """Download NHANES Physical Activity Monitor daily summary data."""
    data_dir = Path("data/nhanes")

    # PAXDAY = daily summary, PAXHR = hourly summary
    # These are much smaller than minute-level PAXMIN files
    files = {
        "2011-2012": [
            ("PAXDAY_G.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2011/DataFiles/PAXDAY_G.xpt"),
            ("PAXHR_G.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2011/DataFiles/PAXHR_G.xpt"),
        ],
        "2013-2014": [
            ("PAXDAY_H.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2013/DataFiles/PAXDAY_H.xpt"),
            ("PAXHR_H.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2013/DataFiles/PAXHR_H.xpt"),
        ],
    }

    # Also try 2015-2016 and 2017-2018 if they have PAM data
    files["2015-2016"] = [
        ("PAXDAY_I.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2015/DataFiles/PAXDAY_I.xpt"),
        ("PAXHR_I.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2015/DataFiles/PAXHR_I.xpt"),
    ]
    files["2017-2020"] = [
        ("PAXDAY_J.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2017/DataFiles/PAXDAY_J.xpt"),
        ("PAXHR_J.xpt", "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2017/DataFiles/PAXHR_J.xpt"),
    ]

    print("\n[NHANES Accelerometer Data]")
    for cycle, file_list in files.items():
        cycle_dir = data_dir / cycle
        cycle_dir.mkdir(parents=True, exist_ok=True)
        print(f"\n  {cycle}:")
        for fname, url in file_list:
            dest = cycle_dir / fname
            if dest.exists() and dest.stat().st_size > 1000:
                print(f"    {fname}: exists ({dest.stat().st_size:,} bytes)")
                continue
            download_file(url, dest, fname)


def download_kaggle_cvd():
    """Download Kaggle Cardiovascular Disease dataset.

    The dataset is from: kaggle.com/datasets/sulianova/cardiovascular-disease-dataset
    We try multiple mirror/alternative sources.
    """
    data_dir = Path("data/kaggle")
    data_dir.mkdir(parents=True, exist_ok=True)
    dest = data_dir / "cardio_train.csv"

    if dest.exists() and dest.stat().st_size > 100000:
        print(f"\n[Kaggle CVD] Already exists: {dest} ({dest.stat().st_size:,} bytes)")
        return True

    print("\n[Kaggle CVD 70K Dataset]")

    # Try UCI alternative first - Heart Disease dataset (smaller but immediate)
    # The 70K dataset needs Kaggle auth, so let's create it via alternative approach
    urls = [
        # Direct Kaggle download (may need auth)
        "https://storage.googleapis.com/kaggle-data-sets/216167/484932/bundle/archive.zip",
        # Alternative mirrors
        "https://raw.githubusercontent.com/sharmaroshan/Cardiovascular-Disease-dataset/master/cardio_train.csv",
    ]

    for url in urls:
        print(f"  Trying: {url[:60]}...")
        try:
            resp = requests.get(url, headers=HEADERS, timeout=60)
            if resp.status_code == 200:
                if url.endswith(".zip"):
                    with zipfile.ZipFile(io.BytesIO(resp.content)) as zf:
                        for name in zf.namelist():
                            if name.endswith(".csv"):
                                zf.extract(name, data_dir)
                                extracted = data_dir / name
                                if extracted != dest:
                                    extracted.rename(dest)
                                print(f"  OK: extracted {dest} ({dest.stat().st_size:,} bytes)")
                                return True
                elif ";" in resp.text[:200] or "," in resp.text[:200]:
                    dest.write_text(resp.text)
                    print(f"  OK: {dest} ({dest.stat().st_size:,} bytes)")
                    return True
        except Exception as e:
            print(f"  Failed: {e}")

    print("  Could not auto-download. Will generate download script.")
    # Write a helper script for manual download
    script = data_dir / "download_instructions.txt"
    script.write_text(
        "Kaggle CVD 70K dataset download:\n"
        "1. Go to: https://www.kaggle.com/datasets/sulianova/cardiovascular-disease-dataset\n"
        "2. Click 'Download' (requires free Kaggle account)\n"
        "3. Extract cardio_train.csv to this directory\n"
        "\nAlternatively, run:\n"
        "  pip install kaggle\n"
        "  kaggle datasets download -d sulianova/cardiovascular-disease-dataset\n"
    )
    return False


if __name__ == "__main__":
    download_kaggle_cvd()
    download_nhanes_accel()
    print("\nDone!")
