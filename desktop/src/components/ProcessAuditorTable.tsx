import React, { useState } from "react";
import {
  Cpu,
  Terminal,
  AlertTriangle,
  CheckCircle2,
  RotateCcw,

  Eye,
  Ban,

} from "lucide-react";

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
    <div className="rounded-xl border border-surface-border bg-background-elevated shadow-card overflow-hidden">
      {/* Table Header Controls */}
      <div className="p-4 border-b border-surface-border flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-background-subtle">
        <div className="flex items-center space-x-3">
          <div className="p-2 rounded-lg bg-surface border border-surface-border text-indigo">
            <Cpu className="w-4 h-4" strokeWidth={1.75} />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-sm font-semibold text-brand-text tracking-tight">
                Process Behavior Auditor
              </h2>
              <span
                className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                  suspiciousCount > 0
                    ? "bg-status-dangerBg text-status-danger border-status-dangerBorder font-semibold"
                    : "bg-status-safeBg text-status-safe border-status-safeBorder font-semibold"
                }`}
              >
                {suspiciousCount > 0 ? `${suspiciousCount} Suspicious Tasks` : "0 Vulnerabilities"}
              </span>
            </div>
            <p className="text-xs text-brand-muted mt-0.5">
              Continuous inspection of running executables, reverse shell signatures, and privilege abuse.
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2 self-start sm:self-auto">
          <div className="flex p-0.5 rounded-lg bg-background border border-surface-border text-xs">
            <button
              onClick={() => setFilter("ALL")}
              className={`px-2.5 py-1 rounded-md text-xs font-mono transition ${
                filter === "ALL"
                  ? "bg-surface-active text-brand-text"
                  : "text-brand-muted hover:text-brand-secondary"
              }`}
            >
              All ({processes.length})
            </button>
            <button
              onClick={() => setFilter("SUSPICIOUS")}
              className={`px-2.5 py-1 rounded-md text-xs font-mono transition ${
                filter === "SUSPICIOUS"
                  ? "bg-surface-active text-brand-text"
                  : "text-brand-muted hover:text-brand-secondary"
              }`}
            >
              Risky ({suspiciousCount})
            </button>
          </div>

          <button
            onClick={onRefresh}
            className="flex items-center space-x-1 px-3 py-1.5 rounded-lg bg-surface hover:bg-surface-hover border border-surface-border text-brand-secondary hover:text-brand-text text-xs transition"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span className="hidden md:inline">Rescan</span>
          </button>
        </div>
      </div>

      {/* Structured Data Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs font-mono">
          <thead>
            <tr className="border-b border-surface-border bg-surface text-brand-muted uppercase text-[10px] tracking-wider">
              <th className="py-2.5 px-4 font-semibold">Process Name</th>
              <th className="py-2.5 px-4 font-semibold">PID</th>
              <th className="py-2.5 px-4 font-semibold">Binary Path</th>
              <th className="py-2.5 px-4 font-semibold">Audit Verdict</th>
              <th className="py-2.5 px-4 font-semibold text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-surface-border">
            {displayedProcesses.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-8 text-center text-brand-faint text-xs">
                  No active processes match the selected filter.
                </td>
              </tr>
            ) : (
              displayedProcesses.map((proc) => (
                <tr
                  key={proc.pid}
                  className="hover:bg-surface-hover transition-colors group"
                >
                  {/* Process Name */}
                  <td className="py-3 px-4 text-brand-text font-semibold flex items-center space-x-2">
                    <Terminal className="w-3.5 h-3.5 text-brand-muted" />
                    <span>{proc.name}</span>
                  </td>

                  {/* PID */}
                  <td className="py-3 px-4 text-brand-muted">{proc.pid}</td>

                  {/* Path */}
                  <td className="py-3 px-4 text-brand-secondary truncate max-w-xs text-[11px]">
                    {proc.path}
                    {proc.threat_detail && (
                      <div className="text-[10px] text-status-danger mt-0.5 truncate font-sans">
                        {proc.threat_detail}
                      </div>
                    )}
                  </td>

                  {/* Verdict Badge */}
                  <td className="py-3 px-4">
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${
                        proc.is_suspicious
                          ? "bg-status-dangerBg text-status-danger border-status-dangerBorder"
                          : "bg-status-safeBg text-status-safe border-status-safeBorder"
                      }`}
                    >
                      {proc.is_suspicious ? (
                        <>
                          <AlertTriangle className="w-2.5 h-2.5" />
                          Critical Risk
                        </>
                      ) : (
                        <>
                          <CheckCircle2 className="w-2.5 h-2.5" />
                          Verified Benign
                        </>
                      )}
                    </span>
                  </td>

                  {/* Actions */}
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end space-x-1 opacity-80 group-hover:opacity-100 transition-opacity">
                      <button
                        title="Sandbox Audit"
                        className="p-1 rounded text-brand-muted hover:text-accent hover:bg-surface transition"
                      >
                        <Eye className="w-3.5 h-3.5" />
                      </button>
                      {proc.is_suspicious && (
                        <button
                          title="Terminate Task"
                          className="p-1 rounded text-status-danger hover:bg-status-dangerBg transition"
                        >
                          <Ban className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
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
