// Pocket Sparrow - Background Service Worker
// Communicates strictly with local daemon on 127.0.0.1:41789 with 0 WAN egress

const LOCAL_BRIDGE_URL = "http://127.0.0.1:41789/scan";

chrome.webNavigation.onBeforeNavigate.addListener(async (details) => {
  // Only inspect top-level navigation
  if (details.frameId !== 0) return;
  const targetUrl = details.url;

  // Ignore internal browser pages and loopback
  if (targetUrl.startsWith("chrome://") || targetUrl.startsWith("about:") || targetUrl.includes("127.0.0.1")) {
    return;
  }

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 40); // 40ms strict timeout

    const response = await fetch(LOCAL_BRIDGE_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url: targetUrl }),
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (response.ok) {
      const data = await response.json();
      if (data.should_block) {
        console.warn("[POCKET SPARROW BLOCKED]", targetUrl, data);
        // Show notification or redirect to warning
        chrome.tabs.update(details.tabId, {
          url: chrome.runtime.getURL(`popup/blocked.html?url=${encodeURIComponent(targetUrl)}&reason=${encodeURIComponent(data.xai_reason)}&cat=${encodeURIComponent(data.category)}`),
        });
      }
    }
  } catch (err) {
    // Daemon offline or timed out, continue fail-safe
    console.debug("[POCKET SPARROW BRIDGE]", err.message);
  }
});
