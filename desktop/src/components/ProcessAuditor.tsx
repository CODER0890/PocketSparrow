import React from "react";

export interface ProcessAuditItem {
  pid: number;
  name: string;
  path: string;
  is_suspicious: boolean;
  threat_detail: string;
}

interface ProcessAuditorProps {
  processes: ProcessAuditItem[];
  onRefresh: () => void;
}

export const ProcessAuditor: React.FC<ProcessAuditorProps> = ({ processes, onRefresh }) => {
  const suspiciousCount = processes.filter((p) => p.is_suspicious).length;

  return (
    <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-base font-bold text-white">Process Behavior Auditor</h2>
            <span
              className={`text-[10px] px-2 py-0.5 rounded font-bold uppercase ${
                suspiciousCount > 0
                  ? "bg-rose-950 text-rose-400 border border-rose-800"
                  : "bg-emerald-950 text-emerald-400 border border-emerald-800"
              }`}
            >
              {suspiciousCount > 0 ? `${suspiciousCount} RISKY PROCESSES` : "CLEAN"}
            </span>
          </div>
          <p className="text-xs text-slate-400">
            Monitors process table for masquerading binaries, fake browser wrappers, and unauthorized socket listeners.
          </p>
        </div>

        <button
          onClick={onRefresh}
          className="text-xs px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
        >
          Rescan Processes
        </button>
      </div>

      <div className="space-y-2 max-h-48 overflow-y-auto">
        {processes.length === 0 ? (
          <div className="text-xs text-slate-500 py-3 text-center">No active process warnings.</div>
        ) : (
          processes.map((proc) => (
            <div
              key={proc.pid}
              className={`p-3 rounded-lg border text-xs flex items-center justify-between ${
                proc.is_suspicious
                  ? "bg-rose-950/30 border-rose-800/80 text-rose-200"
                  : "bg-slate-950/60 border-slate-800/80 text-slate-300"
              }`}
            >
              <div>
                <div className="flex items-center space-x-2">
                  <span className="font-bold text-white">{proc.name}</span>
                  <span className="text-[10px] text-slate-500">PID: {proc.pid}</span>
                </div>
                <div className="text-[11px] text-slate-400 truncate max-w-lg">{proc.path}</div>
                {proc.is_suspicious && (
                  <div className="text-rose-400 font-semibold mt-1">
                    ⚠ {proc.threat_detail}
                  </div>
                )}
              </div>

              <div className="text-right">
                <span
                  className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    proc.is_suspicious ? "bg-rose-900 text-rose-200" : "bg-emerald-950 text-emerald-300"
                  }`}
                >
                  {proc.is_suspicious ? "SUSPICIOUS" : "NORMAL"}
                </span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
