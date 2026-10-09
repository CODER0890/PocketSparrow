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
import { motion, useReducedMotion } from "framer-motion";
import { ScanResultPayload } from "./XaiDrawer";
import { MOTION_EASING, MOTION_DURATION } from "../styles/motion";

interface ThreatInspectorProps {
  onScan: (type: "Url" | "SmsText" | "QrPayload", payload: string) => Promise<ScanResultPayload>;
  selectedType?: "Url" | "SmsText" | "QrPayload";
  initialPayload?: string;
}

export const ThreatInspector: React.FC<ThreatInspectorProps> = ({
  onScan,
  selectedType,
  initialPayload,
}) => {
  const shouldReduceMotion = !!useReducedMotion();
  const [contentType, setContentType] = useState<"Url" | "SmsText" | "QrPayload">(
    selectedType || "Url"
  );
  const [inputPayload, setInputPayload] = useState(
    initialPayload || "https://secure-p\u0430ypal.com/verify-account?token=9281a4b"
  );
  const [isScanning, setIsScanning] = useState(false);
  const [copied, setCopied] = useState(false);

  React.useEffect(() => {
    if (selectedType) setContentType(selectedType);
  }, [selectedType]);

  React.useEffect(() => {
    if (initialPayload !== undefined) setInputPayload(initialPayload);
  }, [initialPayload]);

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
    <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 space-y-5 transition-colors shadow-sm dark:shadow-none">
      {/* Header and Type Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-zinc-900 dark:text-zinc-100 tracking-tight">
            Payload Inspector
          </h2>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Run on-device heuristic and neural model evaluations against untrusted vectors.
          </p>
        </div>

        {/* Vector Tabs */}
        <div className="inline-flex p-1 rounded-md bg-zinc-100 dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 self-start sm:self-auto text-xs">
          <button
            onClick={() => {
              setContentType("Url");
              setInputPayload("https://secure-p\u0430ypal.com/verify-account?token=9281a4b");
            }}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded transition-colors ${
              contentType === "Url"
                ? "bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium shadow-sm"
                : "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200"
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
                ? "bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium shadow-sm"
                : "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200"
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
                ? "bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium shadow-sm"
                : "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200"
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>QR Code</span>
          </button>
        </div>
      </div>

      {/* Input Container with Soft Glow Focus & Scanning Radar Sweep */}
      <div className="relative group rounded-md overflow-hidden focus-within:ring-2 focus-within:ring-indigo-500/20 focus-within:border-indigo-500/60 transition-all duration-200 border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950">
        {/* Sleek Scanning Sweep Radar Line */}
        {isScanning && !shouldReduceMotion && (
          <motion.div
            className="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-indigo-500 to-transparent z-10"
            initial={{ x: "-100%" }}
            animate={{ x: "100%" }}
            transition={{
              duration: 0.6,
              repeat: Infinity,
              ease: "easeInOut",
            }}
          />
        )}

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
          className="w-full bg-transparent p-3.5 text-sm font-mono text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-600 focus:outline-none transition-colors resize-none leading-relaxed"
        />

        <div className="absolute right-3 bottom-3 flex items-center space-x-2">
          <button
            onClick={handleCopy}
            className="p-1 rounded text-zinc-400 hover:text-zinc-600 dark:text-zinc-500 dark:hover:text-zinc-300 transition-colors"
            title="Copy payload"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
          <span className="text-xs font-mono text-zinc-400 dark:text-zinc-600">
            {inputPayload.length} B
          </span>
        </div>
      </div>

      {/* Quick Test Vectors */}
      <div className="flex flex-wrap items-center gap-2 pt-1">
        <span className="text-xs text-zinc-500 dark:text-zinc-400 mr-1">Sample vectors:</span>
        {DEMO_PRESETS.map((preset, idx) => (
          <motion.button
            key={idx}
            whileHover={{ y: -1 }}
            whileTap={{ y: 0 }}
            transition={{ duration: MOTION_DURATION.micro, ease: MOTION_EASING }}
            onClick={() => {
              setContentType(preset.type);
              setInputPayload(preset.value);
            }}
            className="text-xs px-2.5 py-1 rounded bg-zinc-50 hover:bg-zinc-100 border border-zinc-200 text-zinc-700 dark:bg-zinc-900 dark:hover:bg-zinc-800 dark:border-zinc-800 dark:text-zinc-300 dark:hover:text-zinc-100 transition-colors"
          >
            {preset.label}
          </motion.button>
        ))}
      </div>

      {/* Action Row */}
      <div className="pt-4 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
        <div className="text-xs text-zinc-500 dark:text-zinc-400">
          Engine: Tier 1 Heuristics &amp; MobileBERT INT8
        </div>

        <div className="flex items-center space-x-3">
          <span className="text-xs text-zinc-400 dark:text-zinc-500 hidden sm:inline">
            Press <kbd className="px-1.5 py-0.5 rounded bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 font-mono">⌘ + ↵</kbd>
          </span>

          {/* Evaluate Button: micro-scale on hover and tap */}
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            transition={{ duration: MOTION_DURATION.micro, ease: MOTION_EASING }}
            onClick={handleScan}
            disabled={isScanning || !inputPayload.trim()}
            className="flex items-center space-x-2 px-4 py-2 rounded-md bg-zinc-900 hover:bg-zinc-800 text-white dark:bg-zinc-100 dark:hover:bg-zinc-200 dark:text-zinc-900 font-medium text-xs transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isScanning ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Search className="w-3.5 h-3.5" />
            )}
            <span>{isScanning ? "Evaluating..." : "Evaluate Threat"}</span>
          </motion.button>
        </div>
      </div>
    </div>
  );
};
