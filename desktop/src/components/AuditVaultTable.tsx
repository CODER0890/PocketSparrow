import React, { useState } from "react";
import {
  Database,
  Lock,
  Clock,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,

  ChevronRight,
} from "lucide-react";
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
    <div className="rounded-xl border border-surface-border bg-background-elevated shadow-card overflow-hidden">
      {/* Table Header Controls */}
      <div className="p-4 border-b border-surface-border flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-background-subtle">
        <div className="flex items-center space-x-3">
          <div className="p-2 rounded-lg bg-surface border border-surface-border text-accent">
            <Database className="w-4 h-4" strokeWidth={1.75} />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-sm font-semibold text-brand-text tracking-tight">
                Encrypted Forensic Vault
              </h2>
              <span className="flex items-center gap-1 text-[10px] font-mono px-2 py-0.5 rounded-full bg-surface border border-surface-border text-brand-muted">
                <Lock className="w-2.5 h-2.5 text-accent" />
                SQLCipher AES-256
              </span>
            </div>
            <p className="text-xs text-brand-muted mt-0.5">
              Immutable forensic incident ledger retained purely on-device. Zero telemetry sync.
            </p>
          </div>
        </div>

        {/* Filter Pills */}
        <div className="flex p-0.5 rounded-lg bg-background border border-surface-border text-xs self-start sm:self-auto">
          {["ALL", "MALICIOUS", "SUSPICIOUS", "SAFE"].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-3 py-1 rounded-md text-[11px] font-mono transition ${
                filter === f
                  ? "bg-surface-active text-brand-text border border-surface-borderHover"
                  : "text-brand-muted hover:text-brand-secondary"
              }`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* Structured Ledger Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs font-mono">
          <thead>
            <tr className="border-b border-surface-border bg-surface text-brand-muted uppercase text-[10px] tracking-wider">
              <th className="py-2.5 px-4 font-semibold">Timestamp</th>
              <th className="py-2.5 px-4 font-semibold">Vector</th>
              <th className="py-2.5 px-4 font-semibold">Target Payload</th>
              <th className="py-2.5 px-4 font-semibold">Threat Category</th>
              <th className="py-2.5 px-4 font-semibold">Verdict</th>
              <th className="py-2.5 px-4 font-semibold text-right">Latency</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-surface-border">
            {filteredLogs.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-8 text-center text-brand-faint text-xs">
                  No forensic log entries matching selected criteria.
                </td>
              </tr>
            ) : (
              filteredLogs.map((log) => (
                <tr
                  key={log.id}
                  onClick={() => {
                    onSelectLog({
                      verdict: log.verdict,
                      tier_triggered: "Tier1Heuristic",
                      confidence: 0.98,
                      latency_us: log.latency_us,
                      category: log.category,
                      xai_reason: `Historical forensic record for payload: ${log.payload_snippet}. Evaluated on-device in ${log.latency_us} µs.`,
                      should_block: log.verdict === "Malicious",
                    });
                  }}
                  className="hover:bg-surface-hover cursor-pointer transition-colors group"
                >
                  {/* Timestamp */}
                  <td className="py-3 px-4 text-brand-muted whitespace-nowrap flex items-center space-x-1.5">
                    <Clock className="w-3 h-3 text-brand-faint" />
                    <span>{log.timestamp}</span>
                  </td>

                  {/* Vector */}
                  <td className="py-3 px-4">
                    <span className="px-1.5 py-0.5 rounded bg-surface border border-surface-border text-brand-secondary text-[10px]">
                      {log.type}
                    </span>
                  </td>

                  {/* Target Payload */}
                  <td className="py-3 px-4 text-brand-text truncate max-w-sm group-hover:text-accent transition-colors">
                    {log.payload_snippet}
                  </td>

                  {/* Category */}
                  <td className="py-3 px-4 text-brand-secondary text-[11px]">
                    {log.category}
                  </td>

                  {/* Verdict Badge */}
                  <td className="py-3 px-4">
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${
                        log.verdict === "Malicious"
                          ? "bg-status-dangerBg text-status-danger border-status-dangerBorder"
                          : log.verdict === "Suspicious"
                          ? "bg-status-warningBg text-status-warning border-status-warningBorder"
                          : "bg-status-safeBg text-status-safe border-status-safeBorder"
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
                  </td>

                  {/* Latency */}
                  <td className="py-3 px-4 text-right text-brand-secondary font-mono text-[11px]">
                    <div className="flex items-center justify-end space-x-1.5">
                      <span>{log.latency_us} µs</span>
                      <ChevronRight className="w-3 h-3 text-brand-faint opacity-0 group-hover:opacity-100 transition-opacity" />
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
