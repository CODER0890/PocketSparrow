# Pocket Sparrow — Execution & Demo Guide 🚀

This document explains how to set up, build, test, and run **Pocket Sparrow** on Desktop (Linux, Windows, macOS) and Android.

---

## ⚡ Quick Start (TL;DR)

If Flutter is already in your `PATH`:

```bash
# 1. Navigate to the project directory
cd /home/gjgameryt-0890/CodeCarnival/pocket_sparrow

# 2. Run natively on Desktop (Linux)
flutter run -d linux

# 3. Or run on an Android emulator / connected device
flutter run -d android
```

---

## 🛠️ Environment Configuration

Ensure Flutter is available in your shell session:

```bash
# Add Flutter to your current terminal session
export PATH="$HOME/flutter/bin:$PATH"

# (Optional) Verify connected target devices
flutter devices
```

Expected output includes your native desktop device:
```
Found 1 connected device:
  Linux (desktop) • linux • linux-x64 • Arch Linux / Ubuntu / Fedora
```

---

## 🖥️ Running on Desktop Platforms

Pocket Sparrow is designed with an adaptive UI that automatically uses a **NavigationRail sidebar** on wide desktop windows and a **BottomNavigationBar** on mobile screens.

### 🐧 Linux Desktop
```bash
cd pocket_sparrow
export PATH="$HOME/flutter/bin:$PATH"
flutter run -d linux
```

To compile a standalone native executable:
```bash
flutter build linux --debug
# The binary is located at:
# build/linux/x64/debug/bundle/pocket_sparrow
```

### 🪟 Windows Desktop
```bash
flutter run -d windows
```

### 🍏 macOS Desktop
```bash
flutter run -d macos
```

---

## 📱 Running on Android

Pocket Sparrow requires **zero internet permissions**. The Android manifest explicitly omits `android.permission.INTERNET`, guaranteeing an OS-enforced airgap.

### Android Emulator or Physical Device
```bash
# 1. Start your emulator or plug in an Android device via USB
flutter emulators --launch <emulator_id> # (if using Android emulator)

# 2. Run the application
flutter run -d android

# 3. Build a standalone APK
flutter build apk --debug
# The output APK will be at:
# build/app/outputs/flutter-apk/app-debug.apk
```

---

## 🧪 Running Automated Tests & Benchmark

Pocket Sparrow includes comprehensive unit tests verifying the Shannon entropy engine, Cyrillic lookalike detector, SMS banking regex patterns, and the sub-5ms performance benchmark.

```bash
cd pocket_sparrow
export PATH="$HOME/flutter/bin:$PATH"

# Run heuristic engine tests and latency benchmark
flutter test test/heuristic_engine_test.dart

# Run full test suite (heuristics + UI smoke tests)
flutter test
```

Expected output:
```
00:01 +11: All tests passed!
```

---

## 🎬 Step-by-Step Hackathon Demo Script

Follow this 5-step walkthrough during demos or video recordings to showcase the full feature set:

### Step 1: Splash Screen & Airplane Mode Audit (Privacy Proof)
1. Launch the app: watch the **1.5s Splash Screen** displaying the pulsing shield and the `"100% On-Device Threat Detection"` tagline.
2. Navigate to the **Privacy** tab (`Privacy` icon).
3. Point out:
   - **0 BYTES SENT** live meter with a pulsing green glow.
   - **Network Interface Audit**: Shows `0 network sockets opened` and `DNS Resolution: BLOCKED`.
   - **OS Permission Audit**: Confirms `android.permission.INTERNET` is NOT requested in the manifest.
4. Tap **Run Offline Verification Test** to prove heuristics operate in a complete airgap.
5. Tap the **AIRGAP ACTIVE** badge in the top AppBar to demonstrate the visual strikethrough of Wi-Fi and Cellular icons.

### Step 2: Cyrillic Homoglyph Attack (Scan URL)
1. Go to the **URL** tab.
2. Under **Demo Mode: 10 Curated Test URLs**, tap the `Apple ID Cyrillic Homoglyph` chip.
   *(Or manually enter `https://аpple.com/login` using the Cyrillic `а` \u0430).*
