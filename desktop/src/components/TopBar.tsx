import React from "react";
import { ChevronRight, Search, Zap } from "lucide-react";
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
    <header className="h-16 border-b border-zinc-800 bg-zinc-950 px-8 flex items-center justify-between sticky top-0 z-30 select-none">
      {/* Breadcrumbs */}
      <div className="flex items-center space-x-2 text-sm">
        <span className="text-zinc-400">Pocket Sparrow</span>
        <ChevronRight className="w-4 h-4 text-zinc-600" />
        <span className="font-medium text-zinc-100">{getTabLabel(activeTab)}</span>
      </div>

      {/* Right Controls & Status Badges */}
      <div className="flex items-center space-x-4">
        {/* Flat Latency Metric */}
        <div className="hidden sm:flex items-center space-x-2 text-xs text-zinc-400 border border-zinc-800 bg-zinc-900/40 px-3 py-1.5 rounded-md">
          <Zap className="w-3.5 h-3.5 text-zinc-400" />
          <span>P99 SLA:</span>
          <span className="font-medium text-zinc-200 font-mono">{latencyMs} ms</span>
        </div>

        {/* Flat Offline Badge */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-md border border-zinc-800 bg-zinc-900/40 text-xs">
          <span className="flex h-2 w-2 rounded-full bg-emerald-500" />
          <span className="font-medium text-zinc-200">Air-Gapped</span>
          <span className="text-zinc-500 font-mono">({wanBytes} B WAN)</span>
        </div>

        {/* Action Button */}
        <button
          onClick={onOpenInspector}
          className="flex items-center space-x-2 px-3 py-1.5 rounded-md bg-zinc-100 hover:bg-zinc-200 text-zinc-900 text-xs font-medium transition-colors shadow-sm"
        >
          <Search className="w-3.5 h-3.5" />
          <span>Inspect Payload</span>
        </button>
      </div>
    </header>
  );
};
