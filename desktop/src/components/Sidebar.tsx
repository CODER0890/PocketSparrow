import React from "react";
import {
  LayoutDashboard,
  SearchCode,
  Cpu,

  Database,
  Settings,
  ShieldCheck,
  Plane,
  WifiOff,

  Fingerprint,
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
  airplaneMode,
  wanBytes,
}) => {
  const navItems = [
    { id: "dashboard" as NavTab, label: "Dashboard", icon: LayoutDashboard, badge: undefined },
    { id: "inspector" as NavTab, label: "Live Inspector", icon: SearchCode, badge: "Real-Time" },
    { id: "processes" as NavTab, label: "Process Auditor", icon: Cpu, badge: "1 Alert" },
    { id: "logs" as NavTab, label: "Encrypted Vault", icon: Database, badge: undefined },
    { id: "settings" as NavTab, label: "Engine Config", icon: Settings, badge: undefined },
  ];

  return (
    <aside className="w-64 border-r border-surface-border bg-background-subtle/80 backdrop-blur-xl flex flex-col justify-between select-none h-screen sticky top-0 shrink-0 z-40">
      {/* Brand Header */}
      <div>
        <div className="p-5 border-b border-surface-border">
          <div className="flex items-center space-x-3">
            <div className="h-9 w-9 rounded-lg bg-surface border border-surface-borderHover flex items-center justify-center text-accent shadow-sm">
              <ShieldCheck className="w-5 h-5 text-accent" strokeWidth={1.75} />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="font-semibold text-brand-text tracking-tight text-sm">
                  Pocket Sparrow
                </span>
                <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-surface border border-surface-border text-brand-muted">
                  v0.1
                </span>
              </div>
              <p className="text-[11px] text-brand-muted tracking-tight">On-Device Threat Shield</p>
            </div>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="p-3 space-y-1">
          <div className="px-3 py-1.5 text-[10px] font-mono uppercase tracking-wider text-brand-faint">
            Security Core
          </div>
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onSelectTab(item.id)}
                className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium transition-all group ${
                  isActive
                    ? "bg-surface-active text-brand-text shadow-sm border border-surface-borderHover"
                    : "text-brand-secondary hover:text-brand-text hover:bg-surface-hover"
                }`}
              >
                <div className="flex items-center space-x-2.5">
                  <Icon
                    className={`w-4 h-4 transition-colors ${
                      isActive ? "text-accent" : "text-brand-muted group-hover:text-brand-secondary"
                    }`}
                    strokeWidth={1.75}
                  />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[10px] font-mono px-1.5 py-0.5 rounded-full ${
                      item.badge.includes("Alert")
                        ? "bg-status-dangerBg text-status-danger border border-status-dangerBorder"
                        : "bg-surface border border-surface-border text-brand-muted"
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Offline Air-Gap Proof Card */}
      <div className="p-4 border-t border-surface-border space-y-3">
        <div className="rounded-xl border border-surface-border bg-surface p-3.5 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-1.5 text-xs font-medium text-brand-text">
              <Plane className="w-3.5 h-3.5 text-status-safe" strokeWidth={1.75} />
              <span>{airplaneMode ? "Hardware Airgap" : "100% Offline"}</span>
            </div>
            <span className="flex h-2 w-2 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-safe opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-status-safe"></span>
            </span>
          </div>

          <p className="text-[11px] text-brand-muted leading-relaxed">
            Evaluation is 100% on-device. Zero telemetry sockets permitted.
          </p>

          <div className="pt-2 border-t border-surface-border flex items-center justify-between text-[10px] font-mono text-brand-faint">
            <span className="flex items-center gap-1">
              <WifiOff className="w-3 h-3 text-brand-muted" />
              WAN: {wanBytes} Bytes
            </span>
            <span className="text-status-safe font-semibold">AIR-GAPPED</span>
          </div>
        </div>

        {/* Daemon Signature */}
        <div className="px-1 flex items-center justify-between text-[10px] font-mono text-brand-faint">
          <span className="flex items-center gap-1">
            <Fingerprint className="w-3 h-3" />
            daemon-01
          </span>
          <span>SHA-256 Valid</span>
        </div>
      </div>
    </aside>
  );
};
