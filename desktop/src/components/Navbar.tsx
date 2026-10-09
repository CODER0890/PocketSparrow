import React from "react";

interface NavbarProps {
  airplaneMode: boolean;
  wanBytes: number;
}

export const Navbar: React.FC<NavbarProps> = ({ airplaneMode, wanBytes }) => {
  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur px-6 py-4 flex items-center justify-between sticky top-0 z-50">
      <div className="flex items-center space-x-3">
        <div className="h-9 w-9 rounded-lg bg-cyan-500/10 border border-cyan-500/40 flex items-center justify-center text-cyan-400 font-bold text-lg">
          PS
        </div>
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="text-lg font-bold text-white tracking-wide">POCKET SPARROW</h1>
            <span className="text-xs px-2 py-0.5 rounded bg-cyan-950 border border-cyan-800 text-cyan-400 font-semibold">
              v0.1.0 MVP
            </span>
          </div>
          <p className="text-xs text-slate-400">100% On-Device Threat & Phishing Defense</p>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        {/* Air-gap / Airplane Mode Status */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-md bg-emerald-950/60 border border-emerald-800/80 text-emerald-400 text-xs font-semibold">
          <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span>{airplaneMode ? "AIRPLANE MODE (AIR-GAPPED)" : "100% OFFLINE ACTIVE"}</span>
        </div>

        {/* Zero-Egress Network Counter */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-md bg-slate-950 border border-slate-800 text-slate-300 text-xs">
          <span className="text-slate-500">WAN Egress:</span>
          <span className="font-bold text-cyan-400">{wanBytes} B (ZERO TELEMETRY)</span>
        </div>
      </div>
    </header>
  );
};
