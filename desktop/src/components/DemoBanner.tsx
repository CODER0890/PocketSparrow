import React from "react";
import { AlertTriangle, X } from "lucide-react";

interface DemoBannerProps {
  onDismiss: () => void;
  onQuickDemo?: () => void;
}

export const DemoBanner: React.FC<DemoBannerProps> = ({ onDismiss, onQuickDemo }) => {
  return (
    <div className="w-full rounded-lg border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-xs flex items-center justify-between transition-colors">
      <div className="flex items-center space-x-3">
        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-500/20 text-amber-600 dark:text-amber-400">
          <AlertTriangle className="h-3.5 w-3.5" />
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center sm:space-x-2">
          <span className="font-semibold text-amber-900 dark:text-amber-300 uppercase tracking-wider text-[11px]">
            DEMO MODE ACTIVE
          </span>
          <span className="hidden sm:inline text-amber-600 dark:text-amber-500">•</span>
          <span className="text-amber-800 dark:text-amber-200">
            Simulating Phish URL, Bank SMS, Fake QR &amp; Banking Trojan payloads in isolated memory.
          </span>
        </div>
      </div>

      <div className="flex items-center space-x-3 shrink-0 ml-4">
        {onQuickDemo && (
          <button
            onClick={onQuickDemo}
            className="hidden md:inline-flex px-2.5 py-1 rounded bg-amber-600/20 hover:bg-amber-600/30 text-amber-900 dark:text-amber-200 font-medium text-[11px] transition-colors"
          >
            Trigger Test Vector
          </button>
        )}
        <button
          onClick={onDismiss}
          className="p-1 rounded text-amber-700 dark:text-amber-400 hover:text-amber-900 dark:hover:text-amber-200 hover:bg-amber-500/20 transition-colors"
          title="Dismiss banner"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
};
