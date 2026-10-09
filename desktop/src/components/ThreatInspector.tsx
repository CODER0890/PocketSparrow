import React, { useState } from "react";
import { ScanResultPayload } from "./ThreatAlertCard";

interface ThreatInspectorProps {
  onScan: (type: "Url" | "SmsText" | "QrPayload", payload: string) => Promise<ScanResultPayload>;
}

export const ThreatInspector: React.FC<ThreatInspectorProps> = ({ onScan }) => {
  const [contentType, setContentType] = useState<"Url" | "SmsText" | "QrPayload">("Url");
  const [inputPayload, setInputPayload] = useState("https://g00gle-security-check.cfd/auth/verify?id=9281");
  const [isScanning, setIsScanning] = useState(false);

  // Demo presets for 3-minute Airplane Mode verification
  const DEMO_PRESETS = [
    {
      label: "Test Case A: Lookalike Phishing URL",
      type: "Url" as const,
      value: "https://g00gle-security-check.cfd/auth/verify?id=9281",
    },
    {
      label: "Test Case A: Urgent Wire Smishing",
      type: "SmsText" as const,
      value: "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: http://fake-bank.top",
    },
    {
      label: "Test Case B: Quishing Malicious QR Code",
      type: "QrPayload" as const,
      value: "MEBKM:TITLE:Claim Reward;URL:https://chase-login.top/account/suspend;;",
    },
    {
      label: "Test Case B: Executable Script QR Code",
      type: "QrPayload" as const,
      value: "javascript:alert('Stolen Token: ' + document.cookie)",
    },
    {
      label: "Benign Control: Clean Wikipedia Link",
      type: "Url" as const,
      value: "https://en.wikipedia.org/wiki/Information_security",
    },
  ];

  const handleScan = async () => {
    if (!inputPayload.trim()) return;
    setIsScanning(true);
    try {
      await onScan(contentType, inputPayload);
    } finally {
      setIsScanning(false);
    }
  };

  return (
    <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-base font-bold text-white">Live Threat Inspector</h2>
          <p className="text-xs text-slate-400">
            Real-time on-device evaluation across URLs, SMS messages, and QR payloads.
          </p>
        </div>

        {/* Content Type Selector */}
        <div className="flex space-x-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
          <button
            onClick={() => {
              setContentType("Url");
              setInputPayload("https://g00gle-security-check.cfd/auth/verify?id=9281");
            }}
            className={`px-3 py-1 rounded font-medium transition ${
              contentType === "Url" ? "bg-cyan-600 text-white" : "text-slate-400 hover:text-white"
            }`}
          >
            URL
          </button>
          <button
            onClick={() => {
              setContentType("SmsText");
              setInputPayload("BANK ALERT: Unusual wire transfer of $2,450.00 initiated. Cancel now: http://x.top");
            }}
            className={`px-3 py-1 rounded font-medium transition ${
              contentType === "SmsText" ? "bg-cyan-600 text-white" : "text-slate-400 hover:text-white"
            }`}
          >
            SMS / Chat
          </button>
          <button
            onClick={() => {
              setContentType("QrPayload");
              setInputPayload("MEBKM:TITLE:Reward;URL:https://chase-login.top/auth;;");
            }}
            className={`px-3 py-1 rounded font-medium transition ${
              contentType === "QrPayload" ? "bg-cyan-600 text-white" : "text-slate-400 hover:text-white"
            }`}
          >
            QR Code
          </button>
        </div>
      </div>

      {/* Input Field */}
      <div className="space-y-3">
        <textarea
          rows={3}
          value={inputPayload}
          onChange={(e) => setInputPayload(e.target.value)}
          placeholder="Paste URL, SMS text, or decoded QR code payload here..."
          className="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-cyan-500 font-mono resize-none"
        />

        {/* Quick Demo Presets */}
        <div className="flex flex-wrap gap-2 items-center text-xs">
          <span className="text-slate-500 font-semibold">Airplane Mode Presets:</span>
          {DEMO_PRESETS.map((preset, idx) => (
            <button
              key={idx}
              onClick={() => {
                setContentType(preset.type);
                setInputPayload(preset.value);
              }}
              className="px-2.5 py-1 rounded bg-slate-800/80 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700/60 transition"
            >
              {preset.label}
            </button>
          ))}
        </div>

        {/* Scan Trigger Button */}
        <div className="flex justify-end pt-2">
          <button
            onClick={handleScan}
            disabled={isScanning || !inputPayload.trim()}
            className="px-5 py-2 rounded-lg bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white text-xs font-bold tracking-wider uppercase transition shadow-lg shadow-cyan-900/30 flex items-center space-x-2 disabled:opacity-50"
          >
            {isScanning ? (
              <>
                <span className="animate-spin h-3.5 w-3.5 border-2 border-white border-t-transparent rounded-full"></span>
                <span>Evaluating On-Device...</span>
              </>
            ) : (
              <span>Evaluate Threat (&lt;50ms)</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
