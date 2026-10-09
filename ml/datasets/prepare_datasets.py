#!/usr/bin/env python3
"""
Pocket Sparrow - Dataset Preparation & Split Generator
Prepares, cleans, and partitions safe public/simulated datasets for phishing URLs,
SMS scams, and benign messages into train/val/test splits.
"""

import json
import random
from pathlib import Path

def prepare_splits(input_json_path: Path, output_dir: Path):
    if not input_json_path.exists():
        from generate_synthetic_data import main as gen_data
        gen_data()

    with open(input_json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # Deterministic shuffle for reproducibility
    random.seed(42)
    random.shuffle(data)

    total = len(data)
    train_end = int(total * 0.70)
    val_end = int(total * 0.85)

    train_data = data[:train_end]
    val_data = data[train_end:val_end]
    test_data = data[val_end:]

    output_dir.mkdir(parents=True, exist_ok=True)

    for split_name, split_records in [("train", train_data), ("val", val_data), ("test", test_data)]:
        split_path = output_dir / f"{split_name}.json"
        with open(split_path, "w", encoding="utf-8") as f:
            json.dump(split_records, f, indent=2, ensure_ascii=False)

        # Count labels
        counts = {0: 0, 1: 0, 2: 0}
        for r in split_records:
            counts[r["label"]] += 1
        print(f"[{split_name.upper()}] Total: {len(split_records)} | SAFE(0): {counts[0]}, SUSP(1): {counts[1]}, MAL(2): {counts[2]} -> {split_path.name}")

    print("[SUCCESS] Dataset splits generated successfully.")

def main():
    base_dir = Path(__file__).resolve().parent
    input_file = base_dir / "sample_threats.json"
    splits_dir = base_dir / "splits"
    prepare_splits(input_file, splits_dir)

if __name__ == "__main__":
    main()
