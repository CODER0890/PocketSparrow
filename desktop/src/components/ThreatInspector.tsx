import React, { useState } from "react";
import {
  Globe,
  MessageSquare,
  QrCode,
  Terminal,
  Zap,
  CornerDownLeft,
  Sparkles,
  Check,
  Copy,

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

  // Enterprise presets matching Airplane Mode live scenarios
  const DEMO_PRESETS = [
    {
      label: "Test Case A: Cyrillic Homoglyph",
      tag: "HOMOGRAPH",
      method: "URL",
      type: "Url" as const,
      value: "https://secure-p\u0430ypal.com/verify-account?token=9281a4b",
    },
    {
      label: "Test Case A: Urgent Wire Smishing",
      tag: "SOCIAL_ENG",
      method: "SMS",
      type: "SmsText" as const,
      value:
        "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: http://fake-chase.top",
    },
    {
      label: "Test Case B: Quishing Malicious QR",
      tag: "REDIRECT",
      method: "QR",
      type: "QrPayload" as const,
      value: "MEBKM:TITLE:Claim Reward;URL:https://chase-login.top/account/suspend;;",
    },
    {
      label: "Test Case B: Script Injection QR",
      tag: "EXEC_SCRIPT",
      method: "QR",
      type: "QrPayload" as const,
      value: "javascript:alert('Stolen Token: ' + document.cookie)",
    },
    {
      label: "Benign Control: Authenticated Domain",
      tag: "TRUSTED",
      method: "URL",
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
    <div className="rounded-xl border border-surface-border bg-background-elevated shadow-card overflow-hidden">
      {/* Terminal Title Bar */}
      <div className="px-4 py-2.5 border-b border-surface-border bg-background-subtle flex items-center justify-between">
        <div className="flex items-center space-x-3">
          {/* Mock Window Dots */}
          <div className="flex space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-surface-border"></span>
            <span className="w-2.5 h-2.5 rounded-full bg-surface-border"></span>
            <span className="w-2.5 h-2.5 rounded-full bg-surface-border"></span>
          </div>
          <span className="text-brand-faint text-xs">|</span>
          <div className="flex items-center space-x-1.5 text-xs font-mono text-brand-muted">
            <Terminal className="w-3.5 h-3.5 text-accent" strokeWidth={1.75} />
            <span className="text-brand-secondary">sparrow@engine</span>
            <span className="text-brand-faint">:</span>
            <span className="text-accent">~/inspect</span>
          </div>
        </div>

        {/* Vector Mode Selector Tabs */}
        <div className="flex items-center p-0.5 rounded-lg bg-background border border-surface-border text-xs">
          <button
            onClick={() => {
              setContentType("Url");
              setInputPayload("https://secure-p\u0430ypal.com/verify-account?token=9281a4b");
            }}
            className={`flex items-center space-x-1.5 px-3 py-1 rounded-md transition-all font-medium text-xs ${
              contentType === "Url"
                ? "bg-surface-active text-brand-text shadow-sm border border-surface-borderHover"
                : "text-brand-muted hover:text-brand-secondary"
            }`}
          >
            <Globe className="w-3.5 h-3.5" />
            <span>URL</span>
          </button>
          <button
            onClick={() => {
              setContentType("SmsText");
              setInputPayload(
                "URGENT: Your bank account is suspended. Wire funds immediately to 9821-XXXX."
              );
            }}
            className={`flex items-center space-x-1.5 px-3 py-1 rounded-md transition-all font-medium text-xs ${
              contentType === "SmsText"
                ? "bg-surface-active text-brand-text shadow-sm border border-surface-borderHover"
                : "text-brand-muted hover:text-brand-secondary"
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
            className={`flex items-center space-x-1.5 px-3 py-1 rounded-md transition-all font-medium text-xs ${
              contentType === "QrPayload"
                ? "bg-surface-active text-brand-text shadow-sm border border-surface-borderHover"
                : "text-brand-muted hover:text-brand-secondary"
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>QR Code</span>
          </button>
        </div>
      </div>

      {/* Editor Content Area with Gutter */}
      <div className="p-4 bg-background/50">
        <div className="flex rounded-lg border border-surface-border bg-background-subtle/80 overflow-hidden focus-within:border-accent/60 transition shadow-inner">
          {/* Line Numbers Gutter */}
          <div className="w-10 py-3 bg-surface border-r border-surface-border text-center select-none font-mono text-[11px] text-brand-faint leading-5 space-y-0.5">
            <div>01</div>
            <div>02</div>
            <div>03</div>
          </div>

          {/* Text Area Input */}
          <div className="flex-1 relative">
            <textarea
              rows={3}
              value={inputPayload}
              onChange={(e) => setInputPayload(e.target.value)}
              onKeyDown={(e) => {
                if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
                  handleScan();
                }
              }}
              placeholder="Paste target URL, message payload, or QR code content..."
              className="w-full bg-transparent p-3 text-xs text-brand-text font-mono leading-5 focus:outline-none resize-none placeholder:text-brand-faint"
            />
            <div className="absolute right-2.5 bottom-2.5 flex items-center space-x-2">
              <button
                onClick={handleCopy}
                className="p-1 rounded text-brand-faint hover:text-brand-muted transition"
                title="Copy Payload"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-status-safe" /> : <Copy className="w-3.5 h-3.5" />}
              </button>
              <span className="text-[10px] font-mono text-brand-faint">
                {inputPayload.length} B
              </span>
            </div>
          </div>
        </div>

        {/* Preset Chips Row */}
        <div className="mt-3.5 flex flex-wrap items-center gap-2">
          <span className="text-[11px] font-mono text-brand-faint flex items-center gap-1">
            <Sparkles className="w-3 h-3 text-accent" /> Test Vectors:
          </span>
          {DEMO_PRESETS.map((preset, idx) => (
            <button
              key={idx}
              onClick={() => {
                setContentType(preset.type);
                setInputPayload(preset.value);
              }}
              className="text-xs px-2.5 py-1 rounded-md bg-surface hover:bg-surface-hover border border-surface-border hover:border-surface-borderHover text-brand-secondary hover:text-brand-text transition flex items-center space-x-1.5 font-mono group"
            >
              <span
                className={`text-[9px] px-1 py-0.2 rounded font-semibold ${
                  preset.tag === "TRUSTED"
                    ? "bg-status-safeBg text-status-safe border border-status-safeBorder"
                    : "bg-status-dangerBg text-status-danger border border-status-dangerBorder"
                }`}
              >
                {preset.method}
              </span>
              <span className="text-[11px] group-hover:text-brand-text transition-colors">
                {preset.label}
              </span>
            </button>
          ))}
        </div>

        {/* Action Row */}
        <div className="mt-4 pt-3 border-t border-surface-border flex items-center justify-between">
          <div className="text-[11px] text-brand-muted font-mono flex items-center gap-1.5">
            <span>SLA Target: &lt;50ms</span>
            <span className="text-brand-faint">•</span>
            <span>INT8 MobileBERT + Local Trie</span>
          </div>

          <div className="flex items-center space-x-3">
            <span className="text-[11px] font-mono text-brand-faint hidden sm:inline">
              Press <kbd className="px-1.5 py-0.5 rounded bg-surface border border-surface-border text-brand-muted">⌘ + ↵</kbd>
            </span>

            <button
              onClick={handleScan}
              disabled={isScanning || !inputPayload.trim()}
              className="flex items-center space-x-2 px-5 py-2 rounded-lg bg-accent hover:bg-accent-hover text-background font-semibold text-xs tracking-tight shadow-md shadow-accent/10 disabled:opacity-50 disabled:cursor-not-allowed transition active:scale-[0.98]"
            >
              <Zap className={`w-3.5 h-3.5 ${isScanning ? "animate-spin" : ""}`} />
              <span>{isScanning ? "Evaluating..." : "Evaluate Threat"}</span>
              <CornerDownLeft className="w-3 h-3 opacity-60" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
