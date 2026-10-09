import React from "react";
import {
  ShieldCheck,
  Zap,


  ChevronRight,

  Search,
} from "lucide-react";
import { NavTab } from "./Sidebar";

interface TopBarProps {
  activeTab: NavTab;
  latencyUs: number;
  wanBytes: number;
  onOpenInspector: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  activeTab,
  latencyUs,
  wanBytes,
  onOpenInspector,
}) => {
  const getTabLabel = (tab: NavTab) => {
    switch (tab) {
      case "dashboard":
        return "System Overview & Telemetry";
      case "inspector":
        return "Live Threat Inspector";
      case "processes":
        return "Process Behavior Auditor";
      case "logs":
        return "Encrypted Vault History";
      case "settings":
        return "Engine Configuration";
    }
  };

  const latencyMs = (latencyUs / 1000).toFixed(3);

  return (
    <header className="h-14 border-b border-surface-border bg-background/80 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-30">
      {/* Breadcrumbs & Title */}
      <div className="flex items-center space-x-3 text-xs">
        <div className="flex items-center space-x-1.5 text-brand-muted">
          <span>Security Center</span>
          <ChevronRight className="w-3.5 h-3.5 text-brand-faint" />
          <span className="text-brand-secondary">On-Device Shield</span>
          <ChevronRight className="w-3.5 h-3.5 text-brand-faint" />
        </div>
        <span className="font-semibold text-brand-text tracking-tight">
          {getTabLabel(activeTab)}
        </span>
      </div>

      {/* Global Indicators & Certificates */}
      <div className="flex items-center space-x-3">
        {/* Real-time Response SLA Indicator */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg border border-surface-border bg-surface text-xs font-mono">
          <Zap className="w-3.5 h-3.5 text-accent" strokeWidth={1.75} />
          <span className="text-brand-muted text-[11px]">SLA Response:</span>
          <span className="text-brand-text font-semibold">{latencyMs} ms</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-status-safeBg text-status-safe border border-status-safeBorder font-semibold">
            P99 PASS
          </span>
        </div>

        {/* Cryptographic Zero-Telemetry Certificate Badge */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg border border-surface-border bg-surface text-xs">
          <div className="flex items-center space-x-1.5">
            <ShieldCheck className="w-3.5 h-3.5 text-status-safe" strokeWidth={1.75} />
            <span className="text-brand-muted text-[11px] font-mono">AIRGAP PROOF:</span>
            <span className="font-mono text-status-safe font-semibold text-[11px]">
              {wanBytes} B WAN
            </span>
          </div>
          <span className="text-brand-faint">|</span>
          <span className="text-[10px] font-mono text-brand-muted hidden sm:inline">
            CERT-SHA256: 9F2A...3B01
          </span>
        </div>

        {/* Quick Launch Inspector Button */}
        <button
          onClick={onOpenInspector}
          className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-surface border border-surface-borderHover hover:border-accent text-brand-text hover:text-accent text-xs font-medium transition shadow-sm"
        >
          <Search className="w-3.5 h-3.5" />
          <span className="hidden md:inline">Inspect Target</span>
        </button>
      </div>
    </header>
  );
};
