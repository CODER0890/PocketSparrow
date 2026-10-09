import React, { useState } from "react";
import { Globe, MessageSquare, QrCode, Search, Zap, Sparkles } from "lucide-react";
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
      label: "Test A: Cyrillic Homoglyph URL",
      tag: "PHISH",
      type: "Url" as const,
      value: "https://secure-p\u0430ypal.com/verify-account",
    },
    {
      label: "Test A: Urgent Wire Smishing",
      tag: "SCAM",
      type: "SmsText" as const,
      value: "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: http://fake-chase.top",
    },
    {
      label: "Test B: Quishing Malicious QR",
      tag: "QUISH",
      type: "QrPayload" as const,
      value: "MEBKM:TITLE:Claim Reward;URL:https://chase-login.top/account/suspend;;",
    },
    {
      label: "Test B: Executable Script QR",
      tag: "CODE",
      type: "QrPayload" as const,
      value: "javascript:alert('Stolen Token: ' + document.cookie)",
    },
    {
      label: "Benign Control: Wikipedia Link",
      tag: "SAFE",
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
    <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-4">
        <div>
          <div className="flex items-center space-x-2">
            <Search className="w-4 h-4 text-cyan-400" />
            <h2 className="text-base font-bold text-white tracking-wide font-mono">
              Live Threat Inspector
            </h2>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time on-device inspection across URLs, SMS messages, and QR payloads.
          </p>
        </div>

        {/* Content Type Selector */}
        <div className="flex space-x-1 bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs self-start md:self-auto shadow-inner">
          <button
            onClick={() => {
              setContentType("Url");
              setInputPayload("https://g00gle-security-check.cfd/auth/verify?id=9281");
            }}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg font-medium transition-all ${
              contentType === "Url"
                ? "bg-gradient-to-r from-cyan-600 to-blue-600 text-white shadow-md shadow-cyan-600/30"
                : "text-slate-400 hover:text-white hover:bg-slate-900"
            }`}
          >
            <Globe className="w-3.5 h-3.5" />
            <span>URL</span>
          </button>
          <button
            onClick={() => {
              setContentType("SmsText");
              setInputPayload("URGENT: Your bank account is locked. Wire funds immediately to 9821-XXXX.");
            }}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg font-medium transition-all ${
              contentType === "SmsText"
                ? "bg-gradient-to-r from-cyan-600 to-blue-600 text-white shadow-md shadow-cyan-600/30"
                : "text-slate-400 hover:text-white hover:bg-slate-900"
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>SMS / Chat</span>
          </button>
          <button
            onClick={() => {
              setContentType("QrPayload");
              setInputPayload("MEBKM:TITLE:Claim Reward;URL:https://chase-login.top/account/suspend;;");
            }}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg font-medium transition-all ${
              contentType === "QrPayload"
                ? "bg-gradient-to-r from-cyan-600 to-blue-600 text-white shadow-md shadow-cyan-600/30"
                : "text-slate-400 hover:text-white hover:bg-slate-900"
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>QR Code</span>
          </button>
        </div>
      </div>

      {/* Input Text Area */}
      <div className="relative">
        <textarea
          rows={3}
          value={inputPayload}
          onChange={(e) => setInputPayload(e.target.value)}
          placeholder="Paste URL, SMS text message, or decoded QR code payload to inspect..."
          className="w-full bg-slate-950/80 border border-slate-700/80 rounded-xl p-3.5 text-xs text-slate-100 font-mono focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400/40 transition resize-none placeholder:text-slate-600 shadow-inner"
        />
        <div className="absolute right-3 bottom-3 text-[10px] text-slate-500 font-mono">
          {inputPayload.length} chars
        </div>
      </div>

      {/* Preset Action Chips */}
      <div className="mt-3.5 flex flex-wrap items-center gap-2">
        <span className="text-[11px] text-slate-400 font-medium flex items-center gap-1">
          <Sparkles className="w-3 h-3 text-cyan-400" /> Presets:
        </span>
        {DEMO_PRESETS.map((preset, idx) => (
          <button
            key={idx}
            onClick={() => {
              setContentType(preset.type);
              setInputPayload(preset.value);
            }}
            className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-950 hover:bg-slate-800/80 border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white font-mono transition flex items-center space-x-1.5 group"
          >
            <span
              className={`text-[9px] px-1 py-0.2 rounded font-bold ${
                preset.tag === "SAFE"
                  ? "bg-emerald-950 text-emerald-400"
                  : "bg-rose-950 text-rose-400"
              }`}
            >
              {preset.tag}
            </span>
            <span className="group-hover:text-cyan-300 transition-colors">{preset.label}</span>
          </button>
        ))}
      </div>

      {/* Scan Button */}
      <div className="mt-4 flex justify-end">
        <button
          onClick={handleScan}
          disabled={isScanning || !inputPayload.trim()}
          className="flex items-center space-x-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs uppercase tracking-wider shadow-lg shadow-cyan-500/25 hover:shadow-cyan-400/40 disabled:opacity-50 disabled:cursor-not-allowed transition-all font-mono active:scale-95"
        >
          <Zap className={`w-4 h-4 ${isScanning ? "animate-spin" : ""}`} />
          <span>{isScanning ? "INSPECTING..." : "EVALUATE THREAT (<50ms)"}</span>
        </button>
      </div>
    </div>
  );
};
