#!/usr/bin/env python3
"""
Pocket Sparrow - Unified INT8 Quantization Script
Converts trained PyTorch MobileBERT/DistilBERT to both:
1. Desktop: INT8 ONNX (Target size: ~32MB, latency < 40ms)
2. Android: INT8 TFLite FlatBuffer with PTQ (Target size: ~33MB, latency < 40ms)
"""

import sys
import argparse
from pathlib import Path

def resolve_models_dir(default_str: str) -> Path:
    p = Path(default_str)
    if p.exists():
        return p
    # If run from ml/
    ml_models = Path(__file__).resolve().parent.parent / "models"
    if ml_models.exists():
        return ml_models
    return p

def main():
    default_models = str(resolve_models_dir("ml/models"))

    parser = argparse.ArgumentParser(description="Unified INT8 Quantization for Pocket Sparrow")
    parser.add_argument("--checkpoint", type=str, default=f"{default_models}/checkpoints", help="Trained model directory")
    parser.add_argument("--output_dir", type=str, default=default_models, help="Target models directory")
    args = parser.parse_args()

    out_dir = Path(args.output_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    onnx_target = out_dir / "pocket_sparrow_int8.onnx"
    tflite_target = out_dir / "pocket_sparrow_int8.tflite"

    print("================================================================")
    print("Pocket Sparrow - Unified INT8 Quantization Engine")
    print("================================================================")
    print(f"Input Checkpoint: {args.checkpoint}")
    print(f"Target ONNX:      {onnx_target}")
    print(f"Target TFLite:    {tflite_target}")
    print("Target Size SLA:  <= 35 MB")
    print("Target Latency:   < 40 ms on modern CPU/NPU")
    print("----------------------------------------------------------------")

    # 1. Run ONNX Quantization
    from quantize_onnx import export_and_quantize
    export_and_quantize(args.checkpoint, str(onnx_target))

    # 2. Run TFLite Quantization
    from quantize_tflite import export_and_quantize_tflite
    export_and_quantize_tflite(args.checkpoint, str(tflite_target))

    print("\n[SUCCESS] Unified INT8 Quantization pipeline complete.")

if __name__ == "__main__":
    # Ensure current directory is in sys.path
    export_dir = Path(__file__).resolve().parent
    sys.path.insert(0, str(export_dir))
    main()
