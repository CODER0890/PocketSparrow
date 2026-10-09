#!/usr/bin/env python3
"""
Pocket Sparrow - Vocabulary Generator & Exporter
Builds a compact WordPiece vocabulary file (vocab.txt) optimized for URL and SMS scam tokens,
including common cyber threat subwords, special tokens ([PAD], [UNK], [CLS], [SEP], [MASK]),
and ASCII characters.
"""

import os
from pathlib import Path

SPECIAL_TOKENS = ["[PAD]", "[UNK]", "[CLS]", "[SEP]", "[MASK]"]

CORE_WORDS = [
    # General words
    "the", "of", "and", "to", "a", "in", "for", "is", "on", "that", "by", "this", "with",
    "i", "you", "it", "not", "or", "be", "are", "from", "at", "as", "your", "all", "have",
    "new", "more", "an", "was", "we", "will", "home", "can", "us", "about", "if", "page",
    "my", "has", "search", "free", "but", "our", "one", "other", "do", "no", "information",
    "time", "they", "site", "he", "up", "may", "what", "which", "their", "news", "out",
    "use", "any", "there", "see", "only", "so", "his", "when", "contact", "here", "business",
    "who", "web", "also", "now", "help", "get", "pm", "am", "today", "tomorrow", "yesterday",
    
    # Financial & Banking tokens
    "bank", "chase", "wells", "fargo", "citi", "paypal", "crypto", "bitcoin", "wire", "transfer",
    "account", "balance", "card", "credit", "debit", "payment", "due", "penalty", "irs", "tax",
    "refund", "unauthorized", "dispute", "transaction", "amount", "billing", "deposit", "direct",
    
    # Urgency & Threat tokens
    "urgent", "alert", "notice", "suspended", "locked", "immediately", "action", "required",
    "warning", "security", "breach", "compromised", "verify", "verification", "confirm",
    "confirmation", "update", "cancel", "passcode", "code", "pin", "otp", "password", "login",
    "signin", "auth", "identity", "recover", "unlock", "membership", "paused", "failed",
    
    # Brand & Delivery tokens
    "apple", "google", "amazon", "netflix", "microsoft", "usps", "fedex", "ups", "dhl",
    "package", "delivery", "order", "tracking", "shipment", "address", "parcel", "redelivery",
    
    # Cyber & URL tokens
    "http", "https", "www", "com", "net", "org", "top", "xyz", "cfd", "rest", "loan", "club",
    "shop", "live", "link", "click", "portal", "support", "device", "secure", "online",
    "service", "app", "apk", "download", "install", "gift", "reward", "winner", "claim",
    "prize", "survey", "voucher", "points", "store"
]

def generate_vocab(output_path: Path):
    vocab = list(SPECIAL_TOKENS)
    
    # Add ASCII printable single characters
    for ch in range(32, 127):
        char_str = chr(ch)
        if char_str not in vocab:
            vocab.append(char_str)
            
    # Add core words
    for word in CORE_WORDS:
        if word not in vocab:
            vocab.append(word)
            
    # Add subword prefixes (##)
    for word in CORE_WORDS:
        sub = f"##{word}"
        if sub not in vocab:
            vocab.append(sub)
            
    # Add two-letter subword combinations
    for c1 in "abcdefghijklmnopqrstuvwxyz":
        for c2 in "abcdefghijklmnopqrstuvwxyz":
            sub = f"##{c1}{c2}"
            if sub not in vocab:
                vocab.append(sub)

    # Write out vocab.txt
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        for token in vocab:
            f.write(f"{token}\n")
            
    print(f"Exported vocabulary with {len(vocab)} tokens to {output_path}")

def main():
    models_dir = Path(__file__).resolve().parent.parent / "models"
    vocab_path = models_dir / "vocab.txt"
    generate_vocab(vocab_path)

if __name__ == "__main__":
    main()