3. Tap **Check Link (<5ms)**.
4. Observe:
   - The animated **Scanning Radar** with ripple rings.
   - The **Result Card** sliding in from the bottom with haptic micro-shake vibration.
   - The **BLOCKED** badge with `<5ms` latency.
   - The **XAI Explainer** detailing: *"Domain uses Cyrillic 'а' instead of Latin 'a' (homograph attack to deceive eyes)"*.
5. Tap **Inspect Raw XAI JSON** to show the smooth accordion animation and standard JSON output.

### Step 3: Urgent Banking Scam (Scan SMS)
1. Go to the **SMS** tab.
2. Under **Demo Mode: 10 Curated Scam SMS Scenarios**, tap `SBI Account Deactivation Scam`.
3. Tap **Analyze Message (<5ms)**.
4. Observe:
   - The **Flagged Message Highlights** box displaying red badge highlights on trigger phrases: `account will be blocked`, `urgent`, `verify now`, and `http://sbi-kyc-update.xyz`.
   - The XAI card showing bank spoofing detection + embedded link threat analysis.

### Step 4: Malicious QR Code Decoding (Scan QR)
1. Go to the **QR** tab.
2. Under **Demo Mode: Inject Generated QR Images**, tap **Load Phishing QR PNG**.
3. Observe:
   - The real generated QR image (`assets/demo/qr_phishing.png`) loads in the viewport under the animated laser bar.
   - Instant decoding of the underlying payload (`https://аpple.com/login`).
   - The instant **BLOCKED** verdict and explanation.
4. Tap **Load Safe QR PNG** to show the instant green **SAFE** verdict for `https://google.com`.

### Step 5: Clipboard Sentinel (Clipboard Shield)
1. Go to the **Clipboard** tab.
2. Under **Test Clipboard Hijacking (Demo)**, tap **Copy Phishing Link**.
3. Point out how the background guardian automatically intercepts the copied link in volatile memory and generates an immediate alert card with XAI explanations.

### Step 6: Encrypted Local Audit Log
1. Go to the **Logs** tab (`Audit Log` icon).
2. Point out:
   - Encrypted on-device SQLite database indicator.
   - Filter chips: Tap `Blocked`, `Caution`, or `Safe` to filter records.
   - Tap any record to open the complete inspection breakdown modal.

---

## 📂 Project Structure Reference

```
pocket_sparrow/
├── lib/
│   ├── main.dart                      # Adaptive desktop/mobile navigation shell
│   ├── core/                          # Detection logic & encrypted database
│   │   ├── detection_engine.dart      # Master coordinator & latency stopwatch
│   │   ├── heuristic_engine.dart      # Entropy, homoglyphs, TLDs, IP, brands
│   │   ├── sms_heuristic_engine.dart  # SMS urgency, payment lures, bank patterns
│   │   ├── xai_explainer.dart         # Plain-language explanation builder
│   │   ├── threat_model.dart          # ThreatResult data class
│   │   ├── local_db.dart              # Encrypted SQLite storage (sqflite_common_ffi)
│   │   └── app_state_provider.dart    # Central state management
│   ├── features/                      # The 8 feature screens + splash
│   │   ├── splash/                    # 1.5s splash screen
│   │   ├── dashboard/                 # Protection hero & live stats
│   │   ├── scan_url/                  # URL heuristic scanner
│   │   ├── scan_sms/                  # SMS scanner with highlighted triggers
│   │   ├── scan_qr/                   # QR scanner & PNG image loader
│   │   ├── clipboard_monitor/         # Live clipboard sentinel
│   │   ├── threat_log/                # Encrypted audit log & filters
│   │   ├── privacy_dashboard/         # 0 bytes sent meter & airgap audit
│   │   └── settings/                  # Demo mode & configuration
│   └── ui/                            # Cyber dark theme & animated widgets
├── assets/
│   ├── blocklist.json                 # 50 known-bad domains & TLDs
│   ├── scam_patterns.json             # 20 regex patterns for SMS scams
│   └── demo/
│       ├── qr_phishing.png            # Generated QR image (homoglyph phish)
│       ├── qr_safe.png                # Generated QR image (safe baseline)
│       └── demo_samples.json          # 10 SMS scams & 10 phishing URLs
└── test/
    ├── heuristic_engine_test.dart     # Unit tests & sub-5ms benchmark
    └── widget_test.dart               # UI smoke test
```
