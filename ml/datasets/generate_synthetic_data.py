#!/usr/bin/env python3
"""
Pocket Sparrow - Synthetic Threat Dataset Generator
Generates realistic, safe simulated smishing, phishing, homoglyphs, and benign samples
for fine-tuning and offline validation. 100% synthetic, zero real malware or live campaigns.
"""

import json
import random
from pathlib import Path

# Safe synthetic templates
BENIGN_MESSAGES = [
    "Hey, are we still meeting for lunch today at 1 PM?",
    "Your appointment with Dr. Patel is confirmed for tomorrow at 10:30 AM.",
    "Can you please send me the meeting notes from yesterday's sync?",
    "Reminder: Grocery list - milk, eggs, sourdough bread, apples.",
    "Your package from Order #92834 has been delivered to your front porch.",
    "Hey! Just checking in to see how your weekend went.",
    "The code review is approved. Merging into main branch now.",
    "Don't forget tomorrow is Mom's birthday dinner at 7 PM.",
    "Flight AA412 is on schedule. Gate B12 departs in 45 minutes.",
    "Team standup starts in 5 minutes on Google Meet.",
    "Thanks for the coffee! Let's catch up again next week.",
    "Your library books are due back in 3 days. Renew online anytime.",
    "Hey, could you forward me the PDF report when you get a chance?",
    "Dinner was great tonight, thanks for hosting!",
    "Your weekly screen time report is ready to review."
]

BENIGN_URLS = [
    "https://www.google.com/search?q=weather+today",
    "https://en.wikipedia.org/wiki/Information_security",
    "https://github.com/torvalds/linux",
    "https://developer.android.com/reference/kotlin/packages",
    "https://www.apple.com/macos/sonoma",
    "https://doc.rust-lang.org/book/title-page.html",
    "https://www.nytimes.com/section/technology",
    "https://news.ycombinator.com/item?id=3849201",
    "https://stackoverflow.com/questions/tagged/rust",
    "https://www.reddit.com/r/privacy",
    "https://crates.io/crates/serde",
    "https://flutter.dev/docs/development/ui",
    "https://www.bbc.com/news/world",
    "https://developer.mozilla.org/en-US/docs/Web/JavaScript",
    "https://arxiv.org/abs/2301.00001"
]

SMISHING_TEMPLATES = [
    ("URGENT: Your Chase account ending in 4102 has been suspended due to suspicious activity. Verify identity immediately: {url}", "URGENT_WIRE_TRANSFER"),
    ("USPS Alert: Your package could not be delivered due to an incorrect address. Update delivery address within 24 hours: {url}", "DELIVERY_SCAM"),
    ("BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. If this was not you, cancel transaction now: {url}", "URGENT_WIRE_TRANSFER"),
    ("IRS Final Notice: An outstanding tax penalty of $1,820 is due immediately. Avoid legal action by settling balance: {url}", "GOV_IMPERSONATION"),
    ("Netflix Payment Failed: Your membership has been paused. Update your billing details today to resume streaming: {url}", "CREDENTIAL_HARVESTING"),
    ("Wells Fargo Security: A new device logged into your online banking from IP 192.168.1.1. Lock account now: {url}", "CREDENTIAL_HARVESTING"),
    ("Apple ID Alert: Your account was locked for security reasons. Confirm your password to unlock: {url}", "CREDENTIAL_HARVESTING"),
    ("PayPal Notification: You sent $899.99 to crypto-exchange. If unauthorized, dispute charge within 60 minutes: {url}", "URGENT_WIRE_TRANSFER"),
    ("HR Dept: Please sign the updated direct deposit authorization form before end of business day: {url}", "WORKPLACE_IMPERSONATION"),
    ("Amazon Prime Notice: Your order for iPhone 15 Pro ($1,199.00) is processing. If you did not place this, click here: {url}", "ECOMMERCE_SCAM")
]

