document.getElementById("check-btn").addEventListener("click", async () => {
  const resultDiv = document.getElementById("result");
  resultDiv.innerHTML = "Inspecting on-device...";

  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab || !tab.url) {
      resultDiv.innerHTML = "No active URL found.";
      return;
    }

    const res = await fetch("http://127.0.0.1:41789/scan", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url: tab.url }),
    });

    if (res.ok) {
      const data = await res.json();
      if (data.should_block) {
        resultDiv.innerHTML = `<span style="color: #f87171; font-weight: bold;">[MALICIOUS THREAT BLOCKED]</span><br>${data.xai_reason}`;
      } else {
        resultDiv.innerHTML = `<span style="color: #34d399; font-weight: bold;">[VERIFIED SAFE]</span><br>${data.xai_reason}`;
      }
    } else {
      resultDiv.innerHTML = "Local engine error.";
    }
  } catch (err) {
    resultDiv.innerHTML = "Could not connect to Pocket Sparrow daemon.";
  }
});
