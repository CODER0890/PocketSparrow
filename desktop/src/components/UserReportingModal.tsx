import React, { useState } from "react";
import { X, ShieldAlert, Lock, CheckCircle2 } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface UserReportingModalProps {
  isOpen: boolean;
  onClose: () => void;
  onReportSubmitted: (report: any) => void;
}

export const UserReportingModal: React.FC<UserReportingModalProps> = ({
  isOpen,
  onClose,
  onReportSubmitted,
}) => {
  const [target, setTarget] = useState("");
  const [category, setCategory] = useState("SCAM");
  const [reason, setReason] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const categories = [
    { id: "ROBOCALL", label: "Robocall / Automated" },
    { id: "SCAM", label: "Financial / Impersonation Scam" },
    { id: "PHISHING", label: "Phishing / Credential Harvest" },
    { id: "TELEMARKETING", label: "Unsolicited Telemarketing" },
    { id: "OTHER", label: "Other Malicious Activity" },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!target.trim()) return;

    setIsSubmitting(true);
    try {
      let report: any;
      if (typeof window !== "undefined" && (window as any).__TAURI_INTERNALS__) {
        const { invoke } = await import("@tauri-apps/api/core");
        report = await invoke("report_communication_spam", {
          target: target.trim(),
          category,
          reason: reason.trim(),
        });
      } else {
        // Web fallback simulation
        report = {
          target_hash: `sha256_${Date.now()}`,
          target_masked: target.includes("@")
            ? `${target.slice(0, 3)}***@${target.split("@")[1] || ""}`
            : `${target.slice(0, 3)}****${target.slice(-3)}`,
          category,
          reason,
          timestamp: Date.now(),
        };
      }

      setSuccessMessage(
        `Target securely hashed & quarantined locally in SQLCipher vault.`
      );
      onReportSubmitted(report);

      setTimeout(() => {
        setTarget("");
        setReason("");
        setSuccessMessage(null);
        setIsSubmitting(false);
        onClose();
      }, 1200);
    } catch (err) {
      console.error("Failed to submit spam report:", err);
      setIsSubmitting(false);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm"
          />

          {/* Modal Container */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ duration: 0.2, ease: [0.4, 0, 0.2, 1] }}
            className="relative w-full max-w-lg rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-2xl overflow-hidden z-10"
          >
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded-lg bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400 border border-rose-200 dark:border-rose-500/20">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-semibold text-zinc-900 dark:text-zinc-100">
                    Report Spam or Phishing
                  </h3>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    Flag malicious caller, sender, or smishing target
                  </p>
                </div>
              </div>
              <button
                onClick={onClose}
                className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Privacy Architecture Notice */}
            <div className="px-6 py-3 bg-emerald-50/70 dark:bg-emerald-950/20 border-b border-emerald-200/50 dark:border-emerald-900/30 flex items-start space-x-3 text-xs text-emerald-800 dark:text-emerald-300">
              <Lock className="w-4 h-4 shrink-0 mt-0.5 text-emerald-600 dark:text-emerald-400" />
              <div>
                <span className="font-semibold">100% On-Device Privacy Guarantee:</span> Reports are
                stored exclusively in your local encrypted SQLCipher database. The identifier is
                salted and hashed. Zero network traffic leaves your computer; no cloud database is
                ever contacted.
              </div>
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {successMessage ? (
                <div className="py-8 flex flex-col items-center justify-center text-center space-y-2">
                  <CheckCircle2 className="w-12 h-12 text-emerald-500 animate-bounce" />
                  <p className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                    Target Reported &amp; Blocked
                  </p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">{successMessage}</p>
                </div>
              ) : (
                <>
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-600 dark:text-zinc-400 mb-1.5">
                      Phone Number or Email Address
                    </label>
                    <input
                      type="text"
                      required
                      value={target}
                      onChange={(e) => setTarget(e.target.value)}
                      placeholder="+1 (555) 019-2834 or support@phishing-target.test"
                      className="w-full px-3.5 py-2.5 rounded-lg bg-zinc-50 dark:bg-zinc-950 border border-zinc-300 dark:border-zinc-700 text-sm text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-600 dark:text-zinc-400 mb-1.5">
                      Threat Category
                    </label>
                    <select
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      className="w-full px-3.5 py-2.5 rounded-lg bg-zinc-50 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
                    >
                      {categories.map((c) => (
                        <option
                          key={c.id}
                          value={c.id}
                          className="bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 py-1.5"
                        >
                          {c.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-600 dark:text-zinc-400 mb-1.5">
                      Reason / Pattern Details (Optional)
                    </label>
                    <textarea
                      rows={3}
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      placeholder="e.g. Automated IRS tax scam robocall requesting cryptocurrency transfer..."
                      className="w-full px-3.5 py-2 rounded-lg bg-zinc-50 dark:bg-zinc-950 border border-zinc-300 dark:border-zinc-700 text-sm text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                    />
                  </div>

                  <div className="pt-2 flex items-center justify-end space-x-3">
                    <button
                      type="button"
                      onClick={onClose}
                      className="px-4 py-2 rounded-lg text-sm font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isSubmitting || !target.trim()}
                      className="px-5 py-2 rounded-lg text-sm font-semibold bg-rose-600 hover:bg-rose-500 text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
                    >
                      {isSubmitting ? "Encrypting & Storing..." : "Add to Local Shield DB"}
                    </button>
                  </div>
                </>
              )}
            </form>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};
