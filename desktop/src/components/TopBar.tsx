import React from "react";
import { ChevronRight, Search, Zap, Sun, Moon, Plane } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { NavTab } from "./Sidebar";
import { MOTION_EASING, MOTION_DURATION } from "../styles/motion";

interface TopBarProps {
  activeTab: NavTab;
  latencyUs: number;
  wanBytes: number;
  airplaneMode?: boolean;
  theme: "dark" | "light";
  onToggleTheme: () => void;
  onOpenInspector: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  activeTab,
  latencyUs,
  wanBytes,
  airplaneMode = false,
  theme,
  onToggleTheme,
  onOpenInspector,
}) => {
  const getTabLabel = (tab: NavTab) => {
    switch (tab) {
      case "dashboard":
        return "Overview";
      case "inspector":
        return "Payload Inspector";
      case "processes":
        return "Process Monitor";
      case "logs":
        return "Forensic Vault";
      case "settings":
        return "Engine Configuration";
    }
  };

  const latencyMs = (latencyUs / 1000).toFixed(3);

  return (
    <header className="h-16 border-b border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 px-8 flex items-center justify-between sticky top-0 z-30 select-none transition-colors">
      {/* Breadcrumbs */}
      <div className="flex items-center space-x-2 text-sm">
        <span className="text-zinc-500 dark:text-zinc-400">Pocket Sparrow</span>
        <ChevronRight className="w-4 h-4 text-zinc-300 dark:text-zinc-600" />
        <span className="font-medium text-zinc-900 dark:text-zinc-100">{getTabLabel(activeTab)}</span>
      </div>

      {/* Right Controls & Status Badges */}
      <div className="flex items-center space-x-3">
        {/* Latency SLA Indicator */}
        <div className="hidden sm:flex items-center space-x-2 text-xs text-zinc-600 dark:text-zinc-400 border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/40 px-3 py-1.5 rounded-md">
          <Zap className="w-3.5 h-3.5 text-zinc-400 dark:text-zinc-400" />
          <span>P99 SLA:</span>
          <span className="font-medium text-zinc-900 dark:text-zinc-200 font-mono">{latencyMs} ms</span>
        </div>

        {/* Dynamic Air-Gap / Hardware Airplane Badge */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-md border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/40 text-xs">
          {airplaneMode ? (
            <Plane className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
          ) : (
            <span className="flex h-2 w-2 rounded-full bg-emerald-500" />
          )}
          <span className="font-medium text-zinc-900 dark:text-zinc-200">
            {airplaneMode ? "Airplane Mode (Air-Gapped)" : "Air-Gapped"}
          </span>
          <span className="text-zinc-400 dark:text-zinc-500 font-mono">({wanBytes} B WAN)</span>
        </div>

        {/* Action Button: Inspect Payload */}
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          transition={{ duration: MOTION_DURATION.micro, ease: MOTION_EASING }}
          onClick={onOpenInspector}
          className="flex items-center space-x-2 px-3.5 py-1.5 rounded-md bg-zinc-900 hover:bg-zinc-800 text-white dark:bg-zinc-100 dark:hover:bg-zinc-200 dark:text-zinc-900 text-xs font-medium transition-colors shadow-sm"
        >
          <Search className="w-3.5 h-3.5" />
          <span>Inspect Payload</span>
        </motion.button>

        {/* Theme Toggle Button (Sun / Moon with Rotation Transition) */}
        <motion.button
          whileTap={{ scale: 0.92 }}
          transition={{ duration: MOTION_DURATION.micro, ease: MOTION_EASING }}
          onClick={onToggleTheme}
          className="p-2 rounded-md border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/40 text-zinc-600 dark:text-zinc-300 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800/60 transition-colors overflow-hidden"
          title={`Switch to ${theme === "dark" ? "Light" : "Dark"} theme`}
          aria-label="Toggle theme"
        >
          <AnimatePresence mode="wait" initial={false}>
            {theme === "dark" ? (
              <motion.div
                key="sun"
                initial={{ rotate: -90, opacity: 0, scale: 0.75 }}
                animate={{ rotate: 0, opacity: 1, scale: 1 }}
                exit={{ rotate: 90, opacity: 0, scale: 0.75 }}
                transition={{ duration: MOTION_DURATION.micro, ease: MOTION_EASING }}
              >
                <Sun className="w-4 h-4 text-amber-400" />
              </motion.div>
            ) : (
              <motion.div
                key="moon"
                initial={{ rotate: 90, opacity: 0, scale: 0.75 }}
                animate={{ rotate: 0, opacity: 1, scale: 1 }}
                exit={{ rotate: -90, opacity: 0, scale: 0.75 }}
                transition={{ duration: MOTION_DURATION.micro, ease: MOTION_EASING }}
              >
                <Moon className="w-4 h-4 text-zinc-700" />
              </motion.div>
            )}
          </AnimatePresence>
        </motion.button>
      </div>
    </header>
  );
};
