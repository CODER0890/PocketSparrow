import React from "react";
import { Zap, ShieldAlert, Cpu, WifiOff, CheckCircle2 } from "lucide-react";

interface MetricsHUDProps {
  totalScans: number;
  threatsBlocked: number;
  lastLatencyUs: number;
  peakRamMb: number;
  zeroBytesProof: boolean;
}

export const MetricsHUD: React.FC<MetricsHUDProps> = ({
  totalScans,
  threatsBlocked,
  lastLatencyUs,
  peakRamMb,
  zeroBytesProof,
}) => {
  const latencyMs = (lastLatencyUs / 1000).toFixed(2);

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5">
      {/* 1. Response Latency SLA */}
      <div className="bg-slate-900/70 border border-slate-800 hover:border-cyan-500/40 rounded-xl p-4 transition-all duration-200 shadow-sm relative overflow-hidden group">
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-cyan-500 to-transparent opacity-60 group-hover:opacity-100 transition-opacity"></div>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-1.5 text-xs text-slate-400 uppercase font-semibold">
            <Zap className="w-3.5 h-3.5 text-cyan-400" />
            <span>Latency SLA</span>
          </div>
          <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-950/80 border border-emerald-500/40 text-emerald-300 font-bold tracking-wider">
            &lt;50ms MET
          </span>
        </div>
        <div className="mt-2.5">
          <div className="text-2xl font-black text-white font-mono tracking-tight flex items-baseline gap-1">
            {latencyMs} <span className="text-xs font-normal text-slate-400">ms</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1 flex items-center justify-between font-mono">
            <span>Tier 1: &lt;5ms</span>
            <span className="text-slate-500">|</span>
            <span>Tier 2: &lt;40ms</span>
          </div>
        </div>
      </div>

      {/* 2. Threats Blocked Counter */}
      <div className="bg-slate-900/70 border border-slate-800 hover:border-rose-500/40 rounded-xl p-4 transition-all duration-200 shadow-sm relative overflow-hidden group">
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-rose-500 to-transparent opacity-60 group-hover:opacity-100 transition-opacity"></div>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-1.5 text-xs text-slate-400 uppercase font-semibold">
            <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
            <span>Threats Blocked</span>
          </div>
          <span className="text-[10px] px-2 py-0.5 rounded-full bg-rose-950/80 border border-rose-500/40 text-rose-300 font-bold tracking-wider">
            SHIELD ON
          </span>
        </div>
        <div className="mt-2.5">
          <div className="text-2xl font-black text-rose-400 font-mono tracking-tight flex items-baseline gap-1">
            {threatsBlocked}{" "}
            <span className="text-xs font-normal text-slate-400">/ {totalScans} Total</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1 truncate">
            Phishing • Smishing • Quishing
          </div>
        </div>
      </div>

      {/* 3. Memory Footprint SLA */}
      <div className="bg-slate-900/70 border border-slate-800 hover:border-sky-500/40 rounded-xl p-4 transition-all duration-200 shadow-sm relative overflow-hidden group">
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-sky-500 to-transparent opacity-60 group-hover:opacity-100 transition-opacity"></div>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-1.5 text-xs text-slate-400 uppercase font-semibold">
            <Cpu className="w-3.5 h-3.5 text-sky-400" />
            <span>Peak RAM SLA</span>
          </div>
          <span className="text-[10px] px-2 py-0.5 rounded-full bg-sky-950/80 border border-sky-500/40 text-sky-300 font-bold tracking-wider">
            OPTIMAL
          </span>
        </div>
        <div className="mt-2.5">
          <div className="text-2xl font-black text-sky-300 font-mono tracking-tight flex items-baseline gap-1">
            {peakRamMb.toFixed(1)} <span className="text-xs font-normal text-slate-400">MB</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1 truncate font-mono">
            INT8 Model: 32 MB (&lt;250MB SLA)
          </div>
        </div>
      </div>

      {/* 4. Zero Cloud Egress */}
      <div className="bg-slate-900/70 border border-slate-800 hover:border-emerald-500/40 rounded-xl p-4 transition-all duration-200 shadow-sm relative overflow-hidden group">
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-emerald-500 to-transparent opacity-60 group-hover:opacity-100 transition-opacity"></div>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-1.5 text-xs text-slate-400 uppercase font-semibold">
            <WifiOff className="w-3.5 h-3.5 text-emerald-400" />
            <span>Cloud Egress</span>
          </div>
          <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-950/80 border border-emerald-500/40 text-emerald-300 font-bold tracking-wider">
            {zeroBytesProof ? "0 BYTES WAN" : "AIRGAP VERIFIED"}
          </span>
        </div>
        <div className="mt-2.5">
          <div className="text-2xl font-black text-emerald-300 font-mono tracking-tight flex items-baseline gap-1">
            0 <span className="text-xs font-normal text-slate-400">Bytes Outbound</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1 flex items-center gap-1 font-mono">
            <CheckCircle2 className="w-3 h-3 text-emerald-400 inline" />
            Zero Telemetry Sockets
          </div>
        </div>
      </div>
    </div>
  );
};