PHISHING_URL_PATTERNS = [
    # Typosquatting / Cyrillic homoglyph lookalikes
    ("https://g00gle-security-check.cfd/auth/verify?id=9281", "HOMOGRAPH", "Uses deceptive lookalike domain 'g00gle' with high-risk TLD .cfd"),
    ("https://chase-bank-secure-login.top/account/suspend?ref=urgent", "CREDENTIAL_HARVESTING", "Subdomain nesting and high-risk TLD .top with banking keywords"),
    ("https://paypal.com.verify-billing-update.xyz/login.php", "SUBDOMAIN_DECEPTION", "Spoofs paypal.com in subdomains with actual parent domain verify-billing-update.xyz"),
    ("https://wellsfargo-verify-wire.live/portal/authenticate", "URGENT_WIRE_TRANSFER", "Fake banking portal designed to intercept two-factor codes"),
    ("https://apple-id-verify.support-device.rest/recover", "CREDENTIAL_HARVESTING", "Brand impersonation targeting Apple ID credentials with .rest TLD"),
    ("https://usps-tracking-redelivery.shop/address/confirm", "DELIVERY_SCAM", "Parcel delivery scam designed to harvest credit card for redelivery fee"),
    ("https://netflix-billing-update.club/membership/renewal", "CREDENTIAL_HARVESTING", "Subscription phishing portal harvesting payment details"),
    ("https://amaz0n-security-hub.loan/prime/order-cancel", "HOMOGRAPH", "Typosquatted domain with high-risk loan TLD attempting order cancellation scam"),
    ("https://secure-login-chase.top/online/wire-transfer.html", "URGENT_WIRE_TRANSFER", "High entropy path with misleading banking branding"),
    ("https://рaypal.com/verify-account", "HOMOGRAPH", "Cyrillic homograph 'р' (U+0440) disguising as latin 'p' in paypal.com")
]

def generate_dataset():
    records = []
    
    # 1. Benign Messages & URLs (Label: 0 = SAFE)
    for msg in BENIGN_MESSAGES:
        records.append({
            "id": f"benign_msg_{len(records)}",
            "text": msg,
            "content_type": "SMS_TEXT",
            "label": 0,
            "category": "SAFE",
            "xai_explanation": "Standard conversational message with no deceptive urgency or credential requests."
        })
        
    for url in BENIGN_URLS:
        records.append({
            "id": f"benign_url_{len(records)}",
            "text": url,
            "content_type": "URL",
            "label": 0,
            "category": "SAFE",
            "xai_explanation": "Legitimate top-level domain with natural entropy and authentic DNS identity."
        })
        
    # 2. Suspicious / Ambiguous Samples (Label: 1 = SUSPICIOUS)
    suspicious_samples = [
        ("Your temporary verification passcode is 492019. Valid for 10 minutes. Do not share.", "SMS_TEXT", "Standard OTP message, neutral tone."),
        ("Hi John, here is the shared Dropbox folder with project slides: https://dropbox-share.net/s/9a8b7c", "SMS_TEXT", "Unverified third-party file sharing link with potential risk."),
        ("https://bit.ly/3xY9Ab2", "URL", "Generic URL shortener concealing the final destination hostname."),
        ("https://tinyurl.com/meeting-agenda-2026", "URL", "Shortened redirect link requiring unmasking."),
        ("Click here to claim your survey rewards points before expiration: https://points-rewards.store/claim", "SMS_TEXT", "Promotional rewards message with mild urgency cues.")
    ]
    for text, ctype, xai in suspicious_samples:
        records.append({
            "id": f"suspicious_{len(records)}",
            "text": text,
            "content_type": ctype,
            "label": 1,
            "category": "SUSPICIOUS",
            "xai_explanation": xai
        })
        
    # 3. Malicious Phishing & Smishing (Label: 2 = MALICIOUS)
    for url, category, xai in PHISHING_URL_PATTERNS:
        records.append({
            "id": f"malicious_url_{len(records)}",
            "text": url,
            "content_type": "URL",
            "label": 2,
            "category": category,
            "xai_explanation": xai
        })
        
    for template, category in SMISHING_TEMPLATES:
        sim_url = random.choice([u[0] for u in PHISHING_URL_PATTERNS])
        text = template.format(url=sim_url)
        records.append({
            "id": f"malicious_sms_{len(records)}",
            "text": text,
            "content_type": "SMS_TEXT",
            "label": 2,
            "category": category,
            "xai_explanation": f"High-urgency coercive prompt impersonating reputable service coupled with an unverified credential harvesting domain."
        })
        
    return records

def main():
    dataset_dir = Path(__file__).resolve().parent
    dataset_dir.mkdir(parents=True, exist_ok=True)
    
    data = generate_dataset()
    output_file = dataset_dir / "sample_threats.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        
    print(f"Generated {len(data)} synthetic threat and benign samples -> {output_file}")
    
    # Print summary statistics
    labels = {0: 0, 1: 0, 2: 0}
    for item in data:
        labels[item["label"]] += 1
    print(f"Dataset split: SAFE(0): {labels[0]}, SUSPICIOUS(1): {labels[1]}, MALICIOUS(2): {labels[2]}")

if __name__ == "__main__":
    main()
