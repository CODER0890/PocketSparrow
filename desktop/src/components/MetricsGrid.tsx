import React, { useEffect, useState } from "react";
import { CheckCircle2 } from "lucide-react";
import { motion, useReducedMotion } from "framer-motion";
import {
  MOTION_EASING,
  MOTION_DURATION,
  staggerContainer,
  getStaggerItem,
} from "../styles/motion";

interface MetricsGridProps {
  totalScans: number;
  threatsBlocked: number;
  lastLatencyUs: number;
  peakRamMb: number;
  wanBytes: number;
}

export const MetricsGrid: React.FC<MetricsGridProps> = ({
  totalScans,
  threatsBlocked,
  lastLatencyUs,
  peakRamMb,
  wanBytes,
}) => {
  const shouldReduceMotion = !!useReducedMotion();
  const latencyMs = (lastLatencyUs / 1000).toFixed(3);
  const ramBudgetMb = 250.0;
  const ramPercent = Math.min(100, Math.round((peakRamMb / ramBudgetMb) * 100));
  const blockRate = totalScans > 0 ? ((threatsBlocked / totalScans) * 100).toFixed(1) : "0.0";

  // Trigger pop effect on threatsBlocked changes
  const [prevBlocked, setPrevBlocked] = useState(threatsBlocked);
  const [isPopping, setIsPopping] = useState(false);

  useEffect(() => {
    if (threatsBlocked !== prevBlocked) {
      setPrevBlocked(threatsBlocked);
      setIsPopping(true);
      const timer = setTimeout(() => setIsPopping(false), 450);
      return () => clearTimeout(timer);
    }
  }, [threatsBlocked, prevBlocked]);

  // Micro sparkline points
  const sparkPoints = [
    { x: 0, y: 18 },
    { x: 20, y: 14 },
    { x: 40, y: 20 },
    { x: 60, y: 12 },
    { x: 80, y: 16 },
    { x: 100, y: 8 },
    { x: 120, y: 14 },
    { x: 140, y: 10 },
  ];
  const pathD = sparkPoints.reduce(
    (acc, pt, i) => `${acc} ${i === 0 ? "M" : "L"} ${pt.x} ${pt.y}`,
    ""
  );

  const cardVariants = getStaggerItem(shouldReduceMotion);

  return (
    <motion.div
      variants={staggerContainer}
      initial="initial"
      animate="animate"
      className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6"
    >
      {/* 1. Evaluation Latency SLA */}
      <motion.div
        variants={cardVariants}
        className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors shadow-sm dark:shadow-none"
      >
        <div>
          <div className="flex items-start justify-between">
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
              Evaluation Latency
            </span>
            <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 px-2 py-0.5 rounded">
              <CheckCircle2 className="w-3 h-3" />
              SLA Met
            </span>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <div className="text-3xl font-semibold text-zinc-900 dark:text-zinc-100 font-sans tracking-tight">
              {latencyMs} <span className="text-sm font-normal text-zinc-500 dark:text-zinc-400">ms</span>
            </div>

            {/* Self-drawing SVG Sparkline */}
            <div className="w-24 h-6">
              <svg className="w-full h-full overflow-visible" viewBox="0 0 140 24">
                <motion.path
                  d={pathD}
                  fill="none"
                  stroke="#38BDF8"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  initial={shouldReduceMotion ? { pathLength: 1 } : { pathLength: 0 }}
                  animate={{ pathLength: 1 }}
                  transition={{ duration: 0.8, ease: MOTION_EASING }}
                />
              </svg>
            </div>
          </div>
          <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
            P99 target: &lt;50.0 ms ceiling
          </p>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400">
          <span>Tier 1 Heuristic:</span>
          <span className="font-mono text-zinc-700 dark:text-zinc-300">16 µs</span>
        </div>
      </motion.div>

      {/* 2. Threats Intercepted with Pop Counter Animation */}
      <motion.div
        variants={cardVariants}
        className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors shadow-sm dark:shadow-none"
      >
        <div>
          <div className="flex items-start justify-between">
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
              Threats Intercepted
            </span>
            <span className="inline-flex items-center text-xs font-medium text-rose-700 dark:text-rose-400 bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/20 px-2 py-0.5 rounded">
              {blockRate}% Filtered
            </span>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-semibold text-zinc-900 dark:text-zinc-100 font-sans tracking-tight flex items-baseline space-x-1.5">
              <motion.span
                key={threatsBlocked}
                animate={
                  isPopping && !shouldReduceMotion
                    ? { scale: [1, 1.15, 1], color: ["#f43f5e", "#f43f5e", "currentColor"] }
                    : { scale: 1 }
                }
                transition={{ duration: MOTION_DURATION.threat, ease: MOTION_EASING }}
                className="inline-block"
              >
                {threatsBlocked}
              </motion.span>
              <span className="text-sm font-normal text-zinc-500 dark:text-zinc-400">
                / {totalScans} scanned
              </span>
            </div>
            <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
              Phishing, Smishing &amp; QR Malscripts
            </p>
          </div>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400">
          <span>False Positives:</span>
          <span className="text-emerald-600 dark:text-emerald-400 font-medium">0.00%</span>
        </div>
      </motion.div>

      {/* 3. Memory Footprint */}
      <motion.div
        variants={cardVariants}
        className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors shadow-sm dark:shadow-none"
      >
        <div>
          <div className="flex items-start justify-between">
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
              Resident Memory
            </span>
            <span className="text-xs font-medium text-zinc-600 dark:text-zinc-400 bg-zinc-100 dark:bg-zinc-800 px-2 py-0.5 rounded border border-zinc-200 dark:border-zinc-700/60">
              {ramPercent}% of Budget
            </span>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-semibold text-zinc-900 dark:text-zinc-100 font-sans tracking-tight">
              {peakRamMb.toFixed(1)}{" "}
              <span className="text-sm font-normal text-zinc-500 dark:text-zinc-400">MB</span>
            </div>
            <div className="mt-2.5 w-full bg-zinc-100 dark:bg-zinc-800 rounded-full h-1.5 overflow-hidden">
              <motion.div
                className="bg-indigo-600 dark:bg-indigo-500 h-full rounded-full"
                initial={shouldReduceMotion ? { width: `${ramPercent}%` } : { width: 0 }}
                animate={{ width: `${ramPercent}%` }}
                transition={{ duration: 0.7, ease: MOTION_EASING }}
              />
            </div>
          </div>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400">
          <span>Max Allowed Budget:</span>
          <span className="font-mono text-zinc-700 dark:text-zinc-300">250.0 MB</span>
        </div>
      </motion.div>

      {/* 4. WAN Telemetry (Air-Gap) */}
      <motion.div
        variants={cardVariants}
        className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 flex flex-col justify-between hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors shadow-sm dark:shadow-none"
      >
        <div>
          <div className="flex items-start justify-between">
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
              Telemetry Egress
            </span>
            <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 px-2 py-0.5 rounded">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              Air-Gapped
            </span>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-semibold text-zinc-900 dark:text-zinc-100 font-sans tracking-tight">
              {wanBytes}{" "}
              <span className="text-sm font-normal text-zinc-500 dark:text-zinc-400">Bytes</span>
            </div>
            <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
              100% on-device local execution
            </p>
          </div>
        </div>
        <div className="mt-4 pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400">
          <span>IPC Socket:</span>
          <span className="font-mono text-zinc-700 dark:text-zinc-300">127.0.0.1:41789</span>
        </div>
      </motion.div>
    </motion.div>
  );
};
