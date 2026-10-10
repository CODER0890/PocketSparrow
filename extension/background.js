// Pocket Sparrow Browser Extension - Background Service Worker
// Communicates with local loopback bridge strictly at 127.0.0.1:41789 with 0 WAN telemetry.

const BRIDGE_ENDPOINT = "http://127.0.0.1:41789/scan";
const SESSION_BYPASS = new Set();

// Initialize badge
chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.local.get(["blockedToday", "allowlist"], (data) => {
    const count = data.blockedToday || 0;
    updateBadge(count);
    if (!data.allowlist) {
      chrome.storage.local.set({ allowlist: ["example.com", "rfc-editor.org"] });
    }
  });
});

function updateBadge(count) {
  if (count > 0) {
    chrome.action.setBadgeText({ text: String(count) });
    chrome.action.setBadgeBackgroundColor({ color: "#F43F5E" });
  } else {
    chrome.action.setBadgeText({ text: "" });
  }
}

// Intercept web navigation
chrome.webNavigation.onBeforeNavigate.addListener(async (details) => {
  // Only evaluate top-level main frame navigations
  if (details.frameId !== 0) return;
  const rawUrl = details.url;

  // Ignore internal and extension pages
  if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) return;
  if (rawUrl.includes("127.0.0.1:41789") || rawUrl.includes("chrome-extension://")) return;

  try {
    const parsed = new URL(rawUrl);
    const hostname = parsed.hostname.toLowerCase();

    // Check session bypass
    if (SESSION_BYPASS.has(hostname) || SESSION_BYPASS.has(rawUrl)) {
      return;
    }

    // Check persistent allowlist
    const stored = await chrome.storage.local.get(["allowlist", "blockedToday"]);
    const allowlist = stored.allowlist || [];
    if (allowlist.includes(hostname)) {
      return;
    }

    // Query local Pocket Sparrow engine over loopback socket
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 45); // <50ms SLA

    const response = await fetch(BRIDGE_ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url: rawUrl }),
      signal: controller.signal,
    }).catch(() => null);

    clearTimeout(timeout);

    if (!response || !response.ok) return;

    const data = await response.json();

    if (data.should_block) {
      const nextCount = (stored.blockedToday || 0) + 1;
      await chrome.storage.local.set({ blockedToday: nextCount });
      updateBadge(nextCount);

      // Redirect to full-page XAI warning card
      const blockedPage = chrome.runtime.getURL("blocked.html") +
        `?url=${encodeURIComponent(rawUrl)}` +
        `&category=${encodeURIComponent(data.category || "URL_PHISHING")}` +
        `&reason=${encodeURIComponent(data.xai_reason || "Malicious markers detected by on-device model.")}` +
        `&verdict=${encodeURIComponent(data.verdict || "Malicious")}` +
        `&latency=${encodeURIComponent(data.latency_us || 18420)}`;

      chrome.tabs.update(details.tabId, { url: blockedPage });
    }
  } catch (err) {
    // Fail-open securely on socket failure to not disrupt browsing
    console.warn("[Pocket Sparrow Extension] Evaluation bypass:", err);
  }
});

// Message listener from blocked.html and popup.html
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === "BYPASS_URL") {
    try {
      const parsed = new URL(message.url);
      SESSION_BYPASS.add(parsed.hostname.toLowerCase());
      SESSION_BYPASS.add(message.url);
      sendResponse({ success: true });
    } catch {
      sendResponse({ success: false });
    }
  } else if (message.type === "TRUST_DOMAIN") {
    chrome.storage.local.get(["allowlist"], (data) => {
      const current = data.allowlist || [];
      if (!current.includes(message.domain)) {
        current.push(message.domain);
        chrome.storage.local.set({ allowlist: current }, () => {
          sendResponse({ success: true });
        });
      } else {
        sendResponse({ success: true });
      }
    });
    return true; // Keep sendResponse open for async
  }
});
