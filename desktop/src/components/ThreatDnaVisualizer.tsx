import React, { useState } from "react";
import { motion, useReducedMotion } from "framer-motion";
import { useThreatDNA, ThreatToken } from "../hooks/useThreatDNA";
import { Dna, Info, Sparkles, AlertCircle } from "lucide-react";
import { MOTION_EASING } from "../styles/motion";

interface ThreatDnaVisualizerProps {
  payload: string;
  category?: string;
  className?: string;
}

export const ThreatDnaVisualizer: React.FC<ThreatDnaVisualizerProps> = ({
  payload,
  category,
  className = "",
}) => {
  const shouldReduceMotion = !!useReducedMotion();
  const { tokens, maxWeight, dominantTier } = useThreatDNA(payload);
  const [activeToken, setActiveToken] = useState<{
    token: ThreatToken;
    index: number;
    rect: DOMRect | null;
  } | null>(null);

  if (!payload || tokens.length === 0) return null;

  // Calculates background and text styling based on attention weight and tier
  const getTokenStyle = (token: ThreatToken) => {
    const w = token.weight;
    if (w >= 0.7) {
      // High attention -> Rose alert
      return {
        backgroundColor: `rgba(244, 63, 94, ${Math.min(0.85, 0.2 + w * 0.65)})`,
        color: "#ffffff",
        borderColor: "rgba(244, 63, 94, 0.6)",
      };
    } else if (w >= 0.4) {
      // Moderate attention -> Amber warning
      return {
        backgroundColor: `rgba(245, 158, 11, ${Math.min(0.7, 0.15 + w * 0.5)})`,
        color: "#ffffff",
        borderColor: "rgba(245, 158, 11, 0.5)",
      };
    } else {
      // Low attention -> Cool cyan/blue/slate
      return {
        backgroundColor: `rgba(2, 132, 199, ${Math.max(0.08, w * 0.3)})`,
        color: undefined, // inherit theme
        borderColor: "rgba(2, 132, 199, 0.2)",
      };
    }
  };

  return (
    <div
      className={`rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50/70 dark:bg-zinc-900/60 p-4 space-y-3.5 transition-colors ${className}`}
    >
      {/* Header & Legend */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-zinc-200 dark:border-zinc-800 pb-3">
        <div className="flex items-center space-x-2">
          <div className="p-1.5 rounded-md bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800/40">
            <Dna className="w-3.5 h-3.5" />
          </div>
          <div>
            <h4 className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
              <span>Threat DNA &amp; Attention Heatmap (XAI)</span>
              {category && (
                <span className="text-[10px] px-1.5 py-0.2 rounded font-mono bg-zinc-200 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300">
                  {category}
                </span>
              )}
            </h4>
            <p className="text-[11px] text-zinc-500 dark:text-zinc-400">
              Token-level neural attention and heuristic attribution
            </p>
          </div>
        </div>

        {/* Legend */}
        <div className="flex items-center space-x-3 text-[11px]">
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded bg-sky-500/30 border border-sky-400" />
            <span className="text-zinc-600 dark:text-zinc-400">Cool (Safe/Neutral)</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded bg-rose-500/80 border border-rose-500" />
            <span className="text-zinc-600 dark:text-zinc-400 font-medium text-rose-600 dark:text-rose-400">
              Rose (Threat Focus)
            </span>
          </div>
        </div>
      </div>

      {/* Heatmap Token Stream */}
      <div className="relative font-mono text-xs leading-relaxed break-all bg-white dark:bg-zinc-950 rounded-md p-3 border border-zinc-200 dark:border-zinc-800/80 select-text">
        <div className="flex flex-wrap gap-1 items-baseline">
          {tokens.map((token, idx) => {
            const style = getTokenStyle(token);
            const isHigh = token.weight >= 0.7;

            return (
              <motion.span
                key={`${token.text}-${idx}`}
                initial={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, y: 3 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  duration: 0.4, // 400ms reveal animation
                  delay: shouldReduceMotion ? 0 : Math.min(0.4, idx * 0.02),
                  ease: MOTION_EASING,
                }}
                onMouseEnter={(e) => {
                  const rect = e.currentTarget.getBoundingClientRect();
                  setActiveToken({ token, index: idx, rect });
                }}
                onMouseLeave={() => setActiveToken(null)}
                style={style}
                className={`relative px-1 py-0.5 rounded cursor-help border transition-transform duration-150 inline-block ${
                  isHigh ? "font-semibold shadow-sm hover:scale-105" : "hover:brightness-95 dark:hover:brightness-110"
                } ${
                  token.tier === 1
                    ? "underline decoration-dotted decoration-zinc-400 dark:decoration-zinc-500 underline-offset-2"
                    : ""
                }`}
              >
                {token.text}
              </motion.span>
            );
          })}
        </div>

        {/* Hover Tooltip Popover */}
        {activeToken && (
          <motion.div
            initial={{ opacity: 0, y: 4, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.96 }}
            transition={{ duration: 0.15 }}
            className="absolute left-3 right-3 bottom-full mb-2 z-30 p-2.5 rounded-md bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-xl border border-zinc-700 dark:border-zinc-300 text-xs pointer-events-none"
          >
            <div className="flex items-center justify-between gap-2 border-b border-zinc-800 dark:border-zinc-200 pb-1.5 mb-1.5 font-sans">
              <div className="flex items-center space-x-1.5 font-medium">
                {activeToken.token.weight >= 0.7 ? (
                  <AlertCircle className="w-3.5 h-3.5 text-rose-400 dark:text-rose-600" />
                ) : (
                  <Sparkles className="w-3.5 h-3.5 text-sky-400 dark:text-sky-600" />
                )}
                <span>Token: &quot;{activeToken.token.text}&quot;</span>
              </div>
              <span className="font-mono text-[11px] px-1.5 py-0.5 rounded bg-zinc-800 dark:bg-zinc-200">
                Score: {(activeToken.token.weight * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex items-center justify-between text-[11px] font-sans">
              <span className="opacity-90">{activeToken.token.source}</span>
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-zinc-800/80 dark:bg-zinc-200/80 font-mono">
                {activeToken.token.tier === 1 ? "Tier 1 Heuristic" : "Tier 2 Transformer"}
              </span>
            </div>
          </motion.div>
        )}
      </div>

      {/* Attribution Footer Bar */}
      <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-zinc-500 dark:text-zinc-400 pt-1">
        <div className="flex items-center space-x-2">
          <Info className="w-3.5 h-3.5" />
          <span>
            Dominant Engine:{" "}
            <strong className="text-zinc-700 dark:text-zinc-300">
              {dominantTier === 1 ? "Tier 1 Heuristic Rules" : "Tier 2 MobileBERT INT8"}
            </strong>
          </span>
        </div>
        <div className="flex items-center space-x-3 font-mono text-[10px]">
          <span>Max Attention: {(maxWeight * 100).toFixed(0)}%</span>
          <span>Tokens: {tokens.length}</span>
        </div>
      </div>
    </div>
  );
};
