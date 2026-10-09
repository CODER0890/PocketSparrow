import React from "react";
import {
  LayoutDashboard,
  Search,
  Cpu,
  Database,
  Sliders,
  Shield,
  WifiOff,
} from "lucide-react";

export type NavTab = "dashboard" | "inspector" | "processes" | "logs" | "settings";

interface SidebarProps {
  activeTab: NavTab;
  onSelectTab: (tab: NavTab) => void;
  airplaneMode: boolean;
  wanBytes: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onSelectTab,
  wanBytes,
}) => {
  const navItems = [
    { id: "dashboard" as NavTab, label: "Overview", icon: LayoutDashboard },
    { id: "inspector" as NavTab, label: "Payload Inspector", icon: Search },
    { id: "processes" as NavTab, label: "Process Monitor", icon: Cpu, badge: "1 Alert" },
    { id: "logs" as NavTab, label: "Forensic Vault", icon: Database },
    { id: "settings" as NavTab, label: "Engine Settings", icon: Sliders },
  ];

  return (
    <aside className="w-64 border-r border-zinc-800 bg-zinc-950 flex flex-col justify-between select-none h-screen sticky top-0 shrink-0 z-40">
      <div>
        {/* Workspace / Product Header */}
        <div className="h-16 px-6 border-b border-zinc-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="h-8 w-8 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-100">
              <Shield className="w-4 h-4 text-zinc-200" />
            </div>
            <div>
              <div className="font-semibold text-zinc-100 text-sm tracking-tight">
                Pocket Sparrow
              </div>
              <div className="text-xs text-zinc-400">Threat Defense</div>
            </div>
          </div>
          <span className="text-[10px] font-medium text-zinc-400 px-1.5 py-0.5 rounded bg-zinc-900 border border-zinc-800">
            v0.1.0
          </span>
        </div>

        {/* Navigation Links */}
        <div className="px-3 py-6">
          <div className="px-3 pb-2 text-xs font-medium uppercase tracking-wider text-zinc-400">
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
                  className={`w-full flex items-center justify-between px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-zinc-800 text-zinc-100 shadow-sm"
                      : "text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/60"
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    <Icon className="w-4 h-4 text-zinc-400" />
                    <span>{item.label}</span>
                  </div>
                  {item.badge && (
                    <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20">
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Footer Status Panel */}
      <div className="p-4 border-t border-zinc-800">
        <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-3.5 space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 text-xs text-zinc-300 font-medium">
              <WifiOff className="w-3.5 h-3.5 text-emerald-400" />
              <span>Offline Isolation</span>
            </div>
            <span className="inline-flex items-center gap-1.5 text-[11px] text-emerald-400 font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
              Active
            </span>
          </div>
          <p className="text-xs text-zinc-400 leading-relaxed">
            All models execute locally on-device. Zero network packets sent to WAN.
          </p>
          <div className="pt-2 border-t border-zinc-800/60 flex items-center justify-between text-xs text-zinc-400 font-mono">
            <span>WAN Egress:</span>
            <span className="text-zinc-200 font-medium">{wanBytes} Bytes</span>
          </div>
        </div>
      </div>
    </aside>
  );
};
