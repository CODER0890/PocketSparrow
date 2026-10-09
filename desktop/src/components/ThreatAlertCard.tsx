import React from "react";

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
      className={`rounded-xl border p-5 transition-all ${
        isMalicious
          ? "bg-rose-950/40 border-rose-600/80 glow-danger"
          : isSuspicious
          ? "bg-amber-950/40 border-amber-600/80"
          : "bg-emerald-950/30 border-emerald-700/60"
      }`}
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center space-x-3">
          <div
            className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-sm ${
              isMalicious
                ? "bg-rose-500 text-white"
                : isSuspicious
                ? "bg-amber-500 text-black"
                : "bg-emerald-500 text-white"
            }`}
          >
            {isMalicious ? "!" : isSuspicious ? "?" : "✓"}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span
                className={`text-xs font-bold uppercase tracking-wider px-2 py-0.5 rounded ${
                  isMalicious
                    ? "bg-rose-900/80 text-rose-200 border border-rose-700"
                    : isSuspicious
                    ? "bg-amber-900/80 text-amber-200 border border-amber-700"
                    : "bg-emerald-900/80 text-emerald-200 border border-emerald-700"
                }`}
              >
                {result.verdict.toUpperCase()} THREAT
              </span>
              <span className="text-xs text-slate-400">
                Category: <strong className="text-white">{result.category}</strong>
              </span>
              <span className="text-xs text-slate-400">
                Confidence: <strong className="text-cyan-400">{confPct}%</strong>
              </span>
            </div>
            <h3 className="text-base font-bold text-white mt-1">
              {isMalicious
                ? "Malicious Threat Intercepted & Blocked"
                : isSuspicious
                ? "Suspicious Content Flagged for Review"
                : "Content Passed All On-Device Checks"}
            </h3>
          </div>
        </div>

        {onDismiss && (
          <button
            onClick={onDismiss}
            className="text-slate-400 hover:text-white text-xs px-2 py-1 rounded bg-slate-800/60 hover:bg-slate-700"
          >
            Dismiss
          </button>
        )}
      </div>

      {/* Plain-English XAI Rationale */}
      <div className="mt-4 pt-3 border-t border-slate-800/80">
        <div className="text-xs text-slate-400 font-semibold mb-1">
          EXPLAINABLE AI (XAI) DIAGNOSIS:
        </div>
        <p className="text-sm text-slate-200 leading-relaxed font-sans">
          {result.xai_reason}
        </p>
      </div>

      {/* Engine Metrics Tag */}
      <div className="mt-3 flex items-center justify-between text-[11px] text-slate-400 bg-slate-950/60 px-3 py-1.5 rounded-lg border border-slate-800/60">
        <div className="flex items-center space-x-3">
          <span>
            Engine:{" "}
            <strong className="text-cyan-400">
              {result.tier_triggered === "Tier1Heuristic"
                ? "Tier 1 Heuristic (<5ms)"
                : "Tier 2 INT8 Transformer (<40ms)"}
            </strong>
          </span>
          <span>•</span>
          <span>
            Latency: <strong className="text-white">{(result.latency_us / 1000).toFixed(2)} ms</strong>
          </span>
        </div>
        <div>
          Action:{" "}
          <strong className={result.should_block ? "text-rose-400" : "text-emerald-400"}>
            {result.should_block ? "IMMEDIATE BLOCK RECOMMENDED" : "ALLOW PROCEED"}
          </strong>
        </div>
      </div>
    </div>
  );
};
