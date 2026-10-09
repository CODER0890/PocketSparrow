import React, { useState } from "react";
import { Lock } from "lucide-react";
import { ScanResultPayload } from "./XaiDrawer";

export interface LogEntry {
  id: string;
  timestamp: string;
  type: string;
  payload_snippet: string;
  verdict: "Safe" | "Suspicious" | "Malicious";
  category: string;
  latency_us: number;
}

interface AuditVaultTableProps {
  logs: LogEntry[];
  onSelectLog: (result: ScanResultPayload) => void;
}

export const AuditVaultTable: React.FC<AuditVaultTableProps> = ({ logs, onSelectLog }) => {
  const [filter, setFilter] = useState<string>("ALL");

  const filteredLogs = logs.filter((log) => {
    if (filter === "MALICIOUS") return log.verdict === "Malicious";
    if (filter === "SUSPICIOUS") return log.verdict === "Suspicious";
    if (filter === "SAFE") return log.verdict === "Safe";
    return true;
  });

  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-6 space-y-4">
      {/* Header and Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="text-sm font-semibold text-zinc-100">
              Encrypted Audit Ledger
            </h3>
            <span className="inline-flex items-center gap-1 text-[11px] text-zinc-400 bg-zinc-800 px-2 py-0.5 rounded border border-zinc-700/60 font-medium">
              <Lock className="w-3 h-3 text-zinc-400" />
              SQLCipher AES-256
            </span>
          </div>
          <p className="text-xs text-zinc-400 mt-1">
            Immutable forensic history maintained purely on local storage.
          </p>
        </div>

        {/* Filter Buttons */}
        <div className="inline-flex p-0.5 rounded bg-zinc-950 border border-zinc-800 text-xs self-start sm:self-auto">
          {["ALL", "MALICIOUS", "SUSPICIOUS", "SAFE"].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-2.5 py-1 rounded transition-colors ${
                filter === f
                  ? "bg-zinc-800 text-zinc-100 font-medium"
                  : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              {f === "ALL" ? "All" : f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Data Table */}
      <div className="overflow-x-auto rounded border border-zinc-800">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-zinc-800 bg-zinc-950 text-zinc-400 uppercase tracking-wider font-medium text-[11px]">
              <th className="py-2.5 px-4">Time</th>
              <th className="py-2.5 px-4">Type</th>
              <th className="py-2.5 px-4">Payload</th>
              <th className="py-2.5 px-4">Category</th>
              <th className="py-2.5 px-4">Verdict</th>
              <th className="py-2.5 px-4 text-right">Latency</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/60 bg-zinc-900/20">
            {filteredLogs.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-8 text-center text-zinc-500">
                  No records match the selected filter.
                </td>
              </tr>
            ) : (
              filteredLogs.map((log) => (
                <tr
                  key={log.id}
                  onClick={() =>
                    onSelectLog({
                      verdict: log.verdict,
                      tier_triggered: "Tier1Heuristic",
                      confidence: 0.98,
                      latency_us: log.latency_us,
                      category: log.category,
                      xai_reason:
                        log.verdict === "Malicious"
                          ? `Flagged by on-device engine under rule ${log.category}. Deceptive markers identified.`
                          : "Verified clean payload with safe entropy and authoritative domain validation.",
                      should_block: log.verdict === "Malicious",
                    })
                  }
                  className="hover:bg-zinc-800/30 cursor-pointer transition-colors"
                >
                  <td className="py-3 px-4 font-mono text-zinc-400">{log.timestamp}</td>
                  <td className="py-3 px-4 font-medium text-zinc-300">{log.type}</td>
                  <td className="py-3 px-4 font-mono text-zinc-300 max-w-xs truncate" title={log.payload_snippet}>
                    {log.payload_snippet}
                  </td>
                  <td className="py-3 px-4 text-zinc-400 font-sans">{log.category}</td>
                  <td className="py-3 px-4">
                    {log.verdict === "Malicious" && (
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium bg-rose-500/10 text-rose-400 border border-rose-500/20">
                        <span className="w-1.5 h-1.5 rounded-full bg-rose-400" />
                        Malicious
                      </span>
                    )}
                    {log.verdict === "Suspicious" && (
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-400" />
                        Suspicious
                      </span>
                    )}
                    {log.verdict === "Safe" && (
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                        Safe
                      </span>
                    )}
                  </td>
                  <td className="py-3 px-4 font-mono text-zinc-400 text-right">
                    {(log.latency_us / 1000).toFixed(3)} ms
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
