import React from "react";
import {
  LayoutDashboard,
  Search,
  Cpu,
  Database,
  Sliders,
  Mail,
  Zap,
  Activity,
  ShieldAlert,
  Radio,
} from "lucide-react";
import { motion } from "framer-motion";
import { MOTION_DURATION } from "../styles/motion";
import logoImg from "../assets/logo.png";

export type NavTab =
  | "dashboard"
  | "live_shield"
  | "inspector"
  | "network"
  | "comm_shield"
  | "email"
  | "hardware"
  | "processes"
  | "logs"
  | "settings";

interface SidebarProps {
  activeTab: NavTab;
  onSelectTab: (tab: NavTab) => void;
  airplaneMode: boolean;
  wanBytes: number;
  suspiciousCount?: number;
  emailThreatCount?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onSelectTab,
  suspiciousCount = 0,
  emailThreatCount = 0,
}) => {
  const navItems = [
    { id: "dashboard" as NavTab, label: "Overview", icon: LayoutDashboard },
    { id: "live_shield" as NavTab, label: "Live Shield", icon: Radio },
    { id: "inspector" as NavTab, label: "Payload Inspector", icon: Search },
    { id: "network" as NavTab, label: "Network Interceptor", icon: Activity },
    {
      id: "comm_shield" as NavTab,
      label: "Communication Shield",
      icon: ShieldAlert,
    },
    {
      id: "email" as NavTab,
      label: "Email Shield",
      icon: Mail,
      badge: emailThreatCount > 0 ? `${emailThreatCount} Threat` : undefined,
    },
    { id: "hardware" as NavTab, label: "Hardware Accel", icon: Zap },
    {
      id: "processes" as NavTab,
      label: "Process Monitor",
      icon: Cpu,
      badge: suspiciousCount > 0 ? `${suspiciousCount} Alert` : undefined,
    },
    { id: "logs" as NavTab, label: "Forensic Vault", icon: Database },
    { id: "settings" as NavTab, label: "Engine Settings", icon: Sliders },
  ];

  return (
    <aside className="w-64 border-r border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 flex flex-col justify-between select-none h-screen sticky top-0 shrink-0 z-40 transition-colors overflow-y-auto">
      <div>
        {/* Workspace / Product Header */}
        <div className="h-16 px-6 border-b border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="h-9 w-9 rounded-lg bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 flex items-center justify-center overflow-hidden p-1 shadow-sm">
              <img src={logoImg} alt="Pocket Sparrow Logo" className="w-full h-full object-contain" />
            </div>
            <div>
              <div className="font-semibold text-zinc-900 dark:text-zinc-100 text-sm tracking-tight">
                Pocket Sparrow
              </div>
              <div className="text-xs text-zinc-500 dark:text-zinc-400">Threat Defense</div>
            </div>
          </div>
          <span className="text-[10px] font-medium text-zinc-500 dark:text-zinc-400 px-1.5 py-0.5 rounded bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800">
            v0.1.0
          </span>
        </div>

        {/* Navigation Links with Layout Sliding Indicator */}
        <div className="px-3 py-5">
          <div className="px-3 pb-2 text-xs font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
            Platform
          </div>
          <nav className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => onSelectTab(item.id)}
                  className={`relative w-full flex items-center justify-between px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive
                      ? "text-zinc-900 dark:text-zinc-100"
                      : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200"
                  }`}
                >
                  {/* Sliding layout active indicator */}
                  {isActive && (
                    <motion.div
                      layoutId="activeSidebarIndicator"
                      className="absolute inset-0 rounded-md bg-zinc-100 dark:bg-zinc-800 shadow-sm"
                      transition={{
                        type: "spring",
                        stiffness: 420,
                        damping: 32,
                        duration: MOTION_DURATION.macro,
                      }}
                    />
                  )}

                  <div className="relative z-10 flex items-center space-x-3">
                    <Icon className="w-4 h-4 text-zinc-500 dark:text-zinc-400" />
                    <span>{item.label}</span>
                  </div>

                  {item.badge && (
                    <motion.span
                      animate={
                        item.badge.includes("Alert") || item.badge.includes("Threat")
                          ? { opacity: [0.8, 1, 0.8] }
                          : undefined
                      }
                      transition={{
                        duration: 2.2,
                        repeat: Infinity,
                        ease: "easeInOut",
                      }}
                      className="relative z-10 text-[11px] font-medium px-2 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-200 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20"
                    >
                      {item.badge}
                    </motion.span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Clean Status Footer */}
      <div className="p-4 border-t border-zinc-200 dark:border-zinc-800 flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400">
        <div className="flex items-center space-x-2">
          <span className="w-2 h-2 rounded-full bg-emerald-500" />
          <span className="font-medium text-[11px]">Air-Gap Active</span>
        </div>
        <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-zinc-100 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700">
          0 WAN B
        </span>
      </div>
    </aside>
  );
};
