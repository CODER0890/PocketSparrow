import React from "react";
import { Link2, MessageSquare, QrCode, Clipboard } from "lucide-react";

interface QuickActionsGridProps {
  onSelectAction: (actionType: "Url" | "SmsText" | "QrPayload", autoPaste?: boolean) => void;
}

export const QuickActionsGrid: React.FC<QuickActionsGridProps> = ({ onSelectAction }) => {
  const actions = [
    {
      id: "scan_url",
      label: "Scan URL",
      description: "Homoglyph & DGA Entropy",
      icon: Link2,
      type: "Url" as const,
      autoPaste: false,
    },
    {
      id: "scan_sms",
      label: "Scan SMS",
      description: "Banking Phishing & KYC",
      icon: MessageSquare,
      type: "SmsText" as const,
      autoPaste: false,
    },
    {
      id: "scan_qr",
      label: "Scan QR",
      description: "Quishing & Exec Scheme",
      icon: QrCode,
      type: "QrPayload" as const,
      autoPaste: false,
    },
    {
      id: "paste_check",
      label: "Paste & Check",
      description: "Auto-evaluate clipboard",
      icon: Clipboard,
      type: "Url" as const,
      autoPaste: true,
    },
  ];

  return (
    <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-5 transition-colors shadow-sm dark:shadow-none">
      <div className="flex items-center justify-between pb-3.5 border-b border-zinc-100 dark:border-zinc-800">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
          Quick Actions
        </h3>
        <span className="text-[11px] text-zinc-400 dark:text-zinc-500 font-mono">
          Instant Vectors
        </span>
      </div>

      <div className="grid grid-cols-2 gap-3 mt-3.5">
        {actions.map((act) => {
          const Icon = act.icon;
          return (
            <button
              key={act.id}
              onClick={() => onSelectAction(act.type, act.autoPaste)}
              className="flex items-start space-x-3 p-3 rounded-md border border-zinc-200 dark:border-zinc-800 bg-zinc-50/60 dark:bg-zinc-950/60 hover:bg-zinc-100 dark:hover:bg-zinc-800/60 hover:border-zinc-300 dark:hover:border-zinc-700 transition-all text-left group"
            >
              <div className="p-2 rounded bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 group-hover:text-zinc-900 dark:group-hover:text-zinc-100 transition-colors">
                <Icon className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <div className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                  {act.label}
                </div>
                <div className="text-[11px] text-zinc-500 dark:text-zinc-400 truncate mt-0.5">
                  {act.description}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
};
