const venueOpsUrl = "http://localhost:8080";
const operatorToken = "local-development-token";
const guestTodayUrl = "http://localhost:3000/today";
const towerDashboardUrl = "http://localhost:3001/dashboard";
const pollMs = 2000;
const spentWaitMs = 8000;
const resetNote =
  "Mangrove disruption is spent. Park Flow reuses one recommendation id, so this database will not open a new pending review. From lumen-marsh-platform, run ./scripts/reset-demo.sh --confirm and start the stack again.";

const operatorHeaders = {
  Accept: "application/json",
  Authorization: `Bearer ${operatorToken}`,
};

const cueSection = {
  "lightning-hold": "weather-heading",
  "cypress-vibration": "needs-attention-heading",
  "mangrove-disruption": "needs-attention-heading",
};

let refreshToken = 0;
let mangroveWatch = null;
let cueFollow = null;
const sending = new Set();
let framesWereCovered = false;

const text = (id, value) => {
  document.getElementById(id).textContent = value;
};

const resultNode = (id) => document.querySelector(`[data-result="${id}"]`);

const setResult = (id, state, value) => {
  const node = resultNode(id);
  node.dataset.state = state;
  node.textContent = value;
};

async function readJson(url, headers) {
  try {
    const response = await fetch(url, { cache: "no-store", headers });
    if (!response.ok) {
      return { ok: false, status: response.status, body: null };
    }
    return { ok: true, status: response.status, body: await response.json() };
  } catch {
    return { ok: false, status: 0, body: null };
  }
}

async function postScenario(url) {
  try {
    const response = await fetch(url, {
      method: "POST",
      cache: "no-store",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: "{}",
    });
    let body = null;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    return { ok: response.ok, status: response.status, body };
  } catch {
    return { ok: false, status: 0, body: null };
  }
}

const byUpdatedAt = (left, right) => String(right.updatedAt || "").localeCompare(String(left.updatedAt || ""));

const newest = (items) => items.slice().sort(byUpdatedAt)[0];

const attention = (dashboard, kind) =>
  (dashboard.needsAttention || []).filter((item) => item.kind === kind);

const lane = (items, label) => {
  if (!items.length) return "None";
  const current = newest(items);
  const name = label(current) || "Pending";
  return items.length === 1 ? name : `${items.length} pending. Newest: ${name}`;
};

const candidatesFrom = (dashboard) => {
  const weather = (dashboard.pendingWeatherRecommendations || []).map((item) => ({
    updatedAt: item.updatedAt,
    kind: "Pending weather",
    text: item.recommendedAction || "Pending",
  }));
  const reliability = attention(dashboard, "PENDING_RELIABILITY_RECOMMENDATION").map((item) => ({
    updatedAt: item.updatedAt,
    kind: "Pending reliability",
    text: item.subjectLabel || item.reason || "Pending",
  }));
  const flow = attention(dashboard, "UNPUBLISHED_FLOW_RECOMMENDATION").map((item) => ({
    updatedAt: item.updatedAt,
    kind: "Unpublished flow",
    text: item.subjectLabel || item.reason || "Pending",
  }));
  return [...weather, ...reliability, ...flow];
};

const advisoryLine = (items) => {
  if (!items.length) return "Quiet";
  return items
    .map((item) => (item.message ? `${item.title || "Advisory"}: ${item.message}` : item.title || "Advisory"))
    .join(" ");
};

const bestNextLine = (items) => {
  if (!items.length) return "Quiet";
  return items.map((item) => item.guestMessage || "Published guidance").join(" ");
};

const projectionLine = (advisories, guidance) => {
  const hasAdvisory = advisories.length > 0;
  const hasBestNext = guidance.length > 0;
  if (!hasAdvisory && !hasBestNext) return "Unchanged";
  if (hasAdvisory && hasBestNext) return "Changed: advisory and Best Next";
  if (hasAdvisory) return "Changed: advisory";
  return "Changed: Best Next";
};

const setProjection = (value) => {
  const node = document.getElementById("guest-projection");
  node.textContent = value;
  if (value.startsWith("Changed")) node.dataset.state = "changed";
  else if (value === "Unread") node.dataset.state = "unread";
  else node.dataset.state = "unchanged";
};

const syncRail = () => {
  const offline = document.body.classList.contains("is-offline");
  document.querySelectorAll(".rail button").forEach((button) => {
    button.disabled = offline || sending.has(button.id);
  });
};

const navigateFrame = (frame, url) => {
  try {
    frame.contentWindow.location.href = url;
    return;
  } catch {
    frame.src = url;
  }
};

const showGuestToday = () => {
  navigateFrame(document.getElementById("guest-frame"), guestTodayUrl);
};

const showTowerCard = (hash) => {
  navigateFrame(document.getElementById("console-frame"), `${towerDashboardUrl}#${hash}`);
};

const remountFrames = () => {
  const stamp = Date.now();
  document.getElementById("guest-frame").src = `${guestTodayUrl}?booth=${stamp}`;
  document.getElementById("console-frame").src = `${towerDashboardUrl}?booth=${stamp}`;
};

const setBoothOnline = (online) => {
  const offline = !online;
  if (framesWereCovered && online) remountFrames();
  framesWereCovered = offline;
  document.body.classList.toggle("is-offline", offline);
  document.getElementById("booth-offline").hidden = online;
  syncRail();
};

const renderOffline = () => {
  text("last-recommendation", "VenueOps is offline.");
  text("pending-weather", "Offline");
  text("pending-reliability", "Offline");
  text("unpublished-flow", "Offline");
  text("guest-advisory", "Offline");
  text("guest-best-next", "Offline");
  setProjection("Offline");
  setBoothOnline(false);
};

