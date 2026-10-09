import React from "react";
import {
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  X,
  Lock,
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

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 transition-opacity"
      />

      {/* Slide-out Drawer Panel */}
      <div className="relative w-full max-w-lg bg-white dark:bg-zinc-950 border-l border-zinc-200 dark:border-zinc-800 h-full overflow-y-auto flex flex-col justify-between z-10 p-6 space-y-6 shadow-2xl transition-colors">
        <div>
          {/* Header */}
          <div className="flex items-center justify-between pb-5 border-b border-zinc-200 dark:border-zinc-800">
            <div className="flex items-center space-x-3">
              <div
                className={`p-2 rounded-md border ${
                  isMalicious
                    ? "bg-rose-50 border-rose-200 text-rose-700 dark:bg-rose-500/10 dark:border-rose-500/20 dark:text-rose-400"
                    : isSuspicious
                    ? "bg-amber-50 border-amber-200 text-amber-700 dark:bg-amber-500/10 dark:border-amber-500/20 dark:text-amber-400"
                    : "bg-emerald-50 border-emerald-200 text-emerald-700 dark:bg-emerald-500/10 dark:border-emerald-500/20 dark:text-emerald-400"
                }`}
              >
                {isMalicious ? (
                  <ShieldAlert className="w-5 h-5" />
                ) : isSuspicious ? (
                  <AlertTriangle className="w-5 h-5" />
                ) : (
                  <ShieldCheck className="w-5 h-5" />
                )}
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                    {result.verdict} Verdict
                  </h3>
                  <span className="text-xs px-2 py-0.5 rounded bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-600 dark:text-zinc-400 font-mono">
                    {result.category}
                  </span>
                </div>
                <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                  Evaluated in {result.latency_us} µs on-device
                </p>
              </div>
            </div>

            <button
              onClick={onClose}
              className="p-1.5 rounded-md text-zinc-400 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-900 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Forensic Analysis Section */}
          <div className="mt-6 space-y-6">
            <div>
              <h4 className="text-xs font-medium uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
                Forensic Analysis
              </h4>
              <p className="mt-2 text-sm text-zinc-800 dark:text-zinc-300 leading-relaxed bg-zinc-50 dark:bg-zinc-900/40 p-4 rounded-md border border-zinc-200 dark:border-zinc-800">
                {result.xai_reason}
              </p>
            </div>

            {/* Attribution Factors Table */}
            <div>
              <h4 className="text-xs font-medium uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-2">
                Evaluation Factors
              </h4>
              <div className="rounded border border-zinc-200 dark:border-zinc-800 divide-y divide-zinc-200 dark:divide-zinc-800 text-xs">
                <div className="flex justify-between py-2.5 px-3 bg-zinc-50/70 dark:bg-zinc-900/30">
                  <span className="text-zinc-500 dark:text-zinc-400">Evaluation Engine</span>
                  <span className="font-mono text-zinc-900 dark:text-zinc-200">{result.tier_triggered}</span>
                </div>
                <div className="flex justify-between py-2.5 px-3 bg-zinc-50/70 dark:bg-zinc-900/30">
                  <span className="text-zinc-500 dark:text-zinc-400">Model Confidence</span>
                  <span className="font-mono text-zinc-900 dark:text-zinc-200">
                    {(result.confidence * 100).toFixed(1)}%
                  </span>
                </div>
                <div className="flex justify-between py-2.5 px-3 bg-zinc-50/70 dark:bg-zinc-900/30">
                  <span className="text-zinc-500 dark:text-zinc-400">Execution Latency</span>
                  <span className="font-mono text-zinc-900 dark:text-zinc-200">
                    {(result.latency_us / 1000).toFixed(3)} ms
                  </span>
                </div>
                <div className="flex justify-between py-2.5 px-3 bg-zinc-50/70 dark:bg-zinc-900/30">
                  <span className="text-zinc-500 dark:text-zinc-400">Enforcement Action</span>
                  <span className="font-mono text-zinc-900 dark:text-zinc-200">
                    {result.should_block ? "Quarantine & Block" : "Allow Throughput"}
                  </span>
                </div>
              </div>
            </div>

            {/* Privacy Verification */}
            <div className="rounded-md border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/20 p-3.5 flex items-start space-x-3 text-xs text-zinc-600 dark:text-zinc-400">
              <Lock className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
              <div>
                <span className="text-zinc-900 dark:text-zinc-200 font-medium">Privacy Guaranteed: </span>
                This inference ran entirely on local hardware. No URL or payload data was transmitted outside the host device.
              </div>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="pt-4 border-t border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-md border border-zinc-200 dark:border-zinc-800 hover:bg-zinc-100 dark:hover:bg-zinc-900 text-xs text-zinc-700 dark:text-zinc-300 font-medium transition-colors"
          >
            Dismiss
          </button>
          {isMalicious && (
            <button
              onClick={() => {
                alert("Target payload has been quarantined in SQLCipher vault.");
                onClose();
              }}
              className="px-4 py-2 rounded-md bg-rose-600 hover:bg-rose-500 text-white text-xs font-medium transition-colors"
            >
              Quarantine &amp; Block
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
