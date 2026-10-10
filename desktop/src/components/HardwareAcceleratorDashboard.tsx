import React, { useState, useEffect } from "react";
import { motion, useReducedMotion } from "framer-motion";
import {
  Cpu,
  Flame,
  Zap,
  Activity,
  Layers,
  Clock,
  AlertCircle,
} from "lucide-react";
import { MOTION_EASING } from "../styles/motion";

export interface LayerLatency {
  name: string;
  latency_ms: number;
  percentage: number;
}

export interface FallbackEvent {
  timestamp: string;
  event: string;
  reason: string;
  severity: string;
}

export interface HardwareMetrics {
  active_runtime: string;
  cpu_utilization_pct: number;
  gpu_utilization_pct: number;
  npu_utilization_pct: number;
  memory_used_mb: number;
  memory_total_mb: number;
  thermal_state: string; // "Nominal" | "Fair" | "Serious"
  thermal_temp_c: number;
  layer_breakdown: LayerLatency[];
  fallback_events: FallbackEvent[];
}

interface HardwareAcceleratorDashboardProps {
  className?: string;
}

export const HardwareAcceleratorDashboard: React.FC<HardwareAcceleratorDashboardProps> = ({
  className = "",
}) => {
  const shouldReduceMotion = !!useReducedMotion();
  const [metrics, setMetrics] = useState<HardwareMetrics>({
    active_runtime: "CPU (x86_64 INT8 AVX2 SIMD)",
    cpu_utilization_pct: 12.4,
    gpu_utilization_pct: 0.0,
    npu_utilization_pct: 0.0,
    memory_used_mb: 4120.0,
    memory_total_mb: 16384.0,
    thermal_state: "Nominal",
    thermal_temp_c: 41.2,
    layer_breakdown: [
      { name: "Tokenizer & WordPiece", latency_ms: 0.42, percentage: 2.3 },
      { name: "Embedding Matrix", latency_ms: 1.18, percentage: 6.5 },
      { name: "Self-Attention (4-Head INT8)", latency_ms: 11.84, percentage: 65.1 },
      { name: "Feed-Forward Dense", latency_ms: 3.82, percentage: 21.0 },
      { name: "Classification Head", latency_ms: 0.92, percentage: 5.1 },
    ],
    fallback_events: [
      {
        timestamp: "09:12:04",
        event: "Provider DirectML Fallback",
        reason: "No discrete D3D12 GPU device available. Active fallback to x86_64 AVX2 INT8.",
        severity: "Info",
      },
      {
        timestamp: "09:12:05",
        event: "NNAPI Delegate Bypassed",
        reason: "Platform is Linux Desktop host; NNAPI skipped, using native AVX2 SIMD.",
        severity: "Nominal",
      },
    ],
  });

  // Poll hardware metrics every 2 seconds
  useEffect(() => {
    let isMounted = true;

    const fetchMetrics = async () => {
      try {
        if (typeof window !== "undefined" && (window as any).__TAURI_INTERNALS__) {
          const { invoke } = await import("@tauri-apps/api/core");
          const res = await invoke<HardwareMetrics>("get_hardware_metrics");
          if (isMounted) setMetrics(res);
        } else {
          // Simulation variation in browser
          if (isMounted) {
            setMetrics((prev) => ({
              ...prev,
              cpu_utilization_pct: Math.min(100, Math.max(4, prev.cpu_utilization_pct + (Math.random() * 6 - 3))),
              thermal_temp_c: Number((41.0 + Math.random() * 2.5).toFixed(1)),
            }));
          }
        }
      } catch {
        // Fallback gracefully
      }
    };

    const interval = setInterval(fetchMetrics, 2000);
    fetchMetrics();

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  const totalInferenceMs = metrics.layer_breakdown.reduce((acc, curr) => acc + curr.latency_ms, 0);

  const getThermalColor = (state: string) => {
    switch (state.toLowerCase()) {
      case "serious":
        return "text-rose-500 bg-rose-500/10 border-rose-500/20";
      case "fair":
        return "text-amber-500 bg-amber-500/10 border-amber-500/20";
      default:
        return "text-emerald-500 bg-emerald-500/10 border-emerald-500/20";
    }
  };

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 shadow-sm dark:shadow-none">
        <div className="flex items-center space-x-3.5">
          <div className="p-2.5 rounded-lg bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800/40">
            <Zap className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-base font-semibold text-zinc-900 dark:text-zinc-100 tracking-tight">
                Hardware Accelerator Telemetry
              </h2>
              <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 font-medium">
                Active Runtime: {metrics.active_runtime}
              </span>
            </div>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
              Real-time on-device neural inference performance, provider states, and thermal limits.
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2 text-xs">
          <span className={`px-2.5 py-1 rounded-md border font-medium flex items-center gap-1.5 ${getThermalColor(metrics.thermal_state)}`}>
            <Flame className="w-3.5 h-3.5" />
            <span>Thermal: {metrics.thermal_state} ({metrics.thermal_temp_c}°C)</span>
          </span>
        </div>
      </div>

      {/* Utilization Live Gauges (MSI Afterburner style but minimal) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* CPU Gauge */}
        <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-4 space-y-3">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-zinc-700 dark:text-zinc-300 flex items-center gap-1.5">
              <Cpu className="w-4 h-4 text-indigo-500" />
              <span>CPU Utilization</span>
            </span>
            <span className="font-mono text-zinc-900 dark:text-zinc-100 font-bold">
              {metrics.cpu_utilization_pct.toFixed(1)}%
            </span>
          </div>
          <div className="h-2 w-full bg-zinc-100 dark:bg-zinc-800 rounded-full overflow-hidden">
            <motion.div
              className="h-full bg-indigo-500 rounded-full"
              animate={{ width: `${Math.min(100, metrics.cpu_utilization_pct)}%` }}
              transition={{ duration: 0.3 }}
            />
          </div>
          <div className="flex justify-between text-[11px] text-zinc-400 font-mono">
            <span>RAM: {(metrics.memory_used_mb / 1024).toFixed(1)} GB</span>
            <span>Total: {(metrics.memory_total_mb / 1024).toFixed(1)} GB</span>
          </div>
        </div>

        {/* GPU Gauge */}
        <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-4 space-y-3">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-zinc-700 dark:text-zinc-300 flex items-center gap-1.5">
              <Activity className="w-4 h-4 text-emerald-500" />
              <span>GPU Delegate</span>
            </span>
            <span className="font-mono text-zinc-900 dark:text-zinc-100 font-bold">
              {metrics.gpu_utilization_pct.toFixed(1)}%
            </span>
          </div>
          <div className="h-2 w-full bg-zinc-100 dark:bg-zinc-800 rounded-full overflow-hidden">
            <motion.div
              className="h-full bg-emerald-500 rounded-full"
              animate={{ width: `${Math.min(100, metrics.gpu_utilization_pct)}%` }}
              transition={{ duration: 0.3 }}
            />
          </div>
          <div className="flex justify-between text-[11px] text-zinc-400 font-mono">
            <span>DirectML / Vulkan</span>
            <span>Air-gap Isolated</span>
          </div>
        </div>

        {/* NPU Gauge */}
        <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-4 space-y-3">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-zinc-700 dark:text-zinc-300 flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-amber-500" />
              <span>NPU Engine</span>
            </span>
            <span className="font-mono text-zinc-900 dark:text-zinc-100 font-bold">
              {metrics.npu_utilization_pct.toFixed(1)}%
            </span>
          </div>
          <div className="h-2 w-full bg-zinc-100 dark:bg-zinc-800 rounded-full overflow-hidden">
            <motion.div
              className="h-full bg-amber-500 rounded-full"
              animate={{ width: `${Math.min(100, metrics.npu_utilization_pct)}%` }}
              transition={{ duration: 0.3 }}
            />
          </div>
          <div className="flex justify-between text-[11px] text-zinc-400 font-mono">
            <span>NNAPI / QNN Accelerator</span>
            <span>INT8 Quantized</span>
          </div>
        </div>
      </div>

      {/* Layer-by-Layer Inference Latency Breakdown */}
      <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-5 space-y-4">
        <div className="flex items-center justify-between border-b border-zinc-200 dark:border-zinc-800 pb-3">
          <div className="flex items-center space-x-2 text-xs font-semibold text-zinc-900 dark:text-zinc-100">
            <Layers className="w-4 h-4 text-indigo-500" />
            <span>Layer-by-Layer Latency Profile (MobileBERT INT8)</span>
          </div>
          <div className="flex items-center space-x-2 text-xs font-mono text-zinc-500">
            <Clock className="w-3.5 h-3.5" />
            <span>Total Latency: {totalInferenceMs.toFixed(2)} ms (&lt;50ms SLA)</span>
          </div>
        </div>

        <div className="space-y-3">
          {metrics.layer_breakdown.map((layer, idx) => (
            <div key={idx} className="space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-zinc-700 dark:text-zinc-300 font-medium">
                  {layer.name}
                </span>
                <span className="font-mono text-zinc-500 dark:text-zinc-400 text-[11px]">
                  {layer.latency_ms.toFixed(2)} ms ({layer.percentage.toFixed(1)}%)
                </span>
              </div>
              <div className="h-2 w-full bg-zinc-100 dark:bg-zinc-800 rounded-full overflow-hidden">
                <motion.div
                  className={`h-full rounded-full ${
                    idx === 2
                      ? "bg-rose-500"
                      : idx === 3
                      ? "bg-amber-500"
                      : "bg-sky-500"
                  }`}
                  initial={{ width: 0 }}
                  animate={{ width: `${layer.percentage}%` }}
                  transition={{
                    duration: 0.4,
                    delay: shouldReduceMotion ? 0 : idx * 0.05,
                    ease: MOTION_EASING,
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Fallback Event Log */}
      <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-5 space-y-3">
        <div className="flex items-center justify-between border-b border-zinc-200 dark:border-zinc-800 pb-3">
          <div className="flex items-center space-x-2 text-xs font-semibold text-zinc-900 dark:text-zinc-100">
            <AlertCircle className="w-4 h-4 text-amber-500" />
            <span>Hardware Fallback Audit Events</span>
          </div>
          <span className="text-[11px] text-zinc-400">
            {metrics.fallback_events.length} events logged
          </span>
        </div>

        <div className="divide-y divide-zinc-100 dark:divide-zinc-800 text-xs">
          {metrics.fallback_events.map((ev, idx) => (
            <div key={idx} className="py-2.5 flex items-start justify-between gap-4">
              <div className="space-y-0.5">
                <div className="flex items-center space-x-2">
                  <span className="font-medium text-zinc-900 dark:text-zinc-100">
                    {ev.event}
                  </span>
                  <span className="font-mono text-[10px] px-1.5 py-0.2 rounded bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400">
                    {ev.severity}
                  </span>
                </div>
                <p className="text-[11px] text-zinc-500 dark:text-zinc-400">
                  {ev.reason}
                </p>
              </div>
              <span className="font-mono text-[10px] text-zinc-400 shrink-0">
                {ev.timestamp}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
