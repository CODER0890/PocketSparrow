import React from "react";
import { Activity, Terminal, AlertTriangle, CheckCircle2, RotateCcw } from "lucide-react";

export interface ProcessAuditItem {
  pid: number;
  name: string;
  path: string;
  is_suspicious: boolean;
  is_system?: boolean;
  threat_detail: string;
}

interface ProcessAuditorProps {
  processes: ProcessAuditItem[];
  onRefresh: () => void;
}

export const ProcessAuditor: React.FC<ProcessAuditorProps> = ({ processes, onRefresh }) => {
  const suspiciousCount = processes.filter((p) => p.is_suspicious).length;

  return (
    <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div>
          <div className="flex items-center space-x-2.5">
            <Activity className="w-4 h-4 text-cyan-400" />
            <h2 className="text-base font-bold text-white tracking-wide font-mono">
              Process Behavior Auditor
            </h2>
            <span
              className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold uppercase tracking-wider border ${
                suspiciousCount > 0
                  ? "bg-rose-950 text-rose-300 border-rose-500/60 animate-pulse"
                  : "bg-emerald-950 text-emerald-300 border-emerald-500/60"
              }`}
            >
              {suspiciousCount > 0 ? `${suspiciousCount} RISKY PROCESSES` : "CLEAN"}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Monitors process table for masquerading binaries, fake browser wrappers, and unauthorized listeners.
          </p>
        </div>

        <button
          onClick={onRefresh}
          className="flex items-center space-x-1.5 text-xs px-3.5 py-1.5 rounded-xl bg-slate-950 hover:bg-slate-800 text-slate-200 border border-slate-700/80 hover:border-slate-600 transition shadow-sm font-mono self-start sm:self-auto"
        >
          <RotateCcw className="w-3.5 h-3.5 text-cyan-400" />
          <span>Rescan Processes</span>
        </button>
      </div>

      <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
        {processes.length === 0 ? (
          <div className="text-xs text-slate-500 py-6 text-center font-mono">
            No active process warnings found.
          </div>
        ) : (
          processes.map((proc) => (
            <div
              key={proc.pid}
              className={`p-3 rounded-xl border text-xs flex flex-col md:flex-row md:items-center justify-between gap-2 transition ${
                proc.is_suspicious
                  ? "bg-rose-950/30 border-rose-600/70 text-rose-200 shadow-md shadow-rose-950/30"
                  : "bg-slate-950/70 border-slate-800 text-slate-300 hover:border-slate-700"
              }`}
            >
              <div className="flex items-start md:items-center space-x-3">
                <div
                  className={`p-1.5 rounded-lg border ${
                    proc.is_suspicious
                      ? "bg-rose-900/60 border-rose-500 text-rose-300"
                      : "bg-slate-900 border-slate-700 text-slate-400"
                  }`}
                >
                  <Terminal className="w-3.5 h-3.5" />
                </div>
                <div>
                  <div className="flex items-center space-x-2 font-mono">
                    <span className="font-bold text-white text-xs">{proc.name}</span>
                    <span className="text-[10px] text-slate-500">PID: {proc.pid}</span>
                  </div>
                  <div className="text-[11px] text-slate-400 font-mono truncate max-w-sm sm:max-w-md">
                    {proc.path}
                  </div>
                </div>
              </div>

              <div className="flex items-center space-x-2 self-end md:self-auto font-mono text-[11px]">
                {proc.is_suspicious ? (
                  <div className="flex items-center space-x-1 text-rose-400 font-semibold bg-rose-950/80 px-2 py-0.5 rounded-md border border-rose-800">
                    <AlertTriangle className="w-3 h-3 text-rose-400" />
                    <span>{proc.threat_detail}</span>
                  </div>
                ) : (
                  <div className="flex items-center space-x-1 text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded-md border border-emerald-900">
                    <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                    <span>Verified Benign</span>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
