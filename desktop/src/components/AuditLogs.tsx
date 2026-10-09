import React, { useState } from "react";
import { Database, Lock, Clock, ShieldCheck, ShieldAlert, AlertTriangle } from "lucide-react";

export interface LogEntry {
  id: string;
  timestamp: string;
  type: string;
  payload_snippet: string;
  verdict: "Safe" | "Suspicious" | "Malicious";
  category: string;
  latency_us: number;
}

interface AuditLogsProps {
  logs: LogEntry[];
  onSelectLog: (log: LogEntry) => void;
}

export const AuditLogs: React.FC<AuditLogsProps> = ({ logs, onSelectLog }) => {
  const [filter, setFilter] = useState<string>("ALL");

  const filteredLogs = logs.filter((log) => {
    if (filter === "MALICIOUS") return log.verdict === "Malicious";
    if (filter === "SUSPICIOUS") return log.verdict === "Suspicious";
    if (filter === "SAFE") return log.verdict === "Safe";
    return true;
  });

  return (
    <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div>
          <div className="flex items-center space-x-2.5">
            <Database className="w-4 h-4 text-cyan-400" />
            <h2 className="text-base font-bold text-white tracking-wide font-mono">
              Encrypted Audit Vault
            </h2>
            <span className="flex items-center gap-1 text-[10px] px-2.5 py-0.5 rounded-full bg-slate-950 border border-slate-700 text-slate-300 font-bold font-mono">
              <Lock className="w-2.5 h-2.5 text-cyan-400" />
              SQLCipher AES-256
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Immutable forensic threat history committed strictly on-device with zero cloud synchronization.
          </p>
        </div>

        {/* Filter buttons */}
        <div className="flex space-x-1 bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs shadow-inner self-start sm:self-auto">
          {["ALL", "MALICIOUS", "SUSPICIOUS", "SAFE"].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-3 py-1 rounded-lg text-[10px] font-bold font-mono transition-all ${
                filter === f
                  ? "bg-slate-800 text-white shadow-sm border border-slate-700"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* Log list table */}
      <div className="border border-slate-800/80 rounded-xl overflow-hidden bg-slate-950/60">
        <div className="divide-y divide-slate-800/60 max-h-56 overflow-y-auto">
          {filteredLogs.length === 0 ? (
            <div className="text-xs text-slate-500 py-8 text-center font-mono">
              No matching forensic log entries recorded.
            </div>
          ) : (
            filteredLogs.map((log) => (
              <div
                key={log.id}
                onClick={() => onSelectLog(log)}
                className="p-3 flex items-center justify-between hover:bg-slate-900/80 cursor-pointer transition text-xs font-mono group"
              >
                <div className="flex items-center space-x-3 truncate mr-3">
                  <div className="flex items-center space-x-1 text-[11px] text-slate-500 whitespace-nowrap">
                    <Clock className="w-3 h-3 text-slate-500" />
                    <span>{log.timestamp}</span>
                  </div>

                  <span className="text-[10px] px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300 font-bold">
                    {log.type}
                  </span>

                  <span className="text-slate-200 truncate group-hover:text-cyan-300 transition-colors">
                    {log.payload_snippet}
                  </span>
                </div>

                <div className="flex items-center space-x-2.5 whitespace-nowrap">
                  <span className="text-[10px] text-slate-400 hidden md:inline">
                    {log.category}
                  </span>

                  <span
                    className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold uppercase tracking-wider border flex items-center gap-1 ${
                      log.verdict === "Malicious"
                        ? "bg-rose-950/80 text-rose-300 border-rose-500/50"
                        : log.verdict === "Suspicious"
                        ? "bg-amber-950/80 text-amber-300 border-amber-500/50"
                        : "bg-emerald-950/80 text-emerald-300 border-emerald-500/50"
                    }`}
                  >
                    {log.verdict === "Malicious" ? (
                      <ShieldAlert className="w-2.5 h-2.5" />
                    ) : log.verdict === "Suspicious" ? (
                      <AlertTriangle className="w-2.5 h-2.5" />
                    ) : (
                      <ShieldCheck className="w-2.5 h-2.5" />
                    )}
                    {log.verdict}
                  </span>

                  <span className="text-[11px] text-cyan-400 font-semibold w-14 text-right">
                    {log.latency_us} µs
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
