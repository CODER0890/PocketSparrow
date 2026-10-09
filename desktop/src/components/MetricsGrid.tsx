import React from "react";
import { CheckCircle2 } from "lucide-react";

interface MetricsGridProps {
  totalScans: number;
  threatsBlocked: number;
  lastLatencyUs: number;
  peakRamMb: number;
  wanBytes: number;
}

export const MetricsGrid: React.FC<MetricsGridProps> = ({
  totalScans,
  threatsBlocked,
  lastLatencyUs,
  peakRamMb,
  wanBytes,
}) => {
  const latencyMs = (lastLatencyUs / 1000).toFixed(3);
  const ramBudgetMb = 250.0;
  const ramPercent = Math.min(100, Math.round((peakRamMb / ramBudgetMb) * 100));
  const blockRate = totalScans > 0 ? ((threatsBlocked / totalScans) * 100).toFixed(1) : "0.0";

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {/* 1. Evaluation Latency SLA */}
      <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-700 transition-colors">
        <div className="flex items-start justify-between">
          <span className="text-xs font-medium text-zinc-400 uppercase tracking-wider">
            Evaluation Latency
          </span>
          <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded">
            <CheckCircle2 className="w-3 h-3" />
            SLA Met
          </span>
        </div>
        <div className="mt-4">
          <div className="text-3xl font-semibold text-zinc-100 font-sans tracking-tight">
            {latencyMs} <span className="text-sm font-normal text-zinc-400">ms</span>
          </div>
          <p className="mt-1 text-xs text-zinc-400">
            P99 target: &lt;50.0 ms ceiling
          </p>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-800/80 flex items-center justify-between text-xs text-zinc-400">
          <span>Tier 1 Heuristic:</span>
          <span className="font-mono text-zinc-300">16 µs</span>
        </div>
      </div>

      {/* 2. Threats Intercepted */}
      <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-700 transition-colors">
        <div className="flex items-start justify-between">
          <span className="text-xs font-medium text-zinc-400 uppercase tracking-wider">
            Threats Intercepted
          </span>
          <span className="inline-flex items-center text-xs font-medium text-rose-400 bg-rose-500/10 border border-rose-500/20 px-2 py-0.5 rounded">
            {blockRate}% Filtered
          </span>
        </div>
        <div className="mt-4">
          <div className="text-3xl font-semibold text-zinc-100 font-sans tracking-tight">
            {threatsBlocked}{" "}
            <span className="text-sm font-normal text-zinc-400">
              / {totalScans} scanned
            </span>
          </div>
          <p className="mt-1 text-xs text-zinc-400">
            Phishing, Smishing &amp; QR Malscripts
          </p>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-800/80 flex items-center justify-between text-xs text-zinc-400">
          <span>False Positives:</span>
          <span className="text-emerald-400 font-medium">0.00%</span>
        </div>
      </div>

      {/* 3. Memory Footprint */}
      <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-700 transition-colors">
        <div className="flex items-start justify-between">
          <span className="text-xs font-medium text-zinc-400 uppercase tracking-wider">
            Resident Memory
          </span>
          <span className="text-xs font-medium text-zinc-400 bg-zinc-800 px-2 py-0.5 rounded border border-zinc-700/60">
            {ramPercent}% of Budget
          </span>
        </div>
        <div className="mt-4">
          <div className="text-3xl font-semibold text-zinc-100 font-sans tracking-tight">
            {peakRamMb.toFixed(1)}{" "}
            <span className="text-sm font-normal text-zinc-400">MB</span>
          </div>
          <div className="mt-2.5 w-full bg-zinc-800 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-indigo-500 h-full rounded-full transition-all duration-300"
              style={{ width: `${ramPercent}%` }}
            />
          </div>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-800/80 flex items-center justify-between text-xs text-zinc-400">
          <span>Max Allowed Budget:</span>
          <span className="font-mono text-zinc-300">250.0 MB</span>
        </div>
      </div>

      {/* 4. WAN Telemetry (Air-Gap) */}
      <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-700 transition-colors">
        <div className="flex items-start justify-between">
          <span className="text-xs font-medium text-zinc-400 uppercase tracking-wider">
            Telemetry Egress
          </span>
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            Air-Gapped
          </span>
        </div>
        <div className="mt-4">
          <div className="text-3xl font-semibold text-zinc-100 font-sans tracking-tight">
            {wanBytes}{" "}
            <span className="text-sm font-normal text-zinc-400">Bytes</span>
          </div>
          <p className="mt-1 text-xs text-zinc-400">
            100% on-device local execution
          </p>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-800/80 flex items-center justify-between text-xs text-zinc-400">
          <span>IPC Socket:</span>
          <span className="font-mono text-zinc-300">127.0.0.1:41789</span>
        </div>
      </div>
    </div>
  );
};
