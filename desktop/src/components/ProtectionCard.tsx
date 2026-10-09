import React from "react";
import { ShieldCheck, ClipboardCheck } from "lucide-react";

interface ProtectionCardProps {
  clipboardShield: boolean;
  onToggleClipboardShield: (enabled: boolean) => void;
  latencyUs: number;
}

export const ProtectionCard: React.FC<ProtectionCardProps> = ({
  clipboardShield,
  onToggleClipboardShield,
  latencyUs,
}) => {
  const latencyMs = (latencyUs / 1000).toFixed(3);

  return (
    <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-5 flex flex-col justify-between transition-colors shadow-sm dark:shadow-none">
      <div>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-md bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 text-emerald-600 dark:text-emerald-400">
              <ShieldCheck className="h-4 w-4" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-semibold text-sm text-zinc-900 dark:text-zinc-100">
                  Protection Active
                </span>
                <span className="flex h-2 w-2 rounded-full bg-emerald-500 ring-2 ring-emerald-500/20 animate-pulse" />
              </div>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">
                On-device heuristics + INT8 runtime
              </p>
            </div>
          </div>

          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-zinc-100 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700/60 text-zinc-600 dark:text-zinc-300">
            {latencyMs} ms
          </span>
        </div>

        <p className="mt-3.5 text-xs text-zinc-600 dark:text-zinc-400 leading-relaxed">
          Inbound links, SMS messages, and QR streams are evaluated before execution with 0 WAN telemetry.
        </p>
      </div>

      <div className="mt-4 pt-3.5 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <ClipboardCheck className="h-3.5 w-3.5 text-zinc-500 dark:text-zinc-400" />
          <span className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
            Auto Clipboard Shield
          </span>
        </div>

        {/* Accessible Toggle Switch */}
        <button
          type="button"
          role="switch"
          aria-checked={clipboardShield}
          onClick={() => onToggleClipboardShield(!clipboardShield)}
          className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
            clipboardShield ? "bg-emerald-600" : "bg-zinc-300 dark:bg-zinc-700"
          }`}
        >
          <span
            className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
              clipboardShield ? "translate-x-4" : "translate-x-0"
            }`}
          />
        </button>
      </div>
    </div>
  );
};
