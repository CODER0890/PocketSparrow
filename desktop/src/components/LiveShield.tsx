import React, { useState, useEffect } from "react";
import {
  Bell,
  Play,
  Pause,
  MemoryStick,
  Radio,
  Zap,
  Lock,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { MOTION_DURATION } from "../styles/motion";

interface InterceptedEvent {
  id: string;
  timestamp: string;
  appName: string;
  sender: string;
  snippet: string;
  threatLevel: "SAFE" | "SUSPICIOUS" | "MALICIOUS";
  category: string;
  xaiReason: string;
  latencyUs: number;
  actionTaken: "PASSED" | "BLOCKED" | "ALLOWED_ONCE";
  urls: string[];
}

export const LiveShield: React.FC = () => {
  const [isPaused, setIsPaused] = useState(false);
  const [pauseRemaining, setPauseRemaining] = useState(0);
  const [zeroRetention, setZeroRetention] = useState(false);
  const [sensitivity, setSensitivity] = useState(0.75);
  const [pulseThreat, setPulseThreat] = useState(false);

  const [monitoredApps, setMonitoredApps] = useState<{ [key: string]: boolean }>({
    Browser: true,
    Slack: true,
    Discord: true,
    Telegram: true,
    Email: true,
    Signal: true,
  });

  const [events, setEvents] = useState<InterceptedEvent[]>([
    {
      id: "evt-1",
      timestamp: "11:32:04",
      appName: "Slack",
      sender: "security-bot",
      snippet: "URGENT: Corporate credentials compromised. Re-authenticate at https://auth-verify.test within 10 min.",
      threatLevel: "MALICIOUS",
      category: "HOMOGRAPH_PHISHING",
      xaiReason: "High-entropy homoglyph domain with urgent coercion pattern detected on-device.",
      latencyUs: 1420,
      actionTaken: "BLOCKED",
      urls: ["https://auth-verify.test"],
    },
    {
      id: "evt-2",
      timestamp: "11:29:41",
      appName: "Email",
      sender: "newsletter@trusted.com",
      snippet: "Here is your weekly summary of tech news and release updates.",
      threatLevel: "SAFE",
      category: "SAFE",
      xaiReason: "Clean sender reputation, standard RFC headers, zero evasive script injection.",
      latencyUs: 890,
      actionTaken: "PASSED",
      urls: [],
    },
    {
      id: "evt-3",
      timestamp: "11:24:18",
      appName: "Discord",
      sender: "GameMod",
      snippet: "Special gift: Download game test patch immediately: http://patch-download.test/game_update.apk",
      threatLevel: "MALICIOUS",
      category: "MALICIOUS_DOWNLOAD",
      xaiReason: "Unsigned APK direct download link detected inside chat stream.",
      latencyUs: 1650,
      actionTaken: "BLOCKED",
      urls: ["http://patch-download.test/game_update.apk"],
    },
  ]);

  // Pause timer countdown
  useEffect(() => {
    let timer: ReturnType<typeof setInterval>;
    if (isPaused && pauseRemaining > 0) {
      timer = setInterval(() => {
        setPauseRemaining((prev) => {
          if (prev <= 1) {
            setIsPaused(false);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [isPaused, pauseRemaining]);

  const togglePause = () => {
    if (isPaused) {
      setIsPaused(false);
      setPauseRemaining(0);
    } else {
      setIsPaused(true);
      setPauseRemaining(15 * 60);
    }
  };

  const toggleApp = (appName: string) => {
    setMonitoredApps((prev) => ({
      ...prev,
      [appName]: !prev[appName],
    }));
  };

  const handleSimulate = (
    _name: string,
    app: string,
    sender: string,
    snippet: string,
    isMalicious: boolean
  ) => {
    const urls = snippet.match(/https?:\/\/[^\s]+/g) || [];
    const newEvent: InterceptedEvent = {
      id: `sim-${Date.now()}`,
      timestamp: new Date().toLocaleTimeString(),
      appName: app,
      sender: sender,
      snippet: snippet,
      threatLevel: isMalicious ? "MALICIOUS" : "SAFE",
      category: isMalicious ? "SIMULATED_PHISHING" : "SAFE",
      xaiReason: isMalicious
        ? "Classified via on-device heuristics & MobileBERT transformer (<50ms SLA). Deceptive RFC 2606 .test domain detected."
        : "Passed all heuristic validation checks. Low entropy and zero coercion triggers.",
      latencyUs: Math.floor(Math.random() * 800) + 700,
      actionTaken: isMalicious ? "BLOCKED" : "PASSED",
      urls: urls,
    };

    setEvents((prev) => [newEvent, ...prev.slice(0, 49)]);

    if (isMalicious) {
      setPulseThreat(true);
      setTimeout(() => setPulseThreat(false), 800);
    }
  };

  const formatRemaining = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* 1. Header Card */}
      <div className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/60 p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-4">
            <motion.div
              animate={pulseThreat ? { scale: [1, 1.25, 1] } : undefined}
              transition={{ duration: 0.4 }}
              className={`p-3 rounded-xl border flex items-center justify-center ${
                isPaused
                  ? "bg-amber-500/10 border-amber-500/30 text-amber-500"
                  : pulseThreat
                  ? "bg-rose-500/20 border-rose-500/50 text-rose-500"
                  : "bg-emerald-500/10 border-emerald-500/30 text-emerald-500"
              }`}
            >
              <Radio className="w-6 h-6 animate-pulse" />
            </motion.div>
            <div>
              <div className="flex items-center space-x-3">
                <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                  Live Shield
                </h2>
                <span
                  className={`text-xs px-2.5 py-0.5 rounded-full font-semibold border ${
                    isPaused
                      ? "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20"
                      : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20"
                  }`}
                >
                  {isPaused ? `PAUSED (${formatRemaining(pauseRemaining)})` : "ACTIVE (MONITORING)"}
                </span>
              </div>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                Zero-delay real-time notification & clipboard threat interceptor (<span className="font-mono text-emerald-600 dark:text-emerald-400 font-bold">&lt;2ms extraction, &lt;50ms verdict</span>)
              </p>
            </div>
          </div>

          {/* 0 Bytes WAN Indicator */}
          <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg border border-emerald-500/30 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400 text-xs font-mono font-semibold">
            <Lock className="w-3.5 h-3.5" />
            <span>0 BYTES WAN • AIR-GAPPED</span>
          </div>
        </div>

        {/* Quick Actions Row */}
        <div className="mt-6 pt-5 border-t border-zinc-200 dark:border-zinc-800 flex flex-wrap items-center gap-3">
          <button
            onClick={togglePause}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-xs font-medium transition-colors border ${
              isPaused
                ? "bg-amber-500 text-white border-amber-600"
                : "bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border-zinc-300 dark:border-zinc-700 hover:bg-zinc-200 dark:hover:bg-zinc-700"
            }`}
          >
            {isPaused ? <Play className="w-3.5 h-3.5" /> : <Pause className="w-3.5 h-3.5" />}
            <span>{isPaused ? "Resume Now" : "Pause for 15 min"}</span>
          </button>

          <button
            onClick={() => setZeroRetention(!zeroRetention)}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-xs font-medium transition-colors border ${
              zeroRetention
                ? "bg-emerald-600 text-white border-emerald-700"
                : "bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border-zinc-300 dark:border-zinc-700 hover:bg-zinc-200 dark:hover:bg-zinc-700"
            }`}
          >
            <MemoryStick className="w-3.5 h-3.5" />
            <span>Zero Retention Mode: {zeroRetention ? "ON (RAM ONLY)" : "OFF"}</span>
          </button>

          {zeroRetention && (
            <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium ml-1">
              • Bypasses SQLite Forensic Vault. Ephemeral in volatile memory only.
            </span>
          )}
        </div>
      </div>

      {/* 2. Controls Grid: Sensitivity & App Toggles */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Sensitivity */}
        <div className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/60 p-5 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
              Detection Sensitivity
            </span>
            <span className="text-xs font-mono font-bold text-sky-500">
              {sensitivity >= 0.9 ? "Aggressive (0.95)" : sensitivity >= 0.7 ? "High (0.75)" : "Standard (0.50)"}
            </span>
          </div>
          <input
            type="range"
            min="0.25"
            max="0.95"
            step="0.25"
            value={sensitivity}
            onChange={(e) => setSensitivity(parseFloat(e.target.value))}
            className="w-full accent-sky-500 cursor-pointer"
          />
          <div className="flex justify-between text-[10px] text-zinc-400 mt-2 font-mono">
            <span>Low (0.25)</span>
            <span>Balanced (0.50)</span>
            <span>High (0.75)</span>
            <span>Aggressive (0.95)</span>
          </div>
        </div>

        {/* Monitored Channels */}
        <div className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/60 p-5 shadow-sm">
          <div className="text-sm font-semibold text-zinc-900 dark:text-zinc-100 mb-3">
            Monitored Desktop Channels
          </div>
          <div className="flex flex-wrap gap-2">
            {Object.entries(monitoredApps).map(([app, enabled]) => (
              <button
                key={app}
                onClick={() => toggleApp(app)}
                className={`px-3 py-1 rounded-md text-xs font-medium border transition-colors flex items-center space-x-1.5 ${
                  enabled
                    ? "bg-sky-50 dark:bg-sky-500/10 border-sky-300 dark:border-sky-500/30 text-sky-600 dark:text-sky-400"
                    : "bg-zinc-100 dark:bg-zinc-800 border-zinc-200 dark:border-zinc-700 text-zinc-400 line-through"
                }`}
              >
                <span>{app}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 3. RFC 2606 Safe Simulation Harness */}
      <div className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/60 p-5 shadow-sm">
        <div className="flex items-center space-x-2 mb-2">
          <Zap className="w-4 h-4 text-amber-500" />
          <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
            Safe Threat Simulator (RFC 2606 Reserved Domains)
          </h3>
        </div>
        <p className="text-xs text-zinc-500 dark:text-zinc-400 mb-4">
          Test real-time quarantine pipeline with synthetic RFC 2606 domains (.test) and dummy telephone handles. Zero external requests made.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {[
            {
              name: "PayPal Spoof",
              app: "Browser",
              sender: "Security Alert",
              snippet: "Security alert: Unauthorized login from Moscow. Verify identity at https://security-paypal.test",
              malicious: true,
            },
            {
              name: "Chase Wire Urgent",
              app: "Signal",
              sender: "+1-555-0199",
              snippet: "URGENT: wire transfer of $4,850 pending. Account suspended. Confirm at https://chase-login.test",
              malicious: true,
            },
            {
              name: "Elon Musk BTC Giveaway",
              app: "Telegram",
              sender: "Tesla Giveaway",
              snippet: "Elon Musk giving away 50 BTC. Send 0.1 BTC to receive 1.0 BTC immediately at https://elon-giveaway.test",
              malicious: true,
            },
            {
              name: "Lunch Meeting",
              app: "Slack",
              sender: "Alice",
              snippet: "Hey! Are we still meeting for lunch at 12:30pm today at the bistro downtown?",
              malicious: false,
            },
            {
              name: "Package Delivered",
              app: "Email",
              sender: "Bookstore Logistics",
              snippet: "Your book order #84920 has been delivered to your front porch. Have a great day!",
              malicious: false,
            },
            {
              name: "Drive-By APK",
              app: "Discord",
              sender: "GameMod",
              snippet: "Critical update patch required: http://patch-download.test/system_update.apk",
              malicious: true,
            },
          ].map((tc, idx) => (
            <div
              key={idx}
              className="p-3 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-800/40 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                    {tc.name}
                  </span>
                  <span
                    className={`text-[9px] px-1.5 py-0.5 rounded font-bold ${
                      tc.malicious
                        ? "bg-rose-500/10 text-rose-600 dark:text-rose-400"
                        : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                    }`}
                  >
                    {tc.malicious ? "THREAT" : "SAFE"}
                  </span>
                </div>
                <p className="text-[11px] text-zinc-500 dark:text-zinc-400 line-clamp-2">
                  {tc.snippet}
                </p>
              </div>
              <button
                onClick={() =>
                  handleSimulate(tc.name, tc.app, tc.sender, tc.snippet, tc.malicious)
                }
                className="mt-3 w-full py-1.5 rounded text-[11px] font-semibold bg-zinc-900 dark:bg-zinc-700 text-white hover:bg-zinc-800 dark:hover:bg-zinc-600 transition-colors"
              >
                Simulate Interception
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* 4. Real-time Interception Feed */}
      <div className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/60 p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <Bell className="w-4 h-4 text-zinc-500" />
            <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
              Live Intercept Feed ({events.length})
            </h3>
          </div>
          <button
            onClick={() => setEvents([])}
            className="text-xs text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200 transition-colors"
          >
            Clear Feed
          </button>
        </div>

        <div className="space-y-3">
          <AnimatePresence initial={false}>
            {events.map((evt) => (
              <motion.div
                key={evt.id}
                initial={{ opacity: 0, y: -8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                transition={{ duration: MOTION_DURATION.micro }}
                className={`p-4 rounded-lg border transition-colors ${
                  evt.actionTaken === "BLOCKED"
                    ? "border-rose-200 dark:border-rose-900/40 bg-rose-50/30 dark:bg-rose-950/20"
                    : "border-zinc-200 dark:border-zinc-800 bg-zinc-50/40 dark:bg-zinc-800/30"
                }`}
              >
                <div className="flex items-center justify-between gap-2 mb-2">
                  <div className="flex items-center space-x-2">
                    <span className="text-xs font-semibold px-2 py-0.5 rounded bg-zinc-200 dark:bg-zinc-800 text-zinc-800 dark:text-zinc-200">
                      {evt.appName}
                    </span>
                    <span className="text-xs font-medium text-zinc-600 dark:text-zinc-300">
                      {evt.sender}
                    </span>
                    <span className="text-[10px] text-zinc-400 font-mono">
                      {evt.timestamp}
                    </span>
                  </div>

                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                      evt.actionTaken === "BLOCKED"
                        ? "bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/30"
                        : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                    }`}
                  >
                    {evt.actionTaken}
                  </span>
                </div>

                <p className="text-xs text-zinc-800 dark:text-zinc-200 mb-2">
                  {evt.snippet}
                </p>

                <div className="flex items-center justify-between text-[10px] text-zinc-500 dark:text-zinc-400 font-mono pt-2 border-t border-zinc-200/50 dark:border-zinc-800/50">
                  <span>
                    Category: {evt.category} • Latency: {(evt.latencyUs / 1000).toFixed(2)}ms
                  </span>
                  {evt.actionTaken === "BLOCKED" && (
                    <span className="text-rose-600 dark:text-rose-400 font-semibold">
                      Quarantined on-device
                    </span>
                  )}
                </div>

                {evt.xaiReason && (
                  <div className="mt-2 text-[11px] text-zinc-600 dark:text-zinc-400 bg-white/60 dark:bg-zinc-900/60 p-2 rounded border border-zinc-200 dark:border-zinc-800">
                    <span className="font-semibold text-sky-600 dark:text-sky-400">XAI Reason: </span>
                    {evt.xaiReason}
                  </div>
                )}
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
};
