import React from "react";
import { AlertOctagon, AlertTriangle, CheckCircle, Zap, BrainCircuit, X } from "lucide-react";

export interface ScanResultPayload {
  verdict: "Safe" | "Suspicious" | "Malicious";
  tier_triggered: "Tier1Heuristic" | "Tier2Transformer";
  confidence: number;
  latency_us: number;
  category: string;
  xai_reason: string;
  should_block: boolean;
}

interface ThreatAlertCardProps {
  result: ScanResultPayload | null;
  onDismiss?: () => void;
}

export const ThreatAlertCard: React.FC<ThreatAlertCardProps> = ({ result, onDismiss }) => {
  if (!result) return null;

  const isMalicious = result.verdict === "Malicious";
  const isSuspicious = result.verdict === "Suspicious";
  const confPct = Math.round(result.confidence * 100);

  return (
    <div
      className={`rounded-2xl border p-5 transition-all shadow-2xl relative overflow-hidden backdrop-blur-md animate-in fade-in slide-in-from-top-4 duration-300 ${
        isMalicious
          ? "bg-rose-950/40 border-rose-500/80 shadow-rose-950/50"
          : isSuspicious
          ? "bg-amber-950/40 border-amber-500/80 shadow-amber-950/50"
          : "bg-emerald-950/40 border-emerald-500/70 shadow-emerald-950/50"
      }`}
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center space-x-3.5">
          <div
            className={`h-11 w-11 rounded-xl flex items-center justify-center font-bold shadow-lg ${
              isMalicious
                ? "bg-rose-600/30 border border-rose-500 text-rose-300 shadow-rose-600/20"
                : isSuspicious
                ? "bg-amber-600/30 border border-amber-500 text-amber-300 shadow-amber-600/20"
                : "bg-emerald-600/30 border border-emerald-500 text-emerald-300 shadow-emerald-600/20"
            }`}
          >
            {isMalicious ? (
              <AlertOctagon className="w-6 h-6 text-rose-400 animate-bounce" />
            ) : isSuspicious ? (
              <AlertTriangle className="w-6 h-6 text-amber-400" />
            ) : (
              <CheckCircle className="w-6 h-6 text-emerald-400" />
            )}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span
                className={`text-xs font-black uppercase tracking-wider px-2.5 py-0.5 rounded-full border ${
                  isMalicious
                    ? "bg-rose-900/80 text-rose-200 border-rose-500"
                    : isSuspicious
                    ? "bg-amber-900/80 text-amber-200 border-amber-500"
                    : "bg-emerald-900/80 text-emerald-200 border-emerald-500"
                }`}
              >
                {result.verdict.toUpperCase()} VERDICT
              </span>
              <span className="text-xs px-2 py-0.5 rounded-md bg-slate-900/80 border border-slate-700 text-slate-300 font-mono">
                {result.category}
              </span>
            </div>
            <p className="text-xs text-slate-300 font-mono mt-1">
              Confidence: <span className="font-bold text-white">{confPct}%</span> • SLA Latency:{" "}
              <span className="font-bold text-cyan-300">{result.latency_us} µs</span>
            </p>
          </div>
        </div>

        {onDismiss && (
          <button
            onClick={onDismiss}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Forensic XAI Explanation Box */}
      <div className="mt-4 p-4 rounded-xl bg-slate-950/80 border border-slate-800/90 shadow-inner">
        <div className="flex items-center space-x-2 text-xs font-semibold text-slate-300 mb-1.5 font-mono">
          <BrainCircuit className="w-3.5 h-3.5 text-cyan-400" />
          <span>Explainable AI (XAI) Forensic Breakdown:</span>
        </div>
        <p className="text-xs text-slate-200 leading-relaxed font-sans pl-5 border-l-2 border-cyan-500/60 my-2">
          {result.xai_reason}
        </p>
        <div className="flex items-center justify-between text-[11px] text-slate-400 pt-2 border-t border-slate-900 font-mono">
          <span className="flex items-center gap-1.5">
            <Zap className="w-3 h-3 text-cyan-400" />
            Pipeline:{" "}
            <span className="text-slate-300">
              {result.tier_triggered === "Tier1Heuristic"
                ? "Tier 1 Deterministic Engine"
                : "Tier 2 INT8 MobileBERT"}
            </span>
          </span>
          <span
            className={`font-semibold ${
              result.should_block ? "text-rose-400" : "text-emerald-400"
            }`}
          >
            {result.should_block ? "ACTION: BLOCKED ON-DEVICE" : "ACTION: PASSED (SAFE)"}
          </span>
        </div>
      </div>
    </div>
  );
};
