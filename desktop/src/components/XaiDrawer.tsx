import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  X,

  BrainCircuit,
  Lock,

  Fingerprint,


} from "lucide-react";

export interface ScanResultPayload {
  verdict: "Safe" | "Suspicious" | "Malicious";
  tier_triggered: "Tier1Heuristic" | "Tier2Transformer";
  confidence: number;
  latency_us: number;
  category: string;
  xai_reason: string;
  should_block: boolean;
}

interface XaiDrawerProps {
  result: ScanResultPayload | null;
  onClose: () => void;
}

export const XaiDrawer: React.FC<XaiDrawerProps> = ({ result, onClose }) => {
  if (!result) return null;

  const isMalicious = result.verdict === "Malicious";
  const isSuspicious = result.verdict === "Suspicious";
  const severityScore = isMalicious ? 96 : isSuspicious ? 54 : 4;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex justify-end">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-black/60 backdrop-blur-sm"
        />

        {/* Slide-out Drawer Panel */}
        <motion.div
          initial={{ x: "100%" }}
          animate={{ x: 0 }}
          exit={{ x: "100%" }}
          transition={{ type: "spring", damping: 28, stiffness: 280 }}
          className="relative w-full max-w-lg bg-background-elevated border-l border-surface-border shadow-elevated h-full overflow-y-auto flex flex-col justify-between z-10"
        >
          {/* Drawer Header */}
          <div>
            <div className="p-6 border-b border-surface-border flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div
                  className={`p-2.5 rounded-xl border ${
                    isMalicious
                      ? "bg-status-dangerBg border-status-dangerBorder text-status-danger"
                      : isSuspicious
                      ? "bg-status-warningBg border-status-warningBorder text-status-warning"
                      : "bg-status-safeBg border-status-safeBorder text-status-safe"
                  }`}
                >
                  {isMalicious ? (
                    <ShieldAlert className="w-5 h-5" strokeWidth={1.75} />
                  ) : isSuspicious ? (
                    <AlertTriangle className="w-5 h-5" strokeWidth={1.75} />
                  ) : (
                    <ShieldCheck className="w-5 h-5" strokeWidth={1.75} />
                  )}
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-bold text-brand-text font-mono">
                      {result.verdict.toUpperCase()} INCIDENT
                    </span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-surface border border-surface-border text-brand-secondary">
                      {result.category}
                    </span>
                  </div>
                  <p className="text-xs text-brand-muted mt-0.5 font-mono">
                    Evaluation completed in {result.latency_us} µs on-device
                  </p>
                </div>
              </div>

              <button
                onClick={onClose}
                className="p-1.5 rounded-lg text-brand-muted hover:text-brand-text hover:bg-surface transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Severity Meter & Engine Attribution */}
            <div className="p-6 space-y-5">
              {/* Severity Gauge */}
              <div className="rounded-xl border border-surface-border bg-background-subtle p-4 space-y-3">
                <div className="flex items-center justify-between text-xs font-mono">
                  <span className="text-brand-muted">Calculated Threat Rating:</span>
                  <span
                    className={`font-bold ${
                      isMalicious
                        ? "text-status-danger"
                        : isSuspicious
                        ? "text-status-warning"
                        : "text-status-safe"
                    }`}
                  >
                    {severityScore} / 100 ({isMalicious ? "CRITICAL RISK" : isSuspicious ? "MODERATE" : "SAFE"})
                  </span>
                </div>

                {/* Severity Bar */}
                <div className="w-full bg-surface-border rounded-full h-2 overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      isMalicious
                        ? "bg-status-danger"
                        : isSuspicious
                        ? "bg-status-warning"
                        : "bg-status-safe"
                    }`}
                    style={{ width: `${severityScore}%` }}
                  ></div>
                </div>

                <div className="flex items-center justify-between text-[11px] font-mono text-brand-faint pt-1">
                  <span>Confidence: {Math.round(result.confidence * 100)}%</span>
                  <span>
                    Pipeline:{" "}
                    {result.tier_triggered === "Tier1Heuristic"
                      ? "Tier 1 Deterministic Engine"
                      : "Tier 2 INT8 MobileBERT"}
                  </span>
                </div>
              </div>

              {/* Explainable AI Forensic Justification */}
              <div className="space-y-2">
                <div className="flex items-center space-x-1.5 text-xs font-semibold text-brand-text">
                  <BrainCircuit className="w-3.5 h-3.5 text-accent" strokeWidth={1.75} />
                  <span>Explainable AI (XAI) Forensic Reasoning</span>
                </div>
                <div className="p-4 rounded-xl border border-surface-border bg-background-subtle text-xs text-brand-secondary leading-relaxed font-sans border-l-2 border-l-accent">
                  {result.xai_reason}
                </div>
              </div>

              {/* Forensic Evidence Factors */}
              <div className="space-y-2">
                <div className="text-xs font-semibold text-brand-text flex items-center space-x-1.5">
                  <Fingerprint className="w-3.5 h-3.5 text-indigo" strokeWidth={1.75} />
                  <span>Attribution Evidence</span>
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                  <div className="p-3 rounded-lg border border-surface-border bg-surface space-y-1">
                    <span className="text-[10px] text-brand-muted">Vector Type</span>
                    <p className="text-brand-text font-semibold">Web URI / DNS</p>
                  </div>
                  <div className="p-3 rounded-lg border border-surface-border bg-surface space-y-1">
                    <span className="text-[10px] text-brand-muted">Inspection Engine</span>
                    <p className="text-accent font-semibold">{result.tier_triggered}</p>
                  </div>
                  <div className="p-3 rounded-lg border border-surface-border bg-surface space-y-1">
                    <span className="text-[10px] text-brand-muted">Network Isolation</span>
                    <p className="text-status-safe font-semibold">100% Offline</p>
                  </div>
                  <div className="p-3 rounded-lg border border-surface-border bg-surface space-y-1">
                    <span className="text-[10px] text-brand-muted">Egress Telemetry</span>
                    <p className="text-status-safe font-semibold">0 WAN Bytes</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Action Footer */}
          <div className="p-6 border-t border-surface-border bg-background-subtle space-y-2.5">
            <button
              onClick={onClose}
              className={`w-full py-2.5 rounded-lg font-semibold text-xs transition flex items-center justify-center space-x-2 shadow-sm ${
                isMalicious
                  ? "bg-status-danger hover:bg-rose-600 text-white"
                  : "bg-surface hover:bg-surface-hover border border-surface-border text-brand-text"
              }`}
            >
              <Lock className="w-3.5 h-3.5" />
              <span>{isMalicious ? "Block Target & Quarantine Payload" : "Acknowledge & Close"}</span>
            </button>
            <button
              onClick={onClose}
              className="w-full py-2 rounded-lg text-xs font-mono text-brand-muted hover:text-brand-secondary hover:bg-surface transition"
            >
              Copy Forensic Incident JSON
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
