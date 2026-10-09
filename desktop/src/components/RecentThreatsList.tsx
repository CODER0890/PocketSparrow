import React from "react";
import { ShieldAlert, ShieldCheck, ChevronRight } from "lucide-react";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import { LogEntry } from "./AuditVaultTable";
import { ScanResultPayload } from "./XaiDrawer";
import { MOTION_EASING, MOTION_DURATION } from "../styles/motion";

interface RecentThreatsListProps {
  logs: LogEntry[];
  onSelectThreat: (result: ScanResultPayload) => void;
  onViewAll?: () => void;
}

export const RecentThreatsList: React.FC<RecentThreatsListProps> = ({
  logs,
  onSelectThreat,
  onViewAll,
}) => {
  const shouldReduceMotion = !!useReducedMotion();
  const recentLogs = logs.slice(0, 5);

  return (
    <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-5 transition-colors shadow-sm dark:shadow-none">
      <div className="flex items-center justify-between pb-3.5 border-b border-zinc-100 dark:border-zinc-800">
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
            Recent Threats
          </h3>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
            Latest intercepted vectors evaluated on-device
          </p>
        </div>
        {onViewAll && (
          <button
            onClick={onViewAll}
            className="text-xs font-medium text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-200 flex items-center space-x-1 transition-colors"
          >
            <span>View Vault</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      <div className="divide-y divide-zinc-100 dark:divide-zinc-800/80 mt-1">
        {recentLogs.length === 0 ? (
          <div className="py-8 flex flex-col items-center justify-center text-center">
            <div className="p-2.5 rounded-full bg-zinc-100 dark:bg-zinc-800/60 text-emerald-600 dark:text-emerald-400 mb-2.5">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <p className="text-xs font-semibold text-zinc-900 dark:text-zinc-200">
              No threats detected
            </p>
            <p className="text-[11px] text-zinc-500 dark:text-zinc-400 mt-1 max-w-xs">
              System is actively monitoring incoming vectors in real-time. Zero malicious payloads intercepted.
            </p>
          </div>
        ) : (
          <AnimatePresence initial={false}>
            {recentLogs.map((log) => {
            const isBlocked = log.verdict === "Malicious";
            return (
              <motion.div
                key={log.id}
                layout
                initial={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, y: -12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.98 }}
                transition={{ duration: MOTION_DURATION.macro, ease: MOTION_EASING }}
                whileHover={{ x: 2 }}
                onClick={() =>
                  onSelectThreat({
                    verdict: log.verdict,
                    tier_triggered: "Tier1Heuristic",
                    confidence: 0.98,
                    latency_us: log.latency_us,
                    category: log.category,
                    xai_reason:
                      log.verdict === "Malicious"
                        ? `Detected threat signature ${log.category} via local heuristic/transformer model.`
                        : "Verified benign payload with authentic structure and trusted domain registration.",
                    should_block: isBlocked,
                  })
                }
                className="py-3 flex items-center justify-between hover:bg-zinc-50 dark:hover:bg-zinc-800/40 px-2 rounded cursor-pointer transition-colors group"
              >
                <div className="flex items-center space-x-3 min-w-0 pr-4">
                  <div
                    className={`p-1.5 rounded-md shrink-0 transition-transform group-hover:scale-105 ${
                      isBlocked
                        ? "bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400"
                        : "bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-400"
                    }`}
                  >
                    {isBlocked ? (
                      <ShieldAlert className="w-3.5 h-3.5" />
                    ) : (
                      <ShieldCheck className="w-3.5 h-3.5" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-medium text-zinc-900 dark:text-zinc-100 truncate">
                      {log.type === "URL"
                        ? "Phish URL: "
                        : log.type === "SMS"
                        ? "Bank Smish: "
                        : "Quish Payload: "}
                      <span className="font-mono text-zinc-600 dark:text-zinc-300">
                        {log.payload_snippet}
                      </span>
                    </div>
                    <div className="text-[11px] text-zinc-400 dark:text-zinc-500 font-mono mt-0.5">
                      {log.timestamp} • {(log.latency_us / 1000).toFixed(3)} ms
                    </div>
                  </div>
                </div>

                <div className="shrink-0 flex items-center space-x-2">
                  {isBlocked ? (
                    <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[11px] font-medium bg-rose-50 text-rose-700 border border-rose-200 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20">
                      <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
                      Blocked
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                      Safe
                    </span>
                  )}
                  <ChevronRight className="w-3.5 h-3.5 text-zinc-300 dark:text-zinc-600 group-hover:text-zinc-600 dark:group-hover:text-zinc-300 transition-colors" />
                </div>
              </motion.div>
            );
          })}
          </AnimatePresence>
        )}
      </div>
    </div>
  );
};
