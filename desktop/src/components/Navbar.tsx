import React from "react";
import { ShieldCheck, Plane, WifiOff } from "lucide-react";

interface NavbarProps {
  airplaneMode: boolean;
  wanBytes: number;
}

export const Navbar: React.FC<NavbarProps> = ({ airplaneMode, wanBytes }) => {
  return (
    <header className="border-b border-slate-800/80 bg-slate-950/90 backdrop-blur-md px-6 py-3.5 flex items-center justify-between sticky top-0 z-50 shadow-lg shadow-black/40">
      <div className="flex items-center space-x-3.5">
        <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-cyan-500/20 to-blue-600/30 border border-cyan-400/50 flex items-center justify-center text-cyan-400 shadow-md shadow-cyan-500/20">
          <ShieldCheck className="w-5 h-5 text-cyan-300 animate-pulse" />
        </div>
        <div>
          <div className="flex items-center space-x-2.5">
            <h1 className="text-base font-black text-white tracking-wider font-mono flex items-center gap-1.5">
              POCKET<span className="text-cyan-400">SPARROW</span>
            </h1>
            <span className="text-[10px] uppercase tracking-wider px-2 py-0.5 rounded-full bg-cyan-950/80 border border-cyan-500/40 text-cyan-300 font-bold">
              v0.1.0 MVP
            </span>
          </div>
          <p className="text-[11px] text-slate-400 flex items-center gap-1.5 mt-0.5">
            <span className="h-1.5 w-1.5 rounded-full bg-cyan-400 animate-ping inline-block"></span>
            100% On-Device Threat & Phishing Defense
          </p>
        </div>
      </div>

      <div className="flex items-center space-x-3">
        {/* Air-gap / Airplane Mode Indicator */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-emerald-950/50 border border-emerald-500/40 text-emerald-300 text-xs font-semibold shadow-inner shadow-emerald-500/10">
          <Plane className="w-3.5 h-3.5 text-emerald-400" />
          <span className="tracking-wide">
            {airplaneMode ? "AIRPLANE MODE (AIR-GAPPED)" : "100% OFFLINE ACTIVE"}
          </span>
          <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse ml-0.5"></span>
        </div>

        {/* Zero-Egress Network Counter */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700/80 text-slate-200 text-xs">
          <WifiOff className="w-3.5 h-3.5 text-cyan-400" />
          <span className="text-slate-400">WAN Egress:</span>
          <span className="font-mono font-bold text-cyan-300">{wanBytes} B (ZERO CLOUD)</span>
        </div>
      </div>
    </header>
  );
};