function renderStrip(dashboardResult, advisoryResult, guidanceResult) {
  if (!dashboardResult.ok || !dashboardResult.body) {
    renderOffline();
    return null;
  }

  setBoothOnline(true);
  const dashboard = dashboardResult.body;
  const weather = dashboard.pendingWeatherRecommendations || [];
  const reliability = attention(dashboard, "PENDING_RELIABILITY_RECOMMENDATION");
  const flow = attention(dashboard, "UNPUBLISHED_FLOW_RECOMMENDATION");
  const candidates = candidatesFrom(dashboard);
  text(
    "last-recommendation",
    candidates.length ? `${newest(candidates).kind}: ${newest(candidates).text}` : "None",
  );
  text("pending-weather", lane(weather, (item) => item.recommendedAction));
  text("pending-reliability", lane(reliability, (item) => item.subjectLabel || item.reason));
  text("unpublished-flow", lane(flow, (item) => item.subjectLabel || item.reason));

  if (!advisoryResult.ok || !guidanceResult.ok || !Array.isArray(advisoryResult.body) || !Array.isArray(guidanceResult.body)) {
    text("guest-advisory", "Unread");
    text("guest-best-next", "Unread");
    setProjection("Unread");
    return dashboard;
  }

  text("guest-advisory", advisoryLine(advisoryResult.body));
  text("guest-best-next", bestNextLine(guidanceResult.body));
  setProjection(projectionLine(advisoryResult.body, guidanceResult.body));
  return dashboard;
}

const cardHash = (buttonId, dashboard) => {
  if (buttonId === "lightning-hold") {
    const item = newest(dashboard.pendingWeatherRecommendations || []);
    if (item?.id) return `weather-card-${item.id}`;
    return null;
  }
  const kind =
    buttonId === "cypress-vibration"
      ? "PENDING_RELIABILITY_RECOMMENDATION"
      : "UNPUBLISHED_FLOW_RECOMMENDATION";
  const item = newest(attention(dashboard, kind));
  if (item?.subjectId) return `attention-card-${kind}-${item.subjectId}`;
  return null;
};

function followCue(dashboard) {
  if (!cueFollow || cueFollow.done) return;
  if (!dashboard) {
    cueFollow.done = true;
    return;
  }
  const hash = cardHash(cueFollow.buttonId, dashboard);
  if (hash) {
    showTowerCard(hash);
    cueFollow.done = true;
    return;
  }
  if (Date.now() - cueFollow.startedAt >= spentWaitMs) {
    showTowerCard(cueSection[cueFollow.buttonId]);
    cueFollow.done = true;
  }
}

const hideResetNote = () => {
  const note = document.getElementById("reset-note");
  note.hidden = true;
  note.textContent = "";
};

const showResetNote = () => {
  const note = document.getElementById("reset-note");
  note.hidden = false;
  note.textContent = resetNote;
};

async function evaluateMangrove(token, dashboard) {
  const watch = mangroveWatch;
  if (!watch || watch.resolved || token !== refreshToken) return;

  if (watch.id) {
    const result = await readJson(
      `${venueOpsUrl}/api/v1/operator/flow/recommendations/${watch.id}`,
      operatorHeaders,
    );
    if (token !== refreshToken) return;
    const status = result.body?.recommendation?.status;
    if (status === "PENDING_REVIEW" || status === "APPROVED") {
      watch.resolved = true;
      hideResetNote();
      return;
    }
    if (typeof status === "string") {
      watch.resolved = true;
      showResetNote();
      return;
    }
  } else if (dashboard && attention(dashboard, "UNPUBLISHED_FLOW_RECOMMENDATION").length > 0) {
    watch.resolved = true;
    hideResetNote();
    return;
  }

  if (Date.now() - watch.startedAt >= spentWaitMs) {
    watch.resolved = true;
    showResetNote();
  }
}

async function refresh() {
  const token = ++refreshToken;
  const [dashboardResult, advisoryResult, guidanceResult] = await Promise.all([
    readJson(`${venueOpsUrl}/api/v1/operator/dashboard`, operatorHeaders),
    readJson(`${venueOpsUrl}/api/v1/advisories`),
    readJson(`${venueOpsUrl}/api/v1/flow/recommendations`),
  ]);
  if (token !== refreshToken) return;
  const dashboard = renderStrip(dashboardResult, advisoryResult, guidanceResult);
  followCue(dashboard);
  await evaluateMangrove(token, dashboard);
}

async function fire(button) {
  if (button.disabled || sending.has(button.id)) return;
  sending.add(button.id);
  syncRail();
  setResult(button.id, "pending", "Sending");
  const result = await postScenario(button.dataset.url);
  setResult(button.id, result.ok ? "fired" : "failed", result.ok ? "Fired" : result.status ? `Failed (${result.status})` : "Failed");
  if (result.ok && button.dataset.watch === "mangrove") {
    const recommendationId = result.body?.recommendationId;
    mangroveWatch = {
      id: typeof recommendationId === "string" && recommendationId ? recommendationId : null,
      startedAt: Date.now(),
      resolved: false,
    };
    hideResetNote();
  }
  if (result.ok && cueSection[button.id]) {
    cueFollow = { buttonId: button.id, startedAt: Date.now(), done: false };
    showGuestToday();
  }
  sending.delete(button.id);
  syncRail();
  await refresh();
}

document.querySelectorAll(".rail button").forEach((button) => {
  button.addEventListener("click", () => {
    void fire(button);
  });
});

void refresh();
window.setInterval(() => {
  void refresh();
}, pollMs);
