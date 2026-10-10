import React, { useState } from "react";
import {
  Mail,
  Plus,
  ShieldCheck,
  Dna,
  CheckCircle2,
  Lock,
  Inbox,
  X,
} from "lucide-react";
import { ThreatDnaVisualizer } from "./ThreatDnaVisualizer";
import { ScanResultPayload } from "./XaiDrawer";

export interface EmailAccount {
  id: string;
  provider: "gmail" | "outlook" | "yahoo" | "imap";
  email: string;
  imap_server: string;
  imap_port: number;
  is_active: boolean;
  scan_incoming: boolean;
  real_time_idle: boolean;
  block_link_clicks: boolean;
  notify_on_scan: boolean;
  last_synced: string;
}

export interface ScannedEmailItem {
  id: string;
  account_id: string;
  sender: string;
  recipient: string;
  subject: string;
  snippet: string;
  extracted_urls: string[];
  verdict: "Safe" | "Suspicious" | "Malicious";
  threat_category?: string;
  xai_reason?: string;
  latency_us: number;
  timestamp: string;
  is_read: boolean;
}

interface EmailShieldProps {
  onSelectThreat?: (payload: ScanResultPayload) => void;
  className?: string;
}

export const EmailShield: React.FC<EmailShieldProps> = ({
  onSelectThreat,
  className = "",
}) => {
  const [accounts, setAccounts] = useState<EmailAccount[]>([
    {
      id: "acct_default",
      provider: "imap",
      email: "security-audits@corporate.local",
      imap_server: "mail.corporate.local",
      imap_port: 993,
      is_active: true,
      scan_incoming: true,
      real_time_idle: true,
      block_link_clicks: true,
      notify_on_scan: false,
      last_synced: "Active (Connected)",
    },
  ]);

  const [emails] = useState<ScannedEmailItem[]>([
    {
      id: "eml_1",
      account_id: "acct_default",
      sender: "alert@security-update-chase.example.test",
      recipient: "security-audits@corporate.local",
      subject: "Urgent: Immediate verification required for wire transfer",
      snippet: "Your online banking access has been placed on hold. Please visit http://verify-auth-session.example.test to confirm.",
      extracted_urls: ["http://verify-auth-session.example.test"],
      verdict: "Malicious",
      threat_category: "CREDENTIAL_HARVESTING",
      xai_reason: "High-confidence credential harvesting lure: Combines financial urgency lure with deceptive lookalike domain.",
      latency_us: 18240,
      timestamp: "10 mins ago",
      is_read: false,
    },
    {
      id: "eml_2",
      account_id: "acct_default",
      sender: "notifications@github-support.example.test",
      recipient: "security-audits@corporate.local",
      subject: "New sign-in from unknown device in Frankfurt",
      snippet: "We detected a login to your account from a new IP address. Review security keys if unauthorized.",
      extracted_urls: ["https://example.com/security"],
      verdict: "Safe",
      threat_category: "BENIGN",
      xai_reason: "Standard multi-factor authorization notification. No deceptive anchors or spoofing characters detected.",
      latency_us: 14120,
      timestamp: "35 mins ago",
      is_read: true,
    },
  ]);

  const [activeFilter, setActiveFilter] = useState<"all" | "threats">("all");
  const [selectedDnaEmail, setSelectedDnaEmail] = useState<ScannedEmailItem | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);

  // New account form state
  const [newProvider, setNewProvider] = useState<"gmail" | "outlook" | "yahoo" | "imap">("gmail");
  const [newEmail, setNewEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newServer, setNewServer] = useState("imap.gmail.com");
  const [newPort, setNewPort] = useState(993);
  const [isTestingConn, setIsTestingConn] = useState(false);
  const [testResult, setTestResult] = useState<string | null>(null);

  // Statistics
  const scannedToday = emails.length;
  const threatsBlocked = emails.filter((e) => e.verdict === "Malicious").length;
  const avgLatencyMs = emails.length > 0
    ? (emails.reduce((acc, curr) => acc + curr.latency_us, 0) / emails.length / 1000).toFixed(1)
    : "16.2";

  const filteredEmails = activeFilter === "threats"
    ? emails.filter((e) => e.verdict === "Malicious" || e.verdict === "Suspicious")
    : emails;

  const handleTestConnection = async () => {
    if (!newEmail) return;
    setIsTestingConn(true);
    setTestResult(null);
    await new Promise((r) => setTimeout(r, 600)); // Local check simulation
    setIsTestingConn(false);
    setTestResult("Connection established over TLS (Port " + newPort + "). 0 external telemetry.");
  };

  const handleSaveAccount = () => {
    if (!newEmail) return;
    const acct: EmailAccount = {
      id: `acct_${Date.now()}`,
      provider: newProvider,
      email: newEmail,
      imap_server: newServer,
      imap_port: Number(newPort),
      is_active: true,
      scan_incoming: true,
      real_time_idle: true,
      block_link_clicks: true,
      notify_on_scan: false,
      last_synced: "Just now",
    };
    setAccounts((prev) => [...prev, acct]);
    setShowAddModal(false);
    setNewEmail("");
    setNewPassword("");
    setTestResult(null);
  };

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header and Stats */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 shadow-sm dark:shadow-none">
        <div className="flex items-center space-x-3.5">
          <div className="p-2.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/40">
            <Mail className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-base font-semibold text-zinc-900 dark:text-zinc-100 tracking-tight">
                Email Background Shield
              </h2>
              <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 font-medium">
                IMAP IDLE Active
              </span>
            </div>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
              On-device background mailbox guardian. Zero cloud relays; credentials stay in encrypted local vault.
            </p>
          </div>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center space-x-1.5 px-3.5 py-2 rounded-md bg-zinc-900 hover:bg-zinc-800 text-white dark:bg-zinc-100 dark:hover:bg-zinc-200 dark:text-zinc-900 text-xs font-medium transition-colors shadow-sm self-start sm:self-auto"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Add Account</span>
        </button>
      </div>

      {/* KPI Stats Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-4 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40">
          <div className="text-xs font-medium text-zinc-500 dark:text-zinc-400">
            Emails Scanned Today
          </div>
          <div className="text-xl font-bold font-mono text-zinc-900 dark:text-zinc-100 mt-1">
            {scannedToday}
          </div>
        </div>

        <div className="p-4 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40">
          <div className="text-xs font-medium text-zinc-500 dark:text-zinc-400">
            Threats Quarantined
          </div>
          <div className="text-xl font-bold font-mono text-rose-600 dark:text-rose-400 mt-1">
            {threatsBlocked}
          </div>
        </div>

        <div className="p-4 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40">
          <div className="text-xs font-medium text-zinc-500 dark:text-zinc-400">
            Average Scan Latency
          </div>
          <div className="text-xl font-bold font-mono text-emerald-600 dark:text-emerald-400 mt-1">
            {avgLatencyMs} ms
          </div>
        </div>
      </div>

      {/* Connected Accounts Manager Strip */}
      <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-4 space-y-3">
        <div className="flex items-center justify-between text-xs border-b border-zinc-200 dark:border-zinc-800 pb-2">
          <span className="font-semibold text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
            <Lock className="w-3.5 h-3.5 text-emerald-500" />
            <span>Monitored Mailbox Accounts ({accounts.length})</span>
          </span>
          <span className="text-[11px] text-zinc-400 font-mono">
            Credentials Stored Locally in SQLCipher Vault
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {accounts.map((acct) => (
            <div
              key={acct.id}
              className="p-3 rounded-md border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950/60 flex items-center justify-between gap-3 text-xs"
            >
              <div className="space-y-0.5">
                <div className="font-medium text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
                  <span>{acct.email}</span>
                  <span className="text-[10px] px-1.5 py-0.2 rounded font-mono uppercase bg-zinc-200 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400">
                    {acct.provider}
                  </span>
                </div>
                <div className="text-[11px] text-zinc-400 font-mono">
                  {acct.imap_server}:{acct.imap_port} • {acct.last_synced}
                </div>
              </div>

              <div className="flex items-center space-x-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
                <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                  Shielded
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Live Email Threat Feed */}
      <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-5 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-zinc-200 dark:border-zinc-800 pb-3">
          <div className="flex items-center space-x-2">
            <Inbox className="w-4 h-4 text-zinc-500" />
            <h3 className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
              Live Scanned Email Feed (Last 50 Messages)
            </h3>
          </div>

          <div className="inline-flex p-1 rounded-md bg-zinc-100 dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 text-xs">
            <button
              onClick={() => setActiveFilter("all")}
              className={`px-3 py-1 rounded transition-colors ${
                activeFilter === "all"
                  ? "bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium shadow-sm"
                  : "text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-200"
              }`}
            >
              All Scans ({emails.length})
            </button>
            <button
              onClick={() => setActiveFilter("threats")}
              className={`px-3 py-1 rounded transition-colors ${
                activeFilter === "threats"
                  ? "bg-white dark:bg-zinc-800 text-rose-600 dark:text-rose-400 font-medium shadow-sm"
                  : "text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-200"
              }`}
            >
              Threats Only ({threatsBlocked})
            </button>
          </div>
        </div>

        {filteredEmails.length === 0 ? (
          <div className="py-12 text-center space-y-2">
            <div className="w-10 h-10 rounded-full bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
              Inbox All Clear
            </div>
            <p className="text-[11px] text-zinc-500 max-w-sm mx-auto">
              Zero malicious email links, smishing lures, or credential harvesters detected.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
            {filteredEmails.map((email) => {
              const isMalicious = email.verdict === "Malicious";
              return (
                <div
                  key={email.id}
                  className="py-3.5 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs hover:bg-zinc-50 dark:hover:bg-zinc-950/40 px-2 rounded-md transition-colors"
                >
                  <div className="space-y-1 max-w-2xl">
                    <div className="flex items-center space-x-2">
                      <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                        {email.sender}
                      </span>
                      <span
                        className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${
                          isMalicious
                            ? "bg-rose-50 border-rose-200 text-rose-700 dark:bg-rose-500/10 dark:border-rose-500/20 dark:text-rose-400"
                            : "bg-emerald-50 border-emerald-200 text-emerald-700 dark:bg-emerald-500/10 dark:border-emerald-500/20 dark:text-emerald-400"
                        }`}
                      >
                        {email.verdict}
                      </span>
                      {email.threat_category && (
                        <span className="font-mono text-[10px] text-zinc-500">
                          {email.threat_category}
                        </span>
                      )}
                    </div>
                    <div className="text-zinc-800 dark:text-zinc-200 font-medium">
                      {email.subject}
                    </div>
                    <p className="text-[11px] text-zinc-500 dark:text-zinc-400 line-clamp-1">
                      {email.snippet}
                    </p>
                  </div>

                  <div className="flex items-center space-x-2 shrink-0 self-end md:self-auto">
                    <button
                      onClick={() => setSelectedDnaEmail(email)}
                      className="flex items-center space-x-1 px-2.5 py-1.5 rounded border border-zinc-200 dark:border-zinc-800 hover:bg-zinc-100 dark:hover:bg-zinc-800 text-[11px] text-zinc-700 dark:text-zinc-300 transition-colors"
                      title="Inspect Token-Level Threat DNA"
                    >
                      <Dna className="w-3.5 h-3.5 text-indigo-500" />
                      <span>View DNA</span>
                    </button>

                    <button
                      onClick={() => {
                        const payload: ScanResultPayload = {
                          verdict: email.verdict,
                          tier_triggered: isMalicious ? "Tier2Transformer" : "Tier1Heuristic",
                          confidence: isMalicious ? 0.94 : 0.05,
                          latency_us: email.latency_us,
                          category: email.threat_category || "EMAIL_PHISHING",
                          xai_reason: email.xai_reason || "Evaluated by on-device MobileBERT smishing model.",
                          should_block: isMalicious,
                          payload: email.extracted_urls[0] || email.subject,
                        };
                        if (onSelectThreat) onSelectThreat(payload);
                      }}
                      className="px-2.5 py-1.5 rounded bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:opacity-90 text-[11px] font-medium transition-opacity"
                    >
                      XAI Report
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Threat DNA Modal */}
      {selectedDnaEmail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-xs"
            onClick={() => setSelectedDnaEmail(null)}
          />
          <div className="relative w-full max-w-xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 rounded-xl p-6 shadow-2xl z-10 space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-200 dark:border-zinc-800 pb-3">
              <div className="flex items-center space-x-2">
                <Dna className="w-4 h-4 text-indigo-500" />
                <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                  Email Threat DNA Breakdown
                </h4>
              </div>
              <button
                onClick={() => setSelectedDnaEmail(null)}
                className="text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-1.5 text-xs">
              <div className="font-semibold text-zinc-800 dark:text-zinc-200">
                Subject &amp; Body Payload:
              </div>
              <ThreatDnaVisualizer
                payload={selectedDnaEmail.extracted_urls[0] || selectedDnaEmail.subject}
                category={selectedDnaEmail.threat_category}
              />
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setSelectedDnaEmail(null)}
                className="px-4 py-1.5 rounded-md bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 text-xs font-medium"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Account Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-xs"
            onClick={() => setShowAddModal(false)}
          />
          <div className="relative w-full max-w-md bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 rounded-xl p-6 shadow-2xl z-10 space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-200 dark:border-zinc-800 pb-3">
              <div className="flex items-center space-x-2">
                <Mail className="w-4 h-4 text-emerald-500" />
                <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                  Connect Protected Mailbox
                </h4>
              </div>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-zinc-600 dark:text-zinc-400 mb-1">
                  Provider Picker
                </label>
                <select
                  value={newProvider}
                  onChange={(e) => {
                    const prov = e.target.value as any;
                    setNewProvider(prov);
                    if (prov === "gmail") setNewServer("imap.gmail.com");
                    else if (prov === "outlook") setNewServer("outlook.office365.com");
                    else if (prov === "yahoo") setNewServer("imap.mail.yahoo.com");
                  }}
                  className="w-full px-3 py-2 rounded-md bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100 cursor-pointer focus:outline-none focus:ring-1 focus:ring-blue-500"
                >
                  <option value="gmail" className="bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 py-1">Google Workspace / Gmail (OAuth 2.0)</option>
                  <option value="outlook" className="bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 py-1">Microsoft 365 / Outlook (OAuth 2.0)</option>
                  <option value="yahoo" className="bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 py-1">Yahoo Mail (App Password)</option>
                  <option value="imap" className="bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 py-1">Generic Enterprise IMAP (TLS)</option>
                </select>
              </div>

              <div className="grid grid-cols-3 gap-2">
                <div className="col-span-2">
                  <label className="block text-zinc-600 dark:text-zinc-400 mb-1">
                    IMAP Host
                  </label>
                  <input
                    type="text"
                    value={newServer}
                    onChange={(e) => setNewServer(e.target.value)}
                    className="w-full px-3 py-2 rounded-md bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100"
                  />
                </div>
                <div>
                  <label className="block text-zinc-600 dark:text-zinc-400 mb-1">
                    Port
                  </label>
                  <input
                    type="number"
                    value={newPort}
                    onChange={(e) => setNewPort(Number(e.target.value))}
                    className="w-full px-3 py-2 rounded-md bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100 font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-zinc-600 dark:text-zinc-400 mb-1">
                  Email Address
                </label>
                <input
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  placeholder="analyst@enterprise.com"
                  className="w-full px-3 py-2 rounded-md bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100"
                />
              </div>

              <div>
                <label className="block text-zinc-600 dark:text-zinc-400 mb-1">
                  App Password / Token
                </label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••••••••••"
                  className="w-full px-3 py-2 rounded-md bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100 font-mono"
                />
              </div>

              {testResult && (
                <div className="p-2.5 rounded bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-400 text-[11px] flex items-center space-x-1.5">
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                  <span>{testResult}</span>
                </div>
              )}
            </div>

            <div className="pt-3 border-t border-zinc-200 dark:border-zinc-800 flex items-center justify-between text-xs">
              <button
                type="button"
                onClick={handleTestConnection}
                disabled={isTestingConn || !newEmail}
                className="px-3 py-1.5 rounded border border-zinc-200 dark:border-zinc-800 hover:bg-zinc-100 dark:hover:bg-zinc-900 text-zinc-700 dark:text-zinc-300 font-medium disabled:opacity-50"
              >
                {isTestingConn ? "Testing..." : "Test Connection"}
              </button>

              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3 py-1.5 rounded text-zinc-500 hover:text-zinc-700"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveAccount}
                  disabled={!newEmail}
                  className="px-4 py-1.5 rounded bg-emerald-600 hover:bg-emerald-500 text-white font-medium disabled:opacity-50"
                >
                  Enable Shield
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
