import React, { useState } from "react";
import { RotateCcw, Eye, ShieldAlert, ShieldCheck, X, Copy, Check, AlertTriangle } from "lucide-react";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import { MOTION_EASING, MOTION_DURATION } from "../styles/motion";

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
  onTerminateProcess?: (pid: number) => void;
}

export const ProcessAuditorTable: React.FC<ProcessAuditorTableProps> = ({
  processes,
  onRefresh,
  onTerminateProcess,
}) => {
  const shouldReduceMotion = !!useReducedMotion();
  const [filter, setFilter] = useState<"ALL" | "SUSPICIOUS">("ALL");
  const [inspectingProc, setInspectingProc] = useState<ProcessAuditItem | null>(null);
  const [copied, setCopied] = useState(false);
  const suspiciousCount = processes.filter((p) => p.is_suspicious).length;

  const displayedProcesses = processes.filter((p) => {
    if (filter === "SUSPICIOUS") return p.is_suspicious;
    return true;
  });

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 space-y-4 transition-colors shadow-sm dark:shadow-none">
      {/* Header and Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
              Active Process Audit
            </h3>
            {suspiciousCount > 0 ? (
              <motion.span
                animate={
                  shouldReduceMotion
                    ? undefined
                    : { opacity: [0.8, 1, 0.8] }
                }
                transition={{
                  duration: 2.2,
                  repeat: Infinity,
                  ease: "easeInOut",
                }}
                className="inline-flex items-center gap-1.5 text-xs text-rose-700 dark:text-rose-400 bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/20 px-2 py-0.5 rounded font-medium"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
                {suspiciousCount} Alert
              </motion.span>
            ) : (
              <span className="inline-flex items-center gap-1.5 text-xs text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 px-2 py-0.5 rounded font-medium">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                All Clear
              </span>
            )}
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Real-time cross-platform inspection of running processes across Linux, Windows, macOS, and Android.
          </p>
        </div>

        <div className="flex items-center space-x-2 self-start sm:self-auto">
          <div className="inline-flex p-0.5 rounded bg-zinc-100 dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 text-xs">
            <button
              onClick={() => setFilter("ALL")}
              className={`px-2.5 py-1 rounded transition-colors ${
                filter === "ALL"
                  ? "bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium shadow-sm"
                  : "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200"
              }`}
            >
              All ({processes.length})
            </button>
            <button
              onClick={() => setFilter("SUSPICIOUS")}
              className={`px-2.5 py-1 rounded transition-colors ${
                filter === "SUSPICIOUS"
                  ? "bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium shadow-sm"
                  : "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200"
              }`}
            >
              Alerts ({suspiciousCount})
            </button>
          </div>

          <motion.button
            whileTap={{ rotate: 180 }}
            transition={{ duration: MOTION_DURATION.macro, ease: MOTION_EASING }}
            onClick={onRefresh}
            className="p-1.5 rounded border border-zinc-200 dark:border-zinc-800 bg-zinc-50 hover:bg-zinc-100 dark:bg-zinc-900 dark:hover:bg-zinc-800 text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200 transition-colors"
            title="Rescan host processes"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </motion.button>
        </div>
      </div>

      {/* Data Table */}
      <div className="overflow-x-auto rounded border border-zinc-200 dark:border-zinc-800">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950 text-zinc-500 dark:text-zinc-400 uppercase tracking-wider font-medium text-[11px]">
              <th className="py-2.5 px-4">PID</th>
              <th className="py-2.5 px-4">Process Name</th>
              <th className="py-2.5 px-4">Executable Path</th>
              <th className="py-2.5 px-4">Status</th>
              <th className="py-2.5 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60 bg-white dark:bg-zinc-900/20">
            {displayedProcesses.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-8 text-center text-zinc-400 dark:text-zinc-500">
                  {filter === "SUSPICIOUS"
                    ? "No suspicious processes detected. Host runtime is secure."
                    : "No active processes recorded. Click refresh to scan host processes."}
                </td>
              </tr>
            ) : (
              displayedProcesses.map((proc) => {
                const displayName = proc.name && proc.name.trim() ? proc.name : `proc-${proc.pid}`;
                const displayPath = proc.path && proc.path.trim() ? proc.path : `/proc/${proc.pid}`;

                return (
                  <tr
                    key={proc.pid}
                    className="hover:bg-zinc-50/80 dark:hover:bg-zinc-800/30 transition-colors group cursor-pointer"
                    onClick={() => setInspectingProc(proc)}
                  >
                    <td className="py-3 px-4 font-mono text-zinc-500 dark:text-zinc-400">{proc.pid}</td>
                    <td className="py-3 px-4 font-medium text-zinc-900 dark:text-zinc-200">{displayName}</td>
                    <td className="py-3 px-4 font-mono text-zinc-500 dark:text-zinc-400 max-w-xs truncate" title={displayPath}>
                      {displayPath}
                    </td>
                    <td className="py-3 px-4">
                      {proc.is_suspicious ? (
                        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20">
                          <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
                          Suspicious
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                          Verified
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center justify-end space-x-2">
                        <button
                          onClick={() => setInspectingProc(proc)}
                          className="px-2 py-1 rounded bg-zinc-100 hover:bg-zinc-200 dark:bg-zinc-800 dark:hover:bg-zinc-700 text-zinc-700 dark:text-zinc-300 text-[11px] font-medium transition-colors"
                          title="Inspect process details"
                        >
                          <Eye className="w-3 h-3 inline mr-1" />
                          Inspect
                        </button>
                        {proc.is_suspicious ? (
                          <motion.button
                            whileHover={{ scale: 1.05 }}
                            whileTap={{ scale: 0.95 }}
                            onClick={() => onTerminateProcess && onTerminateProcess(proc.pid)}
                            className="px-2.5 py-1 rounded bg-rose-600 hover:bg-rose-700 text-white dark:bg-rose-500 dark:hover:bg-rose-600 text-[11px] font-medium transition-colors shadow-sm"
                            title="Quarantine and kill suspicious process"
                          >
                            Quarantine
                          </motion.button>
                        ) : (
                          <motion.button
                            whileHover={{ scale: 1.05 }}
                            whileTap={{ scale: 0.95 }}
                            onClick={() => onTerminateProcess && onTerminateProcess(proc.pid)}
                            className="px-2 py-1 rounded border border-zinc-200 dark:border-zinc-800 hover:border-rose-300 dark:hover:border-rose-500/30 text-zinc-500 hover:text-rose-600 dark:text-zinc-400 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-500/10 text-[11px] font-medium transition-colors"
                            title="Terminate process"
                          >
                            Kill
                          </motion.button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Interactive Process Inspection Modal */}
      <AnimatePresence>
        {inspectingProc && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: MOTION_DURATION.micro, ease: MOTION_EASING }}
              className="w-full max-w-lg rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-6 shadow-2xl space-y-5"
            >
              <div className="flex items-center justify-between pb-3 border-b border-zinc-100 dark:border-zinc-800">
                <div className="flex items-center space-x-2.5">
                  <div
                    className={`p-2 rounded-lg ${
                      inspectingProc.is_suspicious
                        ? "bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400"
                        : "bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-400"
                    }`}
                  >
                    {inspectingProc.is_suspicious ? (
                      <ShieldAlert className="w-5 h-5" />
                    ) : (
                      <ShieldCheck className="w-5 h-5" />
                    )}
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                      Process Forensic Audit
                    </h3>
                    <p className="text-xs text-zinc-500 dark:text-zinc-400">
                      PID {inspectingProc.pid} • {inspectingProc.name}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setInspectingProc(null)}
                  className="p-1 rounded text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-3.5 text-xs">
                <div>
                  <label className="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                    Process Name
                  </label>
                  <div className="mt-1 font-mono text-zinc-900 dark:text-zinc-100 bg-zinc-50 dark:bg-zinc-950 p-2.5 rounded border border-zinc-200 dark:border-zinc-800">
                    {inspectingProc.name || `proc-${inspectingProc.pid}`}
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between">
                    <label className="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                      Executable Path
                    </label>
                    <button
                      onClick={() => handleCopy(inspectingProc.path)}
                      className="text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-300 flex items-center space-x-1"
                    >
                      {copied ? <Check className="w-3 h-3 text-emerald-500" /> : <Copy className="w-3 h-3" />}
                      <span className="text-[10px]">{copied ? "Copied" : "Copy"}</span>
                    </button>
                  </div>
                  <div className="mt-1 font-mono text-zinc-800 dark:text-zinc-200 bg-zinc-50 dark:bg-zinc-950 p-2.5 rounded border border-zinc-200 dark:border-zinc-800 break-all select-all">
                    {inspectingProc.path || `/proc/${inspectingProc.pid}`}
                  </div>
                </div>

                <div>
                  <label className="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                    Security Classification
                  </label>
                  <div className="mt-1">
                    {inspectingProc.is_suspicious ? (
                      <div className="p-3 rounded bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/20 text-rose-800 dark:text-rose-300 space-y-1">
                        <div className="flex items-center space-x-1.5 font-semibold">
                          <AlertTriangle className="w-3.5 h-3.5" />
                          <span>Threat Signature Flagged</span>
                        </div>
                        <p className="text-[11px] leading-relaxed">
                          {inspectingProc.threat_detail || "Process demonstrates anomalous background execution behavior or temporary path execution."}
                        </p>
                      </div>
                    ) : (
                      <div className="p-3 rounded bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 text-emerald-800 dark:text-emerald-300">
                        <div className="flex items-center space-x-1.5 font-semibold">
                          <ShieldCheck className="w-3.5 h-3.5" />
                          <span>Verified Operating System Process</span>
                        </div>
                        <p className="text-[11px] text-emerald-700 dark:text-emerald-400 mt-0.5">
                          Authentic binary signing and trusted binary directory. Zero reverse-shell patterns.
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
                <button
                  onClick={() => setInspectingProc(null)}
                  className="px-3 py-1.5 rounded text-xs font-medium text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 transition-colors"
                >
                  Dismiss
                </button>
                <button
                  onClick={() => {
                    if (onTerminateProcess) {
                      onTerminateProcess(inspectingProc.pid);
                    }
                    setInspectingProc(null);
                  }}
                  className={`px-3.5 py-1.5 rounded text-xs font-medium text-white transition-colors ${
                    inspectingProc.is_suspicious
                      ? "bg-rose-600 hover:bg-rose-700"
                      : "bg-zinc-800 hover:bg-rose-600 dark:bg-zinc-700 dark:hover:bg-rose-600"
                  }`}
                >
                  {inspectingProc.is_suspicious ? "Quarantine & Terminate" : "Kill Process"}
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};

