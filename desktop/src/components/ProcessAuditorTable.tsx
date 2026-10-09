import React, { useState } from "react";
import { RotateCcw } from "lucide-react";

export interface ProcessAuditItem {
  pid: number;
  name: string;
  path: string;
  is_suspicious: boolean;
  threat_detail: string;
}

interface ProcessAuditorTableProps {
  processes: ProcessAuditItem[];
  onRefresh: () => void;
}

export const ProcessAuditorTable: React.FC<ProcessAuditorTableProps> = ({
  processes,
  onRefresh,
}) => {
  const [filter, setFilter] = useState<"ALL" | "SUSPICIOUS">("ALL");
  const suspiciousCount = processes.filter((p) => p.is_suspicious).length;

  const displayedProcesses = processes.filter((p) => {
    if (filter === "SUSPICIOUS") return p.is_suspicious;
    return true;
  });

  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-6 space-y-4">
      {/* Header and Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="text-sm font-semibold text-zinc-100">
              Active Process Audit
            </h3>
            {suspiciousCount > 0 ? (
              <span className="inline-flex items-center gap-1.5 text-xs text-rose-400 bg-rose-500/10 border border-rose-500/20 px-2 py-0.5 rounded font-medium">
                <span className="w-1.5 h-1.5 rounded-full bg-rose-400" />
                {suspiciousCount} Alert
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 text-xs text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded font-medium">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                All Clear
              </span>
            )}
          </div>
          <p className="text-xs text-zinc-400 mt-1">
            Real-time inspection of running processes for reverse shell and privilege anomalies.
          </p>
        </div>

        <div className="flex items-center space-x-2 self-start sm:self-auto">
          <div className="inline-flex p-0.5 rounded bg-zinc-950 border border-zinc-800 text-xs">
            <button
              onClick={() => setFilter("ALL")}
              className={`px-2.5 py-1 rounded transition-colors ${
                filter === "ALL"
                  ? "bg-zinc-800 text-zinc-100 font-medium"
                  : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              All ({processes.length})
            </button>
            <button
              onClick={() => setFilter("SUSPICIOUS")}
              className={`px-2.5 py-1 rounded transition-colors ${
                filter === "SUSPICIOUS"
                  ? "bg-zinc-800 text-zinc-100 font-medium"
                  : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              Alerts ({suspiciousCount})
            </button>
          </div>

          <button
            onClick={onRefresh}
            className="p-1.5 rounded border border-zinc-800 bg-zinc-900 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-200 transition-colors"
            title="Rescan processes"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Data Table */}
      <div className="overflow-x-auto rounded border border-zinc-800">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-zinc-800 bg-zinc-950 text-zinc-400 uppercase tracking-wider font-medium text-[11px]">
              <th className="py-2.5 px-4">PID</th>
              <th className="py-2.5 px-4">Process Name</th>
              <th className="py-2.5 px-4">Path</th>
              <th className="py-2.5 px-4">Status</th>
              <th className="py-2.5 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/60 bg-zinc-900/20">
            {displayedProcesses.map((proc) => (
              <tr key={proc.pid} className="hover:bg-zinc-800/30 transition-colors">
                <td className="py-3 px-4 font-mono text-zinc-400">{proc.pid}</td>
                <td className="py-3 px-4 font-medium text-zinc-200">{proc.name}</td>
                <td className="py-3 px-4 font-mono text-zinc-400 max-w-xs truncate" title={proc.path}>
                  {proc.path}
                </td>
                <td className="py-3 px-4">
                  {proc.is_suspicious ? (
                    <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium bg-rose-500/10 text-rose-400 border border-rose-500/20">
                      <span className="w-1.5 h-1.5 rounded-full bg-rose-400" />
                      Suspicious
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                      Verified
                    </span>
                  )}
                </td>
                <td className="py-3 px-4 text-right">
                  {proc.is_suspicious ? (
                    <button
                      onClick={() => alert(`Quarantining PID ${proc.pid}`)}
                      className="px-2.5 py-1 rounded bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-400 text-xs font-medium transition-colors"
                    >
                      Terminate
                    </button>
                  ) : (
                    <span className="text-zinc-600 text-xs font-mono">—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
