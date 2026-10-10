import React, { useState } from "react";
import {
  Globe,
  MessageSquare,
  QrCode,
  Copy,
  Check,
  Search,
  Loader2,
  FileLock,
} from "lucide-react";
import { motion, useReducedMotion } from "framer-motion";
import { ScanResultPayload } from "./XaiDrawer";
import { ThreatDnaVisualizer } from "./ThreatDnaVisualizer";
import { ForensicExportModal } from "./ForensicExportModal";
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
  const [inputPayload, setInputPayload] = useState(initialPayload || "");
  const [isScanning, setIsScanning] = useState(false);
  const [copied, setCopied] = useState(false);
  const [lastResult, setLastResult] = useState<ScanResultPayload | null>(null);
  const [showExportModal, setShowExportModal] = useState(false);

  React.useEffect(() => {
    if (selectedType) setContentType(selectedType);
  }, [selectedType]);

  React.useEffect(() => {
    if (initialPayload !== undefined) setInputPayload(initialPayload);
  }, [initialPayload]);

  const handleScan = async () => {
    if (!inputPayload.trim() || isScanning) return;
    setIsScanning(true);
    try {
      const res = await onScan(contentType, inputPayload);
      setLastResult(res);
    } finally {
      setIsScanning(false);
    }
  };

  const handleCopy = () => {
    if (!inputPayload) return;
    navigator.clipboard.writeText(inputPayload);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getPlaceholder = () => {
    switch (contentType) {
      case "Url":
        return "Enter suspicious URL or domain to evaluate (e.g., https://...)...";
      case "SmsText":
        return "Paste incoming text message or payload to inspect for social engineering...";
      case "QrPayload":
        return "Enter decoded QR payload, data URI, or executable scheme (e.g., javascript:, smsto:)...";
    }
  };

  return (
    <div
      id="threat-inspector-card"
      className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 space-y-5 transition-colors shadow-sm dark:shadow-none"
    >
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

        <div className="flex flex-wrap items-center gap-2.5 self-start sm:self-auto">
          {/* Vector Tabs */}
          <div className="inline-flex p-1 rounded-md bg-zinc-100 dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 text-xs">
            <button
              onClick={() => setContentType("Url")}
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
              onClick={() => setContentType("SmsText")}
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
              onClick={() => setContentType("QrPayload")}
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

          {/* Module 3: Export Forensic Report Button */}
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => setShowExportModal(true)}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-md text-xs font-medium border border-zinc-200 dark:border-zinc-800 bg-zinc-50 hover:bg-zinc-100 dark:bg-zinc-900/60 dark:hover:bg-zinc-800 text-zinc-700 dark:text-zinc-300 transition-colors shadow-2xs"
            title="Export offline AES-256 encrypted forensic audit archive"
          >
            <FileLock className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
            <span>Export Forensic Report</span>
          </motion.button>
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
          id="threat-inspector-input"
          rows={3}
          value={inputPayload}
          onChange={(e) => setInputPayload(e.target.value)}
          onKeyDown={(e) => {
            if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
              handleScan();
            }
          }}
          placeholder={getPlaceholder()}
          className="w-full bg-transparent p-3.5 text-sm font-mono text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-600 focus:outline-none transition-colors resize-none leading-relaxed"
        />

        <div className="absolute right-3 bottom-3 flex items-center space-x-2">
          {inputPayload ? (
            <button
              onClick={handleCopy}
              className="p-1 rounded text-zinc-400 hover:text-zinc-600 dark:text-zinc-500 dark:hover:text-zinc-300 transition-colors"
              title="Copy payload"
            >
              {copied ? (
                <Check className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
              ) : (
                <Copy className="w-3.5 h-3.5" />
              )}
            </button>
          ) : null}
          <span className="text-xs font-mono text-zinc-400 dark:text-zinc-600">
            {new TextEncoder().encode(inputPayload).length} B
          </span>
        </div>
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

      {/* Module 1: Threat DNA Attention Heatmap Visualizer */}
      {inputPayload.trim() && (
        <ThreatDnaVisualizer
          payload={inputPayload}
          category={lastResult?.category}
        />
      )}

      {/* Module 3: Offline Encrypted Forensic Export Modal */}
      <ForensicExportModal
        isOpen={showExportModal}
        onClose={() => setShowExportModal(false)}
        payload={inputPayload || "https://example.test"}
        verdict={lastResult?.verdict}
        category={lastResult?.category}
        xaiReason={lastResult?.xai_reason}
        latencyUs={lastResult?.latency_us}
      />
    </div>
  );
};

export default ThreatInspector;
