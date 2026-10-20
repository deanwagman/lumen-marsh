const guestTodayUrl = "/guest/today";
const towerDashboardUrl = "/tower/dashboard";
const pollMs = 2000;
const spentWaitMs = 8000;
const heartbeatMs = 15000;
const weatherIntentKey = "cutaway.weather-intent.v1";
const weatherPhases = new Set(["PLACE_WEATHER_HOLD", "RETURN_TO_SERVICE"]);
const safeAttractionId = /^[a-z0-9][a-z0-9-]{0,63}$/;
const resetNote = "This scenario has already been used in this run. Choose Start over to return the shared demo to its quiet opening.";

const cueSection = {
  "lightning-hold": "weather-heading",
  "cypress-vibration": "needs-attention-heading",
  "mangrove-disruption": "needs-attention-heading",
};

const openingDwellMs = 6000;

const openingCopy = {
  quiet: {
    cue: "Press Lightning hold.",
    detail: "Control Tower will get a decision, and the guest room will not, until someone publishes.",
  },
  operator: {
    cue: "In Control Tower, review or acknowledge the weather recommendation.",
    detail: "The recommendation does not place any attraction on hold.",
  },
  guest: {
    cue: "The ride still looks open.",
    detail: "That is the published picture.",
  },
  publish: {
    cue: "Place the affected attractions on weather hold as a separate action, then publish guest guidance.",
    detail:
      "This room opens as Local Operator, already signed in. If it shows VenueOps sign in, sign in with your operator account to open the console.",
  },
  clear: {
    cue: "Press Clear the storm.",
    detail: "Clear the storm is the way back to the quiet park.",
  },
  unread: {
    cue: "The guest side is unread, so this page cannot say the ride still looks open.",
    detail: "",
  },
};

const primaryTrigger = {
  quiet: "trigger-lightning-hold",
  clear: "trigger-clear-storm",
};

const triggerOrder = [
  "trigger-lightning-hold",
  "trigger-clear-storm",
  "trigger-cypress-vibration",
  "trigger-mangrove-disruption",
];

const roomForBeat = {
  operator: "tower",
  unread: "tower",
  publish: "tower",
  "publish-guidance": "tower",
  guest: "guest",
  "hold-rides": "tower",
  clearance: "tower",
  recovery: "tower",
  withdraw: "tower",
};

const openingTargets = {
  quiet: ["trigger-lightning-hold"],
  operator: ["tower-room", "strip-operator"],
  guest: ["guest-room", "strip-guest"],
  publish: ["tower-room"],
  "publish-guidance": ["tower-room", "strip-operator"],
  clear: ["trigger-clear-storm"],
  unread: ["tower-room", "strip-operator"],
  "hold-rides": ["tower-room", "strip-operator"],
  clearance: ["tower-room", "strip-operator"],
  recovery: ["tower-room", "strip-operator"],
  withdraw: ["tower-room", "strip-guest"],
};

let refreshToken = 0;
let mangroveWatch = null;
let cueFollow = null;
const sending = new Set();
let framesWereCovered = false;
let openingPhase = null;
let openingTimer = null;
let openingReading = null;
let shownRoom = "guest";
let triggeredWeatherPhase = null;
let hasLease = false;
let leaseExpiresAt = 0;
let leaseTimer = null;
let heartbeatTimer = null;
let resetInProgress = false;
let coreReady = false;
const scenarioReady = new Map();

const safeAttractionIds = (items) =>
  Array.isArray(items)
    ? [...new Set(items.filter((item) => typeof item === "string" && safeAttractionId.test(item)))].slice(0, 20)
    : [];

const attractionLabel = (id) =>
  id
    .split("-")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");

const readWeatherIntent = () => {
  try {
    const value = JSON.parse(window.sessionStorage.getItem(weatherIntentKey) || "null");
    if (!value || !weatherPhases.has(value.phase)) return null;
    const affectedAttractionIds = safeAttractionIds(value.affectedAttractionIds);
    return affectedAttractionIds.length ? { phase: value.phase, affectedAttractionIds } : null;
  } catch {
    return null;
  }
};

