import React, { useState } from "react";
import {
  Globe,
  MessageSquare,
  QrCode,
  Copy,
  Check,
  Search,
  Loader2,
} from "lucide-react";
import { ScanResultPayload } from "./XaiDrawer";

interface ThreatInspectorProps {
  onScan: (type: "Url" | "SmsText" | "QrPayload", payload: string) => Promise<ScanResultPayload>;
}

export const ThreatInspector: React.FC<ThreatInspectorProps> = ({ onScan }) => {
  const [contentType, setContentType] = useState<"Url" | "SmsText" | "QrPayload">("Url");
  const [inputPayload, setInputPayload] = useState(
    "https://secure-p\u0430ypal.com/verify-account?token=9281a4b"
  );
  const [isScanning, setIsScanning] = useState(false);
  const [copied, setCopied] = useState(false);

  const DEMO_PRESETS = [
    {
      label: "Cyrillic Homoglyph",
      type: "Url" as const,
      value: "https://secure-p\u0430ypal.com/verify-account?token=9281a4b",
    },
    {
      label: "Urgent Wire Transfer",
      type: "SmsText" as const,
      value:
        "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: http://fake-chase.top",
    },
    {
      label: "Malicious QR Scheme",
      type: "QrPayload" as const,
      value: "javascript:alert('Stolen Token: ' + document.cookie)",
    },
    {
      label: "Benign Domain",
      type: "Url" as const,
      value: "https://en.wikipedia.org/wiki/Information_security",
    },
  ];

  const handleScan = async () => {
    if (!inputPayload.trim() || isScanning) return;
    setIsScanning(true);
    try {
      await onScan(contentType, inputPayload);
    } finally {
      setIsScanning(false);
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(inputPayload);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-6 space-y-5">
      {/* Header and Type Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-zinc-100 tracking-tight">
            Payload Inspector
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Run on-device heuristic and neural model evaluations against untrusted vectors.
          </p>
        </div>

        {/* Vector Tabs */}
        <div className="inline-flex p-1 rounded-md bg-zinc-950 border border-zinc-800 self-start sm:self-auto text-xs">
          <button
            onClick={() => {
              setContentType("Url");
              setInputPayload("https://secure-p\u0430ypal.com/verify-account?token=9281a4b");
            }}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded transition-colors ${
              contentType === "Url"
                ? "bg-zinc-800 text-zinc-100 font-medium shadow-sm"
                : "text-zinc-400 hover:text-zinc-200"
            }`}
          >
            <Globe className="w-3.5 h-3.5" />
            <span>URL</span>
          </button>
          <button
            onClick={() => {
              setContentType("SmsText");
              setInputPayload(
                "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: http://fake-chase.top"
              );
            }}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded transition-colors ${
              contentType === "SmsText"
                ? "bg-zinc-800 text-zinc-100 font-medium shadow-sm"
                : "text-zinc-400 hover:text-zinc-200"
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>SMS / Message</span>
          </button>
          <button
            onClick={() => {
              setContentType("QrPayload");
              setInputPayload("javascript:alert('Stolen Token: ' + document.cookie)");
            }}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded transition-colors ${
              contentType === "QrPayload"
                ? "bg-zinc-800 text-zinc-100 font-medium shadow-sm"
                : "text-zinc-400 hover:text-zinc-200"
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>QR Code</span>
          </button>
        </div>
      </div>

      {/* Input Textarea (Clean, Standard UI) */}
      <div className="relative">
        <textarea
          rows={3}
          value={inputPayload}
          onChange={(e) => setInputPayload(e.target.value)}
          onKeyDown={(e) => {
            if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
              handleScan();
            }
          }}
          placeholder="Enter URL, text message, or QR string to evaluate..."
          className="w-full bg-zinc-950 border border-zinc-800 rounded-md p-3.5 text-sm font-mono text-zinc-100 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-700 transition-colors resize-none leading-relaxed"
        />
        <div className="absolute right-3 bottom-3 flex items-center space-x-2">
          <button
            onClick={handleCopy}
            className="p-1 rounded text-zinc-500 hover:text-zinc-300 transition-colors"
            title="Copy payload"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
          <span className="text-xs font-mono text-zinc-600">
            {inputPayload.length} B
          </span>
        </div>
      </div>

      {/* Quick Test Vectors */}
      <div className="flex flex-wrap items-center gap-2 pt-1">
        <span className="text-xs text-zinc-400 mr-1">Sample vectors:</span>
        {DEMO_PRESETS.map((preset, idx) => (
          <button
            key={idx}
            onClick={() => {
              setContentType(preset.type);
              setInputPayload(preset.value);
            }}
            className="text-xs px-2.5 py-1 rounded bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 hover:text-zinc-100 transition-colors"
          >
            {preset.label}
          </button>
        ))}
      </div>

      {/* Action Row */}
      <div className="pt-4 border-t border-zinc-800 flex items-center justify-between">
        <div className="text-xs text-zinc-400">
          Engine: Tier 1 Heuristics &amp; MobileBERT INT8
        </div>

        <div className="flex items-center space-x-3">
          <span className="text-xs text-zinc-400 hidden sm:inline">
            Press <kbd className="px-1.5 py-0.5 rounded bg-zinc-900 border border-zinc-800 text-zinc-300 font-mono">⌘ + ↵</kbd>
          </span>

          <button
            onClick={handleScan}
            disabled={isScanning || !inputPayload.trim()}
            className="flex items-center space-x-2 px-4 py-2 rounded-md bg-zinc-100 hover:bg-zinc-200 text-zinc-900 font-medium text-xs transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isScanning ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Search className="w-3.5 h-3.5" />
            )}
            <span>{isScanning ? "Evaluating..." : "Evaluate Threat"}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
