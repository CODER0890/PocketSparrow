#!/usr/bin/env bash
set -e

echo "================================================================"
echo "Pocket Sparrow - Complete ML Pipeline Runner"
echo "================================================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "1. Generating synthetic datasets..."
python3 datasets/generate_synthetic_data.py

echo "2. Preparing dataset splits (train / val / test)..."
python3 datasets/prepare_datasets.py

echo "3. Exporting WordPiece vocabulary & tokenizer config..."
python3 export/export_vocab.py

echo "4. Validating fine-tuning pipeline..."
python3 train/fine_tune.py --epochs 3 --batch_size 16

echo "5. Running unified INT8 quantization (ONNX + TFLite)..."
python3 export/quantize_int8.py

echo "6. Auditing models against size & air-gap SLA..."
python3 export/verify_models.py

echo "7. Running sub-40ms inference benchmark..."
python3 benchmarks/benchmark_inference.py

echo "================================================================"
echo "[SUCCESS] Complete ML Pipeline executed and verified!"
echo "================================================================"