let weatherIntent = readWeatherIntent();

const retainWeatherIntent = (phase, affectedAttractionIds) => {
  if (!weatherPhases.has(phase)) return;
  const safeIds = safeAttractionIds(affectedAttractionIds);
  if (!safeIds.length) return;
  weatherIntent = { phase, affectedAttractionIds: safeIds };
  try {
    window.sessionStorage.setItem(weatherIntentKey, JSON.stringify(weatherIntent));
  } catch {
    // The live dashboard still recovers phase when session storage is unavailable.
  }
};

const clearWeatherIntent = () => {
  weatherIntent = null;
  try {
    window.sessionStorage.removeItem(weatherIntentKey);
  } catch {
    // There is no retained state to clear when storage is unavailable.
  }
};

const text = (id, value) => {
  document.getElementById(id).textContent = value;
};

const resultNode = (id) => document.querySelector(`[data-result="${id}"]`);

const setResult = (id, state, value) => {
  const node = resultNode(id);
  node.dataset.state = state;
  node.textContent = value;
};

async function requestJson(url, options = {}) {
  try {
    const response = await fetch(url, {
      cache: "no-store",
      credentials: "same-origin",
      ...options,
      headers: { Accept: "application/json", ...(options.headers || {}) },
    });
    let body = null;
    if (response.status !== 204) {
      try {
        body = await response.json();
      } catch {
        body = null;
      }
    }
    if (!response.ok) {
      return { ok: false, status: response.status, body };
    }
    return { ok: true, status: response.status, body };
  } catch {
    return { ok: false, status: 0, body: null };
  }
}

const postJson = (url) =>
  requestJson(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: "{}",
  });

const readyValue = (value, fallback = true) => {
  if (typeof value === "boolean") return value;
  if (!value || typeof value !== "object") return fallback;
  if (typeof value.ready === "boolean") return value.ready;
  if (typeof value.available === "boolean") return value.available;
  if (typeof value.healthy === "boolean") return value.healthy;
  const status = String(value.status || value.state || "").toLowerCase();
  return status ? ["ready", "healthy", "available", "up", "ok"].includes(status) : fallback;
};

const readinessPart = (body, ...names) => {
  for (const name of names) {
    const value = body?.scenarios?.[name] ?? body?.producers?.[name] ?? body?.dependencies?.[name] ?? body?.[name];
    if (value !== undefined) return readyValue(value);
    const component = Array.isArray(body?.components)
      ? body.components.find((item) =>
          String(item.scenario || item.name || "").toLowerCase() === name.toLowerCase())
      : null;
    if (component) return readyValue(component);
  }
  return true;
};

function renderReadiness(body, reachable) {
  coreReady = reachable && readyValue(body?.core ?? body?.booth ?? body, reachable);
  const weather = readinessPart(body, "weather", "environmental", "environmental-monitor");
  scenarioReady.set("lightning-hold", readinessPart(body, "lightning-hold") && weather);
  scenarioReady.set("clear-storm", readinessPart(body, "clear-storm") && weather);
  scenarioReady.set("cypress-vibration", readinessPart(body, "cypress-vibration", "reliability", "reliability-intelligence"));
  scenarioReady.set("mangrove-disruption", readinessPart(body, "mangrove-disruption", "flow", "park-flow", "park-flow-intelligence"));
  const summary = [
    ["Core", coreReady],
    ["Weather", scenarioReady.get("lightning-hold")],
    ["Reliability", scenarioReady.get("cypress-vibration")],
    ["Flow", scenarioReady.get("mangrove-disruption")],
  ];
  const node = document.getElementById("readiness");
  node.replaceChildren(
    ...summary.map(([label, ready]) => {
      const item = document.createElement("span");
      item.className = ready ? "is-ready" : "is-down";
      item.textContent = `${label} ${ready ? "ready" : "unavailable"}`;
      return item;
    }),
  );
  setBoothOnline(coreReady);
  syncRail();
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
  document.querySelectorAll(".trigger button").forEach((button) => {
    button.disabled =
      offline ||
      !hasLease ||
      resetInProgress ||
      scenarioReady.get(button.dataset.scenario) === false ||
      sending.has(button.id);
    const producerDown = scenarioReady.get(button.dataset.scenario) === false;
    if (producerDown && !sending.has(button.id)) setResult(button.id, "failed", "Unavailable");
    else if (!producerDown && resultNode(button.id)?.textContent === "Unavailable") setResult(button.id, "", "Ready");
  });
  document.getElementById("start-over").disabled = offline || !hasLease || resetInProgress;
};

