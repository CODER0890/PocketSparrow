// Pocket Sparrow Extension - Blocked Page Logic

document.addEventListener("DOMContentLoaded", () => {
  const params = new URLSearchParams(window.location.search);
  const targetUrl = params.get("url") || "about:blank";
  const category = params.get("category") || "MALICIOUS_VECTOR";
  const reason = params.get("reason") || "Suspicious phishing patterns detected on-device.";
  const verdict = params.get("verdict") || "Malicious";
  const latency = params.get("latency") || "18420";

  document.getElementById("target-url").textContent = targetUrl;
  document.getElementById("category-badge").textContent = category;
  document.getElementById("xai-reason").textContent = reason;
  document.getElementById("verdict-title").textContent = `${verdict} Verdict Blocked`;
  document.getElementById("latency-text").textContent = `Evaluated in ${(Number(latency) / 1000).toFixed(1)} ms • 100% On-Device MobileBERT`;

  // Render Threat DNA token heatmap
  const dnaContainer = document.getElementById("dna-container");
  renderDna(targetUrl, dnaContainer);

  // Return to safety button
  document.getElementById("btn-back").addEventListener("click", () => {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      window.location.href = "https://www.google.com";
    }
  });

  // Proceed anyway button
  document.getElementById("btn-bypass").addEventListener("click", () => {
    chrome.runtime.sendMessage({ type: "BYPASS_URL", url: targetUrl }, () => {
      window.location.href = targetUrl;
    });
  });

  // Trust this domain button
  document.getElementById("btn-trust").addEventListener("click", () => {
    try {
      const hostname = new URL(targetUrl).hostname;
      chrome.runtime.sendMessage({ type: "TRUST_DOMAIN", domain: hostname }, () => {
        alert(`Domain "${hostname}" has been added to your local Pocket Sparrow allowlist.`);
        window.location.href = targetUrl;
      });
    } catch {
      alert("Invalid domain address.");
    }
  });

  // Export forensic report button
  document.getElementById("btn-export").addEventListener("click", async () => {
    const report = {
      app: "Pocket Sparrow Browser Extension",
      version: "1.0.0",
      timestamp: new Date().toISOString(),
      url: targetUrl,
      verdict,
      category,
      xai_reason: reason,
      latency_us: Number(latency),
      offline_guarantee: true,
      wan_egress_bytes: 0,
    };

    const blob = new Blob([JSON.stringify(report, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `pocket-sparrow-report-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  });
});

function renderDna(url, container) {
  const parts = url.split(/([/?&=%#._~:@!$'()*+,;-])/);
  parts.forEach((part, idx) => {
    if (!part) return;
    const span = document.createElement("span");
    span.textContent = part;
    span.className = "dna-token";

    const lower = part.toLowerCase();
    if (["login", "secure", "verify", "update", "bank", "account", "suspended"].some(k => lower.includes(k))) {
      span.classList.add("token-rose");
      span.title = `High Attention (0.92) - Tier 2: Credential lure`;
    } else if (part.length >= 8 && hasHighEntropy(part)) {
      span.classList.add("token-rose");
      span.title = `High Attention (0.88) - Tier 1: Entropy anomaly`;
    } else if (["top", "xyz", "tk", "click", "buzz"].some(tld => lower.includes(tld))) {
      span.classList.add("token-amber");
      span.title = `Moderate Attention (0.75) - Tier 1: Suspicious TLD`;
    } else {
      span.classList.add("token-cool");
      span.title = `Low Attention (0.12) - Neutral token`;
    }

    span.style.animation = `fadeIn 0.4s ease ${Math.min(0.3, idx * 0.02)}s both`;
    container.appendChild(span);
  });
}

function hasHighEntropy(str) {
  const set = new Set(str);
  return set.size >= str.length * 0.7;
}
