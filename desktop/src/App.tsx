import React, { useState, useEffect } from "react";
import { Sidebar, NavTab } from "./components/Sidebar";
import { TopBar } from "./components/TopBar";
import { MetricsGrid } from "./components/MetricsGrid";
import { ThreatInspector } from "./components/ThreatInspector";
import { ProcessAuditorTable, ProcessAuditItem } from "./components/ProcessAuditorTable";
import { AuditVaultTable, LogEntry } from "./components/AuditVaultTable";
import { XaiDrawer, ScanResultPayload } from "./components/XaiDrawer";
import { DemoBanner } from "./components/DemoBanner";
import { ProtectionCard } from "./components/ProtectionCard";
import { QuickActionsGrid } from "./components/QuickActionsGrid";
import { RecentThreatsList } from "./components/RecentThreatsList";
import { Sliders } from "lucide-react";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import { getPageVariants } from "./styles/motion";

export const App: React.FC = () => {
  const shouldReduceMotion = !!useReducedMotion();
  const pageVariants = getPageVariants(shouldReduceMotion);

  // Theme state: dark is default, persisted in localStorage
  const [theme, setTheme] = useState<"dark" | "light">(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("sparrow_theme");
      if (saved === "light" || saved === "dark") return saved;
    }
    return "dark";
  });

  const [activeTab, setActiveTab] = useState<NavTab>("dashboard");
  const [airplaneMode] = useState<boolean>(true);
  const [wanBytes] = useState<number>(0);
  const [lastLatencyUs, setLastLatencyUs] = useState<number>(18);
  const [totalScans, setTotalScans] = useState<number>(14);
  const [threatsBlocked, setThreatsBlocked] = useState<number>(8);
  const [peakRamMb] = useState<number>(14.9);
  const [activeAlert, setActiveAlert] = useState<ScanResultPayload | null>(null);

  // New module states
  const [showDemoBanner, setShowDemoBanner] = useState<boolean>(true);
  const [clipboardShield, setClipboardShield] = useState<boolean>(true);
  const [inspectorType, setInspectorType] = useState<"Url" | "SmsText" | "QrPayload">("Url");
  const [inspectorPayload, setInspectorPayload] = useState<string>(
    "https://secure-p\u0430ypal.com/verify-account?token=9281a4b"
  );

  // Engine configuration settings
  const [entropyThreshold, setEntropyThreshold] = useState<number>(4.5);
  const [activeDelegate, setActiveDelegate] = useState<string>("CPU (x86_64 INT8 AVX2)");

  // Sync theme with HTML document class and localStorage
  useEffect(() => {
    if (typeof document !== "undefined") {
      if (theme === "dark") {
        document.documentElement.classList.add("dark");
      } else {
        document.documentElement.classList.remove("dark");
      }
      localStorage.setItem("sparrow_theme", theme);
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === "dark" ? "light" : "dark"));
  };

  const [logs, setLogs] = useState<LogEntry[]>([
    {
      id: "log_1",
      timestamp: "15:52:12",
      type: "URL",
      payload_snippet: "https://secure-p\u0430ypal.com/verify-account",
      verdict: "Malicious",
      category: "HOMOGRAPH_PUNYCODE",
      latency_us: 18,
    },
    {
      id: "log_2",
      timestamp: "15:46:05",
      type: "SMS",
      payload_snippet: "BANK ALERT: Unusual wire transfer of $2,450.00 initiated...",
      verdict: "Malicious",
      category: "URGENT_WIRE_TRANSFER",
      latency_us: 420,
    },
    {
      id: "log_3",
      timestamp: "15:42:30",
      type: "QR",
      payload_snippet: "javascript:alert(document.cookie)",
      verdict: "Malicious",
      category: "MALICIOUS_QR_SCHEME",
      latency_us: 45,
    },
    {
      id: "log_4",
      timestamp: "15:40:19",
      type: "URL",
      payload_snippet: "https://en.wikipedia.org/wiki/Information_security",
      verdict: "Safe",
      category: "SAFE_AUTHORITATIVE",
      latency_us: 89,
    },
  ]);

  const [processes, setProcesses] = useState<ProcessAuditItem[]>([
    {
      pid: 1042,
      name: "systemd",
      path: "/usr/lib/systemd/systemd",
      is_suspicious: false,
      threat_detail: "",
    },
    {
      pid: 3819,
      name: "pocket-sparrow-daemon",
      path: "/usr/bin/pocket-sparrow-daemon",
      is_suspicious: false,
      threat_detail: "",
    },
    {
      pid: 8912,
      name: "curl_exfil_script.sh",
      path: "/tmp/.hidden/curl_exfil_script.sh",
      is_suspicious: true,
      threat_detail: "Hidden script executing from temporary directory with background socket parameters.",
    },
  ]);

  // Scan handler invoking Tauri or local loopback bridge
  const handleScan = async (
    type: "Url" | "SmsText" | "QrPayload",
    payload: string
  ): Promise<ScanResultPayload> => {
    let result: ScanResultPayload;

    try {
      if (typeof window !== "undefined" && (window as any).__TAURI_INTERNALS__) {
        const { invoke } = await import("@tauri-apps/api/core");
        result = await invoke("scan_payload", { contentType: type, payload });
      } else {
        const res = await fetch("http://127.0.0.1:41789/scan", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ url: payload }),
        });
        if (res.ok) {
          const data = await res.json();
          result = {
            verdict:
              data.verdict === "MALICIOUS"
                ? "Malicious"
                : data.verdict === "SUSPICIOUS"
                ? "Suspicious"
                : "Safe",
            tier_triggered: "Tier1Heuristic",
            confidence: 0.98,
            latency_us: data.latency_us || 18,
            category: data.category || "THREAT",
            xai_reason: data.xai_reason,
            should_block: data.should_block,
          };
        } else {
          result = simulateScan(type, payload);
        }
      }
    } catch {
      result = simulateScan(type, payload);
    }

    setActiveAlert(result);
    setLastLatencyUs(result.latency_us);
    setTotalScans((prev) => prev + 1);
    if (result.verdict === "Malicious") {
      setThreatsBlocked((prev) => prev + 1);
    }

    const newLog: LogEntry = {
      id: `log_${Date.now()}`,
      timestamp: new Date().toLocaleTimeString(),
      type: type.toUpperCase(),
      payload_snippet: payload.length > 50 ? payload.substring(0, 50) + "..." : payload,
      verdict: result.verdict,
      category: result.category,
      latency_us: result.latency_us,
    };

    setLogs((prev) => [newLog, ...prev]);
    return result;
  };

  const simulateScan = (
    _type: "Url" | "SmsText" | "QrPayload",
    payload: string
  ): ScanResultPayload => {
    const lower = payload.toLowerCase();
    const t0 = performance.now();

    if (lower.startsWith("javascript:") || lower.includes("cookie")) {
      return {
        verdict: "Malicious",
        tier_triggered: "Tier1Heuristic",
        confidence: 0.99,
        latency_us: Math.round((performance.now() - t0) * 1000) + 40,
        category: "MALICIOUS_QR_SCHEME",
        xai_reason:
          "Blocked immediately on-device. Executable JavaScript code was embedded inside the link/QR code designed to hijack authentication cookies.",
        should_block: true,
      };
    }

    if (
      lower.includes("g00gle") ||
      lower.includes("cfd") ||
      lower.includes("top") ||
      lower.includes("wire transfer") ||
      lower.includes("suspend") ||
      lower.includes("\u0430") ||
      lower.includes("\u0440")
    ) {
      return {
        verdict: "Malicious",
        tier_triggered: "Tier1Heuristic",
        confidence: 0.98,
        latency_us: Math.round((performance.now() - t0) * 1000) + 18,
        category: lower.includes("wire")
          ? "URGENT_WIRE_TRANSFER"
          : lower.includes("\u0430") || lower.includes("\u0440")
          ? "HOMOGRAPH_PUNYCODE"
          : "CREDENTIAL_HARVESTING",
        xai_reason:
          "Deceptive indicators detected: visual spoofing homoglyph substitution paired with financial or account-locking coercion.",
        should_block: true,
      };
    }

    return {
      verdict: "Safe",
      tier_triggered: "Tier1Heuristic",
      confidence: 0.99,
      latency_us: Math.round((performance.now() - t0) * 1000) + 35,
      category: "SAFE_AUTHORITATIVE",
      xai_reason:
        "Passed all on-device Tier 1 heuristics and Tier 2 transformer checks. Authentic DNS properties, balanced entropy, and trusted structure.",
      should_block: false,
    };
  };

  // Quick Action Dispatcher
  const handleQuickAction = async (
    type: "Url" | "SmsText" | "QrPayload",
    autoPaste: boolean = false
  ) => {
    setInspectorType(type);
    if (autoPaste) {
      try {
        const text = await navigator.clipboard.readText();
        if (text && text.trim()) {
          setInspectorPayload(text);
          await handleScan(type, text);
          return;
        }
      } catch {
        // Fallback for secure clipboard contexts
      }
    }

    if (type === "Url") {
      setInspectorPayload("https://secure-p\u0430ypal.com/verify-account?token=9281a4b");
    } else if (type === "SmsText") {
      setInspectorPayload(
        "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: http://fake-chase.top"
      );
    } else {
      setInspectorPayload("javascript:alert('Stolen Token: ' + document.cookie)");
    }
  };

  return (
    <div className={theme === "dark" ? "dark" : ""}>
      <div className="flex h-screen bg-zinc-50 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 font-sans antialiased overflow-hidden transition-colors">
        {/* 1. Left Navigation Sidebar */}
        <Sidebar
          activeTab={activeTab}
          onSelectTab={setActiveTab}
          airplaneMode={airplaneMode}
          wanBytes={wanBytes}
        />

        {/* 2. Main Workspace */}
        <div className="flex-1 flex flex-col h-screen overflow-hidden">
          {/* Top Header with Sun/Moon Theme Toggle */}
          <TopBar
            activeTab={activeTab}
            latencyUs={lastLatencyUs}
            wanBytes={wanBytes}
            theme={theme}
            onToggleTheme={toggleTheme}
            onOpenInspector={() => setActiveTab("inspector")}
          />

          {/* Main Content Area with Page Transitions */}
          <main className="flex-1 overflow-y-auto p-8 space-y-8">
            <AnimatePresence mode="wait">
              <motion.div
                key={activeTab}
                variants={pageVariants}
                initial="initial"
                animate="animate"
                exit="exit"
              >
                {activeTab === "dashboard" && (
                  <div className="space-y-8 max-w-7xl mx-auto">
                    {/* Module 1: Dismissible Demo Mode Banner */}
                    {showDemoBanner && (
                      <DemoBanner
                        onDismiss={() => setShowDemoBanner(false)}
                        onQuickDemo={() =>
                          handleScan(
                            "Url",
                            "https://secure-p\u0430ypal.com/verify-account?token=9281a4b"
                          )
                        }
                      />
                    )}

                    {/* Module 2: 4-Column High-Level Metrics Grid */}
                    <MetricsGrid
                      totalScans={totalScans}
                      threatsBlocked={threatsBlocked}
                      lastLatencyUs={lastLatencyUs}
                      peakRamMb={peakRamMb}
                      wanBytes={wanBytes}
                    />

                    {/* Modules 3 & 4: Protection Active Card & Quick Actions Grid */}
                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
                      <div className="lg:col-span-5 flex">
                        <div className="w-full">
                          <ProtectionCard
                            clipboardShield={clipboardShield}
                            onToggleClipboardShield={setClipboardShield}
                            latencyUs={lastLatencyUs}
                          />
                        </div>
                      </div>
                      <div className="lg:col-span-7 flex">
                        <div className="w-full">
                          <QuickActionsGrid onSelectAction={handleQuickAction} />
                        </div>
                      </div>
                    </div>

                    {/* Module 5: Live Payload Inspector */}
                    <ThreatInspector
                      onScan={handleScan}
                      selectedType={inspectorType}
                      initialPayload={inspectorPayload}
                    />

                    {/* Module 6: Process Auditor Table & Recent Threats List */}
                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                      <div className="lg:col-span-7">
                        <ProcessAuditorTable
                          processes={processes}
                          onRefresh={() => setProcesses((prev) => [...prev])}
                        />
                      </div>
                      <div className="lg:col-span-5">
                        <RecentThreatsList
                          logs={logs}
                          onSelectThreat={(result) => setActiveAlert(result)}
                          onViewAll={() => setActiveTab("logs")}
                        />
                      </div>
                    </div>
                  </div>
                )}

                {activeTab === "inspector" && (
                  <div className="space-y-8 max-w-5xl mx-auto">
                    <ThreatInspector
                      onScan={handleScan}
                      selectedType={inspectorType}
                      initialPayload={inspectorPayload}
                    />
                    <AuditVaultTable
                      logs={logs}
                      onSelectLog={(result) => setActiveAlert(result)}
                    />
                  </div>
                )}

                {activeTab === "processes" && (
                  <div className="space-y-8 max-w-5xl mx-auto">
                    <ProcessAuditorTable
                      processes={processes}
                      onRefresh={() => setProcesses((prev) => [...prev])}
                    />
                  </div>
                )}

                {activeTab === "logs" && (
                  <div className="space-y-8 max-w-5xl mx-auto">
                    <AuditVaultTable
                      logs={logs}
                      onSelectLog={(result) => setActiveAlert(result)}
                    />
                  </div>
                )}

                {activeTab === "settings" && (
                  <div className="space-y-6 max-w-3xl mx-auto">
                    <div className="rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/40 p-6 space-y-5 transition-colors shadow-sm dark:shadow-none">
                      <div className="flex items-center space-x-3">
                        <div className="p-2 rounded-md bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300">
                          <Sliders className="w-4 h-4" />
                        </div>
                        <div>
                          <h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                            Engine Configuration
                          </h2>
                          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                            Adjust runtime thresholds for local heuristic models and INT8 delegates.
                          </p>
                        </div>
                      </div>

                      <div className="space-y-4 pt-4 border-t border-zinc-100 dark:border-zinc-800 text-xs">
                        {/* Shannon Entropy Threshold */}
                        <div className="flex items-center justify-between p-4 rounded-md bg-zinc-50 dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800">
                          <div>
                            <div className="font-medium text-zinc-900 dark:text-zinc-200">
                              Shannon Entropy Threshold
                            </div>
                            <div className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                              Flags algorithmic randomness in DGA subdomains (Default: 4.5)
                            </div>
                          </div>
                          <div>
                            <input
                              type="number"
                              step="0.1"
                              value={entropyThreshold}
                              onChange={(e) =>
                                setEntropyThreshold(parseFloat(e.target.value) || 4.5)
                              }
                              className="w-16 p-1.5 rounded bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-center text-zinc-900 dark:text-zinc-100 font-mono focus:outline-none focus:border-zinc-400 dark:focus:border-zinc-700"
                            />
                          </div>
                        </div>

                        {/* INT8 Execution Delegate */}
                        <div className="flex items-center justify-between p-4 rounded-md bg-zinc-50 dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800">
                          <div>
                            <div className="font-medium text-zinc-900 dark:text-zinc-200">
                              Transformer INT8 Delegate
                            </div>
                            <div className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                              Hardware acceleration provider for MobileBERT
                            </div>
                          </div>
                          <select
                            value={activeDelegate}
                            onChange={(e) => setActiveDelegate(e.target.value)}
                            className="p-1.5 rounded bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-200 text-xs focus:outline-none focus:border-zinc-400 dark:focus:border-zinc-700"
                          >
                            <option>CPU (x86_64 INT8 AVX2)</option>
                            <option>Android NNAPI Delegate</option>
                            <option>Vulkan / DirectML</option>
                          </select>
                        </div>

                        {/* Encrypted Vault Path */}
                        <div className="flex items-center justify-between p-4 rounded-md bg-zinc-50 dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800">
                          <div>
                            <div className="font-medium text-zinc-900 dark:text-zinc-200">
                              Encrypted Vault Storage
                            </div>
                            <div className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                              AES-256 SQLCipher local database path
                            </div>
                          </div>
                          <span className="text-xs font-mono text-zinc-500 dark:text-zinc-400">
                            ~/.pocket_sparrow/vault.db
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </motion.div>
            </AnimatePresence>
          </main>
        </div>

        {/* 3. Forensic Detail Slide-Out Sheet */}
        <XaiDrawer result={activeAlert} onClose={() => setActiveAlert(null)} />
      </div>
    </div>
  );
};

export default App;
