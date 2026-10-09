import React, { useState } from "react";

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
    <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-base font-bold text-white">Local Encrypted History (SQLCipher AES-256)</h2>
            <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-semibold">
              Zero-Cloud
            </span>
          </div>
          <p className="text-xs text-slate-400">
            Immutable threat logs stored strictly on-device in encrypted SQLite database.
          </p>
        </div>

        {/* Filter buttons */}
        <div className="flex space-x-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
          {["ALL", "MALICIOUS", "SUSPICIOUS", "SAFE"].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-2.5 py-1 rounded text-[11px] font-semibold transition ${
                filter === f ? "bg-slate-800 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* Log list */}
      <div className="divide-y divide-slate-800/80 max-h-56 overflow-y-auto">
        {filteredLogs.length === 0 ? (
          <div className="text-xs text-slate-500 py-6 text-center">No logs match the current filter.</div>
        ) : (
          filteredLogs.map((log) => (
            <div
              key={log.id}
              onClick={() => onSelectLog(log)}
              className="py-2.5 px-3 hover:bg-slate-800/40 rounded-lg cursor-pointer transition flex items-center justify-between text-xs"
            >
              <div className="flex items-center space-x-3">
                <span
                  className={`h-2 w-2 rounded-full ${
                    log.verdict === "Malicious"
                      ? "bg-rose-500"
                      : log.verdict === "Suspicious"
                      ? "bg-amber-500"
                      : "bg-emerald-500"
                  }`}
                />
                <div>
                  <div className="font-semibold text-slate-200 truncate max-w-sm">
                    {log.payload_snippet}
                  </div>
                  <div className="text-[10px] text-slate-500 flex items-center space-x-2 mt-0.5">
                    <span>{log.timestamp}</span>
                    <span>•</span>
                    <span>{log.type}</span>
                    <span>•</span>
                    <span className="text-cyan-400 font-mono">{(log.latency_us / 1000).toFixed(2)} ms</span>
                  </div>
                </div>
              </div>

              <div className="text-right">
                <span
                  className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    log.verdict === "Malicious"
                      ? "bg-rose-950 text-rose-300 border border-rose-800"
                      : log.verdict === "Suspicious"
                      ? "bg-amber-950 text-amber-300 border border-amber-800"
                      : "bg-emerald-950 text-emerald-300 border border-emerald-800"
                  }`}
                >
                  {log.category}
                </span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
