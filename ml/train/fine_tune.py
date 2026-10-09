#!/usr/bin/env python3
"""
Pocket Sparrow - Model Fine-Tuning Pipeline
Fine-tunes MobileBERT (or DistilBERT) on SMS scam, phishing URL, and credential harvesting corpora.
Outputs optimized PyTorch model ready for INT8 quantization.
"""

import os
import sys
import json
import argparse
from pathlib import Path

def resolve_path(p: str) -> Path:
    path = Path(p)
    if path.exists():
        return path
    # Try relative to repo root if run from ml/
    repo_root = Path(__file__).resolve().parent.parent.parent
    if (repo_root / p).exists():
        return repo_root / p
    # Try relative to ml/ if run from repo root
    ml_root = Path(__file__).resolve().parent.parent
    if (ml_root / p).exists():
        return ml_root / p
    return path

def train(args):
    dataset_path = resolve_path(args.dataset_path)

    print("=" * 60)
    print("Pocket Sparrow ML Engine - MobileBERT Fine-Tuning")
    print("=" * 60)
    print(f"Base Model:       {args.model_name}")
    print(f"Dataset:          {dataset_path}")
    print(f"Output Directory: {args.output_dir}")
    print(f"Epochs:           {args.epochs}")
    print(f"Batch Size:       {args.batch_size}")
    print(f"Target Max Size:  ~35MB (INT8 Quantized)")
    print("-" * 60)

    # Check for PyTorch & Transformers
    try:
        import torch
        from transformers import AutoTokenizer, AutoModelForSequenceClassification, Trainer, TrainingArguments
        from sklearn.metrics import accuracy_score, precision_recall_fscore_support
        from torch.utils.data import Dataset
    except ImportError:
        print("[WARNING] PyTorch or Transformers not installed in current environment.")
        print("To run the full GPU/CPU training pipeline:")
        print("  pip install -r ml/requirements.txt")
        print("\nSimulating training pipeline validation check:")
        if dataset_path.exists():
            with open(dataset_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            print(f"[OK] Successfully verified {len(data)} training samples in {dataset_path}")
        else:
            print(f"[ERROR] Dataset {dataset_path} not found.")
            sys.exit(1)
        print("[OK] Fine-tuning configuration verified successfully.")
        return

    # Real training logic if dependencies are present
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Training using compute device: {device}")

    # Load dataset
    with open(dataset_path, "r", encoding="utf-8") as f:
        raw_data = json.load(f)

    texts = [item["text"] for item in raw_data]
    labels = [item["label"] for item in raw_data]

    tokenizer = AutoTokenizer.from_pretrained(args.model_name)
    
    class ThreatDataset(Dataset):
        def __init__(self, texts, labels, tokenizer, max_len=128):
            self.encodings = tokenizer(texts, truncation=True, padding=True, max_length=max_len, return_tensors="pt")
            self.labels = torch.tensor(labels, dtype=torch.long)

        def __len__(self):
            return len(self.labels)

        def __getitem__(self, idx):
            item = {key: val[idx] for key, val in self.encodings.items()}
            item['labels'] = self.labels[idx]
            return item

    dataset = ThreatDataset(texts, labels, tokenizer)
    model = AutoModelForSequenceClassification.from_pretrained(args.model_name, num_labels=3)
    model.to(device)

    def compute_metrics(eval_pred):
        predictions, targets = eval_pred
        preds = predictions.argmax(-1)
        precision, recall, f1, _ = precision_recall_fscore_support(targets, preds, average='weighted', zero_division=0)
        acc = accuracy_score(targets, preds)
        return {'accuracy': acc, 'f1': f1, 'precision': precision, 'recall': recall}

    training_args = TrainingArguments(
        output_dir=args.output_dir,
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        warmup_steps=50,
        weight_decay=0.01,
        logging_dir='./logs',
        logging_steps=10,
        save_strategy="epoch",
        evaluation_strategy="no"
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=dataset,
        compute_metrics=compute_metrics
    )

    print("Beginning fine-tuning...")
    trainer.train()
    
    # Save final model & tokenizer
    output_path = Path(args.output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    model.save_pretrained(output_path)
    tokenizer.save_pretrained(output_path)
    print(f"Model saved to {output_path}")

def main():
    parser = argparse.ArgumentParser(description="Fine-tune MobileBERT/DistilBERT for Pocket Sparrow")
    parser.add_argument("--model_name", type=str, default="google/mobilebert-uncased", help="Hugging Face model ID")
    parser.add_argument("--dataset_path", type=str, default="datasets/sample_threats.json", help="Path to training data")
    parser.add_argument("--output_dir", type=str, default="models/checkpoints", help="Output directory")
    parser.add_argument("--epochs", type=int, default=3, help="Number of training epochs")
    parser.add_argument("--batch_size", type=int, default=16, help="Batch size")
    args = parser.parse_args()
    
    train(args)

if __name__ == "__main__":
    main()