const placeTriggers = (beat) => {
  const primaryId = primaryTrigger[beat] || null;
  const now = document.getElementById("now-action");
  const drawer = document.getElementById("explore-triggers");
  for (const id of triggerOrder) {
    const node = document.getElementById(id);
    (id === primaryId ? now : drawer).appendChild(node);
  }
};

const selectRoom = (which) => {
  shownRoom = which;
  document.getElementById("guest-room").classList.toggle("is-shown", which === "guest");
  document.getElementById("tower-room").classList.toggle("is-shown", which === "tower");
  document.getElementById("tab-guest").setAttribute("aria-selected", which === "guest" ? "true" : "false");
  document.getElementById("tab-tower").setAttribute("aria-selected", which === "tower" ? "true" : "false");
  document.getElementById("tab-guest").tabIndex = which === "guest" ? 0 : -1;
  document.getElementById("tab-tower").tabIndex = which === "tower" ? 0 : -1;
};

const syncTabCue = () => {
  document.getElementById("tab-guest").classList.toggle("is-cued", document.getElementById("guest-room").classList.contains("is-cued"));
  document.getElementById("tab-tower").classList.toggle("is-cued", document.getElementById("tower-room").classList.contains("is-cued"));
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

const isHoldPhase = (beat) => beat === "operator" || beat === "guest" || beat === "publish";

const observedBeat = (reading) => {
  if (!reading.online) return "offline";
  const weather = reading.pendingWeather;
  const advisory = reading.guestAdvisory;
  if (weather === "Offline" || advisory === "Offline") return "offline";
  if (weather === "Reading VenueOps." || advisory === "Reading VenueOps.") return "waiting";
  if (advisory === "Unread") {
    return reading.weatherPhase === null && reading.heldAttractions.length === 0 ? "waiting" : "unread";
  }
  if (reading.weatherPhase === "RETURN_TO_SERVICE") {
    if (reading.hasPendingReturn) return "clearance";
    if (reading.heldAttractions.length > 0 || reading.hasWeatherAdvisory) return "recovery";
    return "quiet";
  }
  if (reading.weatherPhase === "PLACE_WEATHER_HOLD") {
    if (reading.allAffectedHeld) return reading.hasWeatherAdvisory ? "clear" : "publish-guidance";
    if (reading.hasWeatherAdvisory) return "hold-rides";
    if (reading.hasPendingHold) return "hold";
    return "hold-rides";
  }
  if (reading.hasPendingWeather) return "operator";
  if (reading.heldAttractions.length > 0) return "recovery";
  if (reading.hasWeatherAdvisory) return "withdraw";
  return "quiet";
};

const copyFor = (beat, reading) => {
  if (
    beat === "guest" &&
    reading &&
    (reading.guestAdvisory !== "Quiet" || reading.projection !== "Unchanged")
  ) {
    return {
      cue: "The guest advisory is still quiet, so this page will not call that the last open picture.",
      detail: "",
    };
  }
  if (beat === "hold-rides") {
    const names = reading.affectedAttractionIds.length
      ? reading.affectedAttractionIds.map(attractionLabel).join(" and ")
      : "The affected attractions";
    return {
      cue: `${names} ${reading.affectedAttractionIds.length === 1 ? "still needs" : "still need"} weather hold in Control Tower. Recommendation review alone does not hold a ride.`,
      detail: "",
    };
  }
  if (beat === "publish-guidance") {
    return {
      cue: "The affected attractions are on weather hold. Publish guest guidance in Control Tower.",
      detail: "",
    };
  }
  if (beat === "clearance") {
    const advisory =
      !reading.hasWeatherAdvisory
        ? "Guest guidance is quiet."
        : "The published guest advisory remains in place.";
    const conditions = reading.affectedConditions.length
      ? `${reading.affectedConditions.join("; ")}.`
      : "No affected attraction condition remains on the dashboard.";
    const incidents = reading.openAffectedIncidentCount
      ? ` ${reading.openAffectedIncidentCount} related ${reading.openAffectedIncidentCount === 1 ? "incident remains" : "incidents remain"} open.`
      : "";
    return {
      cue: `Review the return-to-service recommendation in Control Tower. ${advisory} ${conditions}${incidents}`,
      detail: "",
    };
  }
  if (beat === "recovery") {
    const conditions = reading.affectedConditions.length
      ? `${reading.affectedConditions.join("; ")}. `
      : "";
    const advisory = reading.hasWeatherAdvisory
      ? "Withdraw guest guidance after the operational decision. "
      : "";
    const incidents = reading.openAffectedIncidentCount
      ? `${reading.openAffectedIncidentCount} related ${reading.openAffectedIncidentCount === 1 ? "incident remains" : "incidents remain"} open.`
      : "";
    return {
      cue:
        `${conditions}${advisory}${incidents}`.trim() ||
        "Complete the return-to-service workflow in Control Tower.",
      detail: "",
    };
  }
  if (beat === "withdraw") {
    return {
      cue: "No weather action is pending and no attraction remains on weather hold. Withdraw the published guest guidance in Control Tower.",
      detail: "",
    };
  }
  return openingCopy[beat];
};

const clearOpeningMarks = () => {
  document.querySelectorAll(".is-cued").forEach((node) => node.classList.remove("is-cued"));
  document.getElementById("lightning-start").hidden = true;
  document.getElementById("opening-kicker").hidden = true;
};

const paintOpening = (beat) => {
  const opening = document.getElementById("opening");
  clearOpeningMarks();
  opening.dataset.beat = beat;
  if (beat === "offline" || beat === "waiting") {
    text("opening-cue", "");
    return;
  }
  const copy = copyFor(beat, openingReading);
  text("opening-cue", copy.cue);
  if (beat === "quiet") {
    document.getElementById("opening-kicker").hidden = false;
    document.getElementById("lightning-start").hidden = false;
  }
  for (const id of openingTargets[beat]) document.getElementById(id).classList.add("is-cued");
  placeTriggers(beat);
  if (roomForBeat[beat]) selectRoom(roomForBeat[beat]);
  syncTabCue();
};

const armOpeningDwell = () => {
  window.clearTimeout(openingTimer);
  openingTimer = window.setTimeout(() => {
    openingTimer = null;
    if (openingPhase === "operator") openingPhase = "guest";
    else if (openingPhase === "guest") openingPhase = "publish";
    else return;
    paintOpening(openingPhase);
    if (openingPhase === "guest") armOpeningDwell();
  }, openingDwellMs);
};

const leaveHold = () => {
  openingPhase = null;
  window.clearTimeout(openingTimer);
  openingTimer = null;
};

function syncOpening(reading) {
  openingReading = reading;
  const beat = observedBeat(reading);
  if (beat === "hold") {
    if (!isHoldPhase(openingPhase)) {
      openingPhase = "operator";
      armOpeningDwell();
    }
    paintOpening(openingPhase);
    return;
  }
  if (openingPhase !== null) leaveHold();
  paintOpening(beat);
}

const setLatest = (value) => {
  const node = document.getElementById("last-recommendation");
  node.textContent = value;
  node.hidden = value === "None";
};

const renderOffline = () => {
  setLatest("—");
  text("pending-weather", "—");
  text("weather-holds", "—");
  text("pending-reliability", "—");
  text("unpublished-flow", "—");
  text("guest-advisory", "—");
  text("guest-best-next", "—");
  setProjection("—");
  setBoothOnline(false);
  syncOpening({
    online: false,
    pendingWeather: "—",
    guestAdvisory: "—",
    projection: "—",
    weatherPhase: weatherIntent?.phase || null,
    hasPendingWeather: false,
    hasPendingHold: false,
    hasPendingReturn: false,
    hasWeatherAdvisory: false,
    heldAttractions: [],
    affectedAttractionIds: weatherIntent?.affectedAttractionIds || [],
    affectedConditions: [],
    allAffectedHeld: false,
    openAffectedIncidentCount: 0,
  });
};

function renderStrip(dashboardResult, advisoryResult, guidanceResult) {
  if (!dashboardResult.ok || !dashboardResult.body) {
    renderOffline();
    return null;
  }

  setBoothOnline(coreReady);
  const dashboard = dashboardResult.body;
  const weather = dashboard.pendingWeatherRecommendations || [];
  const currentWeather = newest(weather);
  const activeWorkflow = dashboard.activeWeatherWorkflow || null;
  const serverAction = activeWorkflow?.action || currentWeather?.action || null;
  const serverAffectedIds =
    activeWorkflow?.affectedAttractionIds || currentWeather?.affectedAttractionIds || [];
  if (serverAction === "RETURN_TO_SERVICE") {
    retainWeatherIntent("RETURN_TO_SERVICE", serverAffectedIds);
    triggeredWeatherPhase = null;
  } else if (
    serverAction === "PLACE_WEATHER_HOLD" &&
    triggeredWeatherPhase !== "RETURN_TO_SERVICE" &&
    (activeWorkflow !== null || weatherIntent?.phase !== "RETURN_TO_SERVICE")
  ) {
    retainWeatherIntent("PLACE_WEATHER_HOLD", serverAffectedIds);
    triggeredWeatherPhase = null;
  }
  let weatherPhase = weatherIntent?.phase || null;
  let affectedIds = weatherIntent?.affectedAttractionIds || [];
  const heldAttractions = (dashboard.attractionsNeedingAttention || []).filter(
    (item) => item.status === "WEATHER_HOLD",
  );
  const heldIds = new Set(heldAttractions.map((item) => item.id));
  const allAffectedHeld =
    affectedIds.length > 0 && affectedIds.every((attractionId) => heldIds.has(attractionId));
  const affectedIdSet = new Set(affectedIds);
  const affectedConditions = (dashboard.attractionsNeedingAttention || [])
    .filter((item) => affectedIdSet.has(item.id))
    .map((item) => `${item.name || item.id}: ${String(item.status || "needs attention").replaceAll("_", " ").toLowerCase()}`);
  const publishedWeatherAdvisories = (dashboard.publishedGuestAdvisories || []).filter((item) => {
    const advisoryIds = safeAttractionIds(item.affectedAttractionIds);
    return affectedIdSet.size === 0 || advisoryIds.some((id) => affectedIdSet.has(id));
  });
  const openAffectedIncidentCount = (dashboard.openIncidents || []).filter((item) => {
    const incidentIds = safeAttractionIds(item.attractionIds);
    return incidentIds.some((id) => affectedIdSet.has(id));
  }).length;
  const hasPendingHold =
    currentWeather?.action === "PLACE_WEATHER_HOLD" ||
    triggeredWeatherPhase === "PLACE_WEATHER_HOLD";
  const hasPendingReturn =
    currentWeather?.action === "RETURN_TO_SERVICE" ||
    triggeredWeatherPhase === "RETURN_TO_SERVICE";
  const isActuallyQuiet =
    weather.length === 0 &&
    heldAttractions.length === 0 &&
    publishedWeatherAdvisories.length === 0 &&
    openAffectedIncidentCount === 0;
  const hasServerWorkflow =
    activeWorkflow !== null ||
    currentWeather?.action === "PLACE_WEATHER_HOLD" ||
    currentWeather?.action === "RETURN_TO_SERVICE";
  if (
    weatherPhase !== null &&
    !hasServerWorkflow &&
    triggeredWeatherPhase === null &&
    isActuallyQuiet
  ) {
    clearWeatherIntent();
    triggeredWeatherPhase = null;
    weatherPhase = null;
    affectedIds = [];
  }
  const reliability = attention(dashboard, "PENDING_RELIABILITY_RECOMMENDATION");
  const flow = attention(dashboard, "UNPUBLISHED_FLOW_RECOMMENDATION");
  const candidates = candidatesFrom(dashboard);
  const pendingWeather = lane(weather, (item) => item.recommendedAction);
  setLatest(candidates.length ? `${newest(candidates).kind}: ${newest(candidates).text}` : "None");
  text("pending-weather", pendingWeather);
  text(
    "weather-holds",
    heldAttractions.length ? heldAttractions.map((item) => item.name || item.id).join(", ") : "None",
  );
  text("pending-reliability", lane(reliability, (item) => item.subjectLabel || item.reason));
  text("unpublished-flow", lane(flow, (item) => item.subjectLabel || item.reason));

  let guestAdvisory = "Unread";
  let guestBestNext = "Unread";
  let projection = "Unread";
  if (advisoryResult.ok && guidanceResult.ok && Array.isArray(advisoryResult.body) && Array.isArray(guidanceResult.body)) {
    guestAdvisory = advisoryLine(advisoryResult.body);
    guestBestNext = bestNextLine(guidanceResult.body);
    projection = projectionLine(advisoryResult.body, guidanceResult.body);
  }

  text("guest-advisory", guestAdvisory);
  text("guest-best-next", guestBestNext);
  setProjection(projection);
  syncOpening({
    online: true,
    pendingWeather,
    guestAdvisory,
    projection,
    weatherPhase,
    hasPendingWeather: weather.length > 0,
    hasPendingHold,
    hasPendingReturn,
    hasWeatherAdvisory: publishedWeatherAdvisories.length > 0,
    heldAttractions: heldAttractions.map((item) => item.name || item.id),
    affectedAttractionIds: affectedIds,
    affectedConditions,
    allAffectedHeld,
    openAffectedIncidentCount,
  });
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
    const result = await requestJson(
      `/venueops/api/v1/operator/flow/recommendations/${encodeURIComponent(watch.id)}`,
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
    requestJson("/venueops/api/v1/operator/dashboard"),
    requestJson("/venueops/api/v1/advisories"),
    requestJson("/venueops/api/v1/flow/recommendations"),
  ]);
  if (token !== refreshToken) return;
  const dashboard = renderStrip(dashboardResult, advisoryResult, guidanceResult);
  followCue(dashboard);
  await evaluateMangrove(token, dashboard);
}

