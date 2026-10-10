// Pocket Sparrow Extension - Popup Logic

document.addEventListener("DOMContentLoaded", () => {
  // 1. Fetch blocked count and allowlist from storage
  chrome.storage.local.get(["blockedToday", "allowlist"], (data) => {
    const count = data.blockedToday || 0;
    document.getElementById("threat-count").textContent = String(count);

    const allowlist = data.allowlist || ["example.com"];
    renderAllowlist(allowlist);
  });

  // 2. Health check local bridge endpoint (127.0.0.1:41789)
  fetch("http://127.0.0.1:41789/scan", {
    method: "OPTIONS",
  })
    .then((res) => {
      if (res.ok || res.status === 204) {
        setStatus(true);
      } else {
        setStatus(false);
      }
    })
    .catch(() => {
      setStatus(false);
    });
});

function setStatus(isConnected) {
  const pill = document.getElementById("status-pill");
  const text = document.getElementById("status-text");
  if (isConnected) {
    pill.className = "status-pill connected";
    text.textContent = "Engine Connected";
  } else {
    pill.style.background = "rgba(244, 63, 94, 0.1)";
    pill.style.color = "#fda4af";
    pill.style.borderColor = "rgba(244, 63, 94, 0.3)";
    text.textContent = "Engine Offline";
  }
}

function renderAllowlist(domains) {
  const list = document.getElementById("domain-list");
  const countEl = document.getElementById("allowlist-count");
  list.innerHTML = "";
  countEl.textContent = `${domains.length} domains`;

  if (domains.length === 0) {
    list.innerHTML = `<li class="domain-item" style="color: #71717a;">No trusted domains yet.</li>`;
    return;
  }

  domains.forEach((dom) => {
    const li = document.createElement("li");
    li.className = "domain-item";
    li.innerHTML = `
      <span>${dom}</span>
      <button class="remove-btn" title="Remove domain">✕</button>
    `;
    li.querySelector(".remove-btn").addEventListener("click", () => {
      removeDomain(dom);
    });
    list.appendChild(li);
  });
}

function removeDomain(domain) {
  chrome.storage.local.get(["allowlist"], (data) => {
    const filtered = (data.allowlist || []).filter((d) => d !== domain);
    chrome.storage.local.set({ allowlist: filtered }, () => {
      renderAllowlist(filtered);
    });
  });
}