async function fire(button) {
  if (button.disabled || !hasLease || sending.has(button.id)) return;
  sending.add(button.id);
  syncRail();
  setResult(button.id, "pending", "Sending");
  const result = await postJson(`/api/v1/demo/scenarios/${encodeURIComponent(button.dataset.scenario)}`);
  setResult(button.id, result.ok ? "fired" : "failed", result.ok ? "Fired" : result.status ? `Failed (${result.status})` : "Failed");
  if (result.ok && weatherPhases.has(button.dataset.weatherPhase)) {
    const affectedAttractionIds =
      weatherIntent?.affectedAttractionIds ||
      safeAttractionIds((button.dataset.affectedAttractions || "").split(","));
    triggeredWeatherPhase = button.dataset.weatherPhase;
    retainWeatherIntent(button.dataset.weatherPhase, affectedAttractionIds);
    if (openingReading) {
      syncOpening({
        ...openingReading,
        weatherPhase: button.dataset.weatherPhase,
        affectedAttractionIds,
        hasPendingHold: button.dataset.weatherPhase === "PLACE_WEATHER_HOLD",
        hasPendingReturn: button.dataset.weatherPhase === "RETURN_TO_SERVICE",
      });
    }
  }
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

const leaseBody = (body) => body?.lease || body || {};

const expiryFrom = (body) => {
  const lease = leaseBody(body);
  const raw = lease.expiresAt ?? lease.expires_at ?? lease.expiration;
  if (typeof raw === "number") return raw < 10_000_000_000 ? raw * 1000 : raw;
  if (typeof raw === "string" && !Number.isNaN(Date.parse(raw))) return Date.parse(raw);
  const seconds = lease.remainingSeconds ?? lease.remaining_seconds ??
    lease.expiresInSeconds ?? lease.expires_in_seconds ?? lease.ttlSeconds ?? lease.ttl_seconds;
  return typeof seconds === "number" ? Date.now() + seconds * 1000 : Date.now() + 5 * 60 * 1000;
};

const leaseIsMine = (body) => {
  const lease = leaseBody(body);
  return lease.mine === true || lease.owned === true || lease.holder === true || lease.isHolder === true ||
    lease.ownedByVisitor === true || ["holder", "controller", "active"].includes(String(lease.role || lease.mode || "").toLowerCase());
};

const leaseIsOccupied = (body) => {
  const lease = leaseBody(body);
  return lease.active === true || lease.occupied === true || lease.available === false ||
    ["active", "occupied", "leased", "spectator", "waiting"].includes(String(lease.status || lease.state || "").toLowerCase());
};

function paintLease() {
  const label = document.getElementById("lease-label");
  const countdown = document.getElementById("lease-countdown");
  const takeControl = document.getElementById("take-control");
  const releaseControl = document.getElementById("release-control");
  if (!hasLease) {
    label.textContent = "Watching another visitor";
    countdown.hidden = true;
    takeControl.hidden = false;
    releaseControl.hidden = true;
    takeControl.textContent = "Try when available";
    syncRail();
    return;
  }
  const seconds = Math.max(0, Math.ceil((leaseExpiresAt - Date.now()) / 1000));
  label.textContent = "You control the demo";
  countdown.hidden = false;
  countdown.textContent = `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
  countdown.setAttribute("aria-label", `${seconds} seconds remaining`);
  takeControl.hidden = true;
  releaseControl.hidden = false;
  if (seconds === 0) {
    loseLease("Your turn ended. You can keep watching.");
    return;
  }
  syncRail();
}

function loseLease(message) {
  hasLease = false;
  leaseExpiresAt = 0;
  window.clearInterval(heartbeatTimer);
  heartbeatTimer = null;
  document.getElementById("lease-label").textContent = message;
  paintLease();
}

function ownLease(body) {
  hasLease = true;
  leaseExpiresAt = expiryFrom(body);
  window.clearInterval(heartbeatTimer);
  heartbeatTimer = window.setInterval(async () => {
    const result = await postJson("/api/v1/demo/leases/heartbeat");
    if (!result.ok) {
      if (result.status === 401 || result.status === 403 || result.status === 404 || result.status === 409) {
        loseLease("Your turn ended. You can keep watching.");
      }
      return;
    }
    leaseExpiresAt = expiryFrom(result.body);
    paintLease();
  }, heartbeatMs);
  paintLease();
}

async function acquireLease() {
  document.getElementById("take-control").hidden = true;
  document.getElementById("lease-label").textContent = "Taking a turn…";
  const result = await postJson("/api/v1/demo/leases");
  if (result.ok) ownLease(result.body);
  else {
    hasLease = false;
    document.getElementById("lease-label").textContent =
      result.status === 409 ? "Another visitor is using the demo" : "Could not start a turn";
    paintLease();
  }
}

async function syncLease({ acquireIfFree = false } = {}) {
  const result = await requestJson("/api/v1/demo/lease");
  if (result.ok && leaseIsMine(result.body)) {
    ownLease(result.body);
    return;
  }
  if (result.ok && leaseIsOccupied(result.body)) {
    loseLease("Another visitor is using the demo");
    return;
  }
  if (acquireIfFree) await acquireLease();
  else loseLease("The demo is available");
}

async function releaseLease() {
  if (!hasLease) return;
  const result = await requestJson("/api/v1/demo/leases/current", { method: "DELETE" });
  if (result.ok) loseLease("Your turn ended. You can keep watching.");
}

async function syncReadiness() {
  const result = await requestJson("/api/v1/demo/readiness");
  renderReadiness(result.body, result.ok);
}

const quietDashboard = (dashboard) =>
  dashboard &&
  [
    dashboard.pendingWeatherRecommendations,
    dashboard.attractionsNeedingAttention,
    dashboard.publishedGuestAdvisories,
    dashboard.openIncidents,
    dashboard.needsAttention,
  ].every((items) => !Array.isArray(items) || items.length === 0) &&
  !dashboard.activeWeatherWorkflow;

const wait = (milliseconds) => new Promise((resolve) => window.setTimeout(resolve, milliseconds));

async function verifyQuiet() {
  for (let attempt = 0; attempt < 6; attempt += 1) {
    const result = await requestJson("/venueops/api/v1/operator/dashboard");
    if (result.ok && quietDashboard(result.body)) return true;
    await wait(1000);
  }
  return false;
}

async function startOver() {
  if (!hasLease || resetInProgress) return;
  resetInProgress = true;
  const progress = document.getElementById("reset-progress");
  const label = document.getElementById("reset-progress-label");
  const retry = document.getElementById("retry-reset");
  progress.hidden = false;
  retry.hidden = true;
  label.textContent = "Resetting the shared demo…";
  syncRail();
  const result = await postJson("/api/v1/demo/reset");
  if (!result.ok) {
    resetInProgress = false;
    label.textContent = result.status ? `Start over failed (${result.status}).` : "Start over could not reach the demo.";
    retry.hidden = false;
    syncRail();
    return;
  }
  label.textContent = "Clearing this browser and reloading both rooms…";
  try {
    window.sessionStorage.clear();
  } catch {
    // Reset still completes when browser storage is unavailable.
  }
  weatherIntent = null;
  triggeredWeatherPhase = null;
  mangroveWatch = null;
  cueFollow = null;
  hideResetNote();
  remountFrames();
  await refresh();
  label.textContent = "Verifying the quiet opening…";
  const quiet = await verifyQuiet();
  resetInProgress = false;
  if (!quiet) {
    label.textContent = "Reset finished, but the quiet opening could not be verified.";
    retry.hidden = false;
  } else {
    await refresh();
    label.textContent = "Ready. The park is quiet.";
    window.setTimeout(() => {
      progress.hidden = true;
    }, 2500);
  }
  syncRail();
}

document.querySelectorAll(".trigger button").forEach((button) => {
  button.addEventListener("click", () => {
    void fire(button);
  });
});

document.getElementById("tab-guest").addEventListener("click", () => selectRoom("guest"));
document.getElementById("tab-tower").addEventListener("click", () => selectRoom("tower"));
document.getElementById("room-switch").addEventListener("keydown", (event) => {
  if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
  event.preventDefault();
  const next = shownRoom === "guest" ? "tower" : "guest";
  selectRoom(next);
  document.getElementById(next === "guest" ? "tab-guest" : "tab-tower").focus();
});

document.getElementById("take-control").addEventListener("click", () => void acquireLease());
document.getElementById("release-control").addEventListener("click", () => void releaseLease());
document.getElementById("start-over").addEventListener("click", () => {
  document.getElementById("reset-dialog").showModal();
});
document.getElementById("reset-dialog").addEventListener("close", (event) => {
  if (event.currentTarget.returnValue === "confirm") void startOver();
});
document.getElementById("retry-reset").addEventListener("click", () => void startOver());

void syncReadiness();
void syncLease({ acquireIfFree: true });
void refresh();
window.setInterval(() => {
  void refresh();
}, pollMs);
window.setInterval(() => {
  void syncReadiness();
  if (!hasLease) void syncLease();
}, 10000);
leaseTimer = window.setInterval(paintLease, 1000);
window.addEventListener("pagehide", () => {
  if (hasLease) {
    void fetch("/api/v1/demo/leases/current", {
      method: "DELETE",
      credentials: "same-origin",
      keepalive: true,
    });
  }
});
