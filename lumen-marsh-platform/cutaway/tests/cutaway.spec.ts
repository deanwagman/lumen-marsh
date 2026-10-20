import { expect, test, type Page, type Route } from "@playwright/test";

type State = {
  occupied: boolean;
  mine: boolean;
  core: boolean;
  producers: Record<string, boolean>;
  dashboard: Record<string, unknown>;
  calls: { method: string; path: string; authorization?: string }[];
  resetStatus: number;
};

const emptyDashboard = () => ({
  pendingWeatherRecommendations: [],
  activeWeatherWorkflow: null,
  attractionsNeedingAttention: [],
  publishedGuestAdvisories: [],
  openIncidents: [],
  needsAttention: [],
});

const newState = (): State => ({
  occupied: false,
  mine: false,
  core: true,
  producers: { weather: true, reliability: true, flow: true },
  dashboard: emptyDashboard(),
  calls: [],
  resetStatus: 200,
});

const json = (route: Route, status: number, body: unknown) =>
  route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });

async function openBooth(page: Page, state = newState()) {
  await page.route("**/guest/**", (route) =>
    route.fulfill({ contentType: "text/html", body: "<h1>Today</h1>" }),
  );
  await page.route("**/tower/**", (route) =>
    route.fulfill({ contentType: "text/html", body: "<h1>Operations dashboard</h1>" }),
  );
  await page.route("**/api/v1/demo/readiness", (route) =>
    json(route, 200, {
      ready: state.core,
      components: [
        { name: "VenueOps", state: state.core ? "READY" : "UNAVAILABLE", core: true, scenario: null },
        { name: "Weather", state: state.producers.weather ? "READY" : "UNAVAILABLE", core: false, scenario: "weather" },
        { name: "Reliability", state: state.producers.reliability ? "READY" : "UNAVAILABLE", core: false, scenario: "reliability" },
        { name: "Flow", state: state.producers.flow ? "READY" : "UNAVAILABLE", core: false, scenario: "flow" },
      ],
    }),
  );
  await page.route("**/api/v1/demo/lease", (route) =>
    json(route, 200, state.mine
      ? { state: "ACTIVE", holder: true, available: false, remainingSeconds: 300 }
      : { state: state.occupied ? "ACTIVE" : "AVAILABLE", holder: false, available: !state.occupied, remainingSeconds: 0 }),
  );
  await page.route("**/api/v1/demo/leases", async (route) => {
    state.calls.push({ method: route.request().method(), path: new URL(route.request().url()).pathname });
    if (state.occupied && !state.mine) return json(route, 409, { detail: "busy" });
    state.mine = true;
    state.occupied = true;
    return json(route, 201, { state: "ACTIVE", holder: true, remainingSeconds: 300 });
  });
  await page.route("**/api/v1/demo/leases/heartbeat", (route) =>
    json(route, state.mine ? 200 : 409, { state: "ACTIVE", remainingSeconds: 300 }),
  );
  await page.route("**/api/v1/demo/leases/current", (route) => {
    state.calls.push({ method: route.request().method(), path: new URL(route.request().url()).pathname });
    state.mine = false;
    state.occupied = false;
    return route.fulfill({ status: 204 });
  });
  await page.route("**/api/v1/demo/scenarios/*", (route) => {
    state.calls.push({
      method: route.request().method(),
      path: new URL(route.request().url()).pathname,
      authorization: route.request().headers().authorization,
    });
    return json(route, 200, {});
  });
  await page.route("**/api/v1/demo/reset", async (route) => {
    state.calls.push({ method: route.request().method(), path: new URL(route.request().url()).pathname });
    return json(route, state.resetStatus, state.resetStatus === 200 ? { reset: true } : { detail: "failed" });
  });
  await page.route("**/venueops/api/v1/operator/dashboard", (route) => {
    state.calls.push({
      method: route.request().method(),
      path: new URL(route.request().url()).pathname,
      authorization: route.request().headers().authorization,
    });
    return json(route, 200, state.dashboard);
  });
  await page.route("**/venueops/api/v1/advisories", (route) => json(route, 200, []));
  await page.route("**/venueops/api/v1/flow/recommendations", (route) => json(route, 200, []));
  await page.goto("/");
  await expect(page.locator("#lease-label")).toHaveText("You control the demo");
  return state;
}

test("acquires a timed visitor turn and uses only same-origin hosted routes", async ({ page }) => {
  const state = await openBooth(page);
  await expect(page.locator("#lease-countdown")).toHaveText(/4:5\d|5:00/);
  await expect(page.locator("#readiness")).toContainText("Core ready");
  await expect(page.locator("#guest-frame")).toHaveAttribute("src", "/guest/today");
  await expect(page.locator("#console-frame")).toHaveAttribute("src", "/tower/dashboard");

  await page.getByRole("button", { name: "Lightning hold" }).click();
  await expect(page.locator("[data-result='lightning-hold']")).toHaveText("Fired");
  expect(state.calls).toContainEqual(expect.objectContaining({
    method: "POST",
    path: "/api/v1/demo/scenarios/lightning-hold",
  }));
  expect(state.calls.every((call) => call.authorization === undefined)).toBe(true);
  expect(await page.locator("body").innerHTML()).not.toContain("localhost");
  expect(await page.locator("body").innerHTML()).not.toContain("local-development-token");

  await page.getByRole("button", { name: "End turn" }).click();
  await expect(page.locator("#lightning-hold")).toBeDisabled();
  expect(state.calls).toContainEqual({ method: "DELETE", path: "/api/v1/demo/leases/current" });
});

test("spectators can watch but cannot mutate the shared demo", async ({ page }) => {
  const state = newState();
  state.occupied = true;
  await page.route("**/guest/**", (route) => route.fulfill({ body: "<h1>Today</h1>", contentType: "text/html" }));
  await page.route("**/tower/**", (route) => route.fulfill({ body: "<h1>Tower</h1>", contentType: "text/html" }));
  await page.route("**/api/v1/demo/readiness", (route) => json(route, 200, { core: true }));
  await page.route("**/api/v1/demo/lease", (route) => json(route, 200, { active: true, available: false }));
  await page.route("**/venueops/api/v1/operator/dashboard", (route) => json(route, 200, emptyDashboard()));
  await page.route("**/venueops/api/v1/advisories", (route) => json(route, 200, []));
  await page.route("**/venueops/api/v1/flow/recommendations", (route) => json(route, 200, []));
  await page.goto("/");

  await expect(page.locator("#lease-label")).toContainText(/another visitor/i);
  await expect(page.getByRole("button", { name: "Lightning hold" })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Start over" })).toBeDisabled();
  await expect(page.locator("#guest-frame")).toBeVisible();
});

test("a producer outage disables only its matching scenarios", async ({ page }) => {
  const state = newState();
  state.producers.reliability = false;
  await openBooth(page, state);

  await page.locator("#explore summary").click();
  await expect(page.getByRole("button", { name: "Cypress Coil vibration" })).toBeDisabled();
  await expect(page.locator("[data-result='cypress-vibration']")).toHaveText("Unavailable");
  await expect(page.getByRole("button", { name: "Mangrove disruption" })).toBeEnabled();
  await expect(page.getByRole("button", { name: "Lightning hold" })).toBeEnabled();
  await expect(page.locator("#booth-offline")).toBeHidden();
});

test("a core outage covers both rooms and disables the booth", async ({ page }) => {
  const state = newState();
  state.core = false;
  await openBooth(page, state);

  await expect(page.locator("#booth-offline")).toBeVisible();
  await expect(page.getByRole("button", { name: "Lightning hold" })).toBeDisabled();
  await expect(page.locator("#readiness")).toContainText("Core unavailable");
});

test("weather decisions stay operator-only until the guest projection is published", async ({ page }) => {
  const state = newState();
  state.dashboard = {
    ...emptyDashboard(),
    pendingWeatherRecommendations: [{
      id: "wx-1",
      action: "PLACE_WEATHER_HOLD",
      recommendedAction: "Hold outdoor attractions",
      affectedAttractionIds: ["cypress-coil", "mangrove-run"],
      updatedAt: "2026-10-20T00:00:00Z",
    }],
    activeWeatherWorkflow: {
      recommendationId: "wx-1",
      action: "PLACE_WEATHER_HOLD",
      operatorStatus: "PENDING",
      affectedAttractionIds: ["cypress-coil", "mangrove-run"],
    },
  };
  await openBooth(page, state);

  await expect(page.locator("#pending-weather")).toHaveText("Hold outdoor attractions");
  await expect(page.locator("#guest-advisory")).toHaveText("Quiet");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
  await expect(page.locator("#opening")).toHaveAttribute("data-beat", "operator");
  await expect(page.locator("#tower-room")).toHaveClass(/is-cued/);
  await expect(page.locator("#guest-room")).not.toHaveClass(/is-cued/);
});

test("Start over is confirmed, reports progress, clears state, and remounts both rooms", async ({ page }) => {
  const state = await openBooth(page);
  await page.evaluate(() => sessionStorage.setItem("cutaway.weather-intent.v1", "stale"));
  const guestBefore = await page.locator("#guest-frame").getAttribute("src");
  const towerBefore = await page.locator("#console-frame").getAttribute("src");

  await page.getByRole("button", { name: "Start over" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.getByRole("dialog")).toContainText("shared demo");
  await page.getByRole("dialog").getByRole("button", { name: "Start over" }).click();
  await expect(page.locator("#reset-progress-label")).toHaveText("Ready. The park is quiet.");

  expect(state.calls).toContainEqual({ method: "POST", path: "/api/v1/demo/reset" });
  expect(await page.evaluate(() => sessionStorage.getItem("cutaway.weather-intent.v1"))).toBeNull();
  await expect(page.locator("#guest-frame")).not.toHaveAttribute("src", guestBefore!);
  await expect(page.locator("#console-frame")).not.toHaveAttribute("src", towerBefore!);
  await expect(page.locator("#opening")).toHaveAttribute("data-beat", "quiet");
});

test("failed Start over offers a retry without remounting rooms", async ({ page }) => {
  const state = newState();
  state.resetStatus = 503;
  await openBooth(page, state);
  const guestBefore = await page.locator("#guest-frame").getAttribute("src");

  await page.getByRole("button", { name: "Start over" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Start over" }).click();
  await expect(page.locator("#reset-progress-label")).toHaveText("Start over failed (503).");
  await expect(page.getByRole("button", { name: "Retry" })).toBeVisible();
  await expect(page.locator("#guest-frame")).toHaveAttribute("src", guestBefore!);
});

test("responsive visitors keep lease controls, readiness, rooms, and Start over usable", async ({ page }) => {
  await openBooth(page);
  await page.setViewportSize({ width: 390, height: 844 });

  await expect(page.locator("#lease-label")).toBeVisible();
  await expect(page.locator("#readiness")).toBeVisible();
  await expect(page.getByRole("button", { name: "Start over" })).toBeVisible();
  await expect(page.getByRole("tab", { name: "Guest" })).toBeVisible();
  await expect(page.locator("#guest-room")).toBeVisible();
  await page.getByRole("tab", { name: "Control Tower" }).click();
  await expect(page.locator("#tower-room")).toBeVisible();
  await expect(page.locator("#guest-room")).toBeHidden();
});

test("in-app docs explain hosted turns, readiness, and reset recovery", async ({ page }) => {
  await page.goto("/docs.html");
  await expect(page.getByRole("heading", { name: "Using Cutaway" })).toBeVisible();
  const guide = page.locator(".guide");
  await expect(guide).toContainText("Turns and Start over");
  await expect(guide).toContainText("spectator");
  await expect(guide).toContainText("compact readiness");
  await expect(guide).toContainText("use Retry");
  await expect(guide).not.toContainText("localhost");
  await expect(guide).not.toContainText("local-development-token");
});

test("captures hosted sandbox lease, spectator, and reset evidence", async ({ page }) => {
  const evidenceDir = process.env.CUTAWAY_HOSTED_EVIDENCE_DIR;
  test.skip(!evidenceDir, "Set CUTAWAY_HOSTED_EVIDENCE_DIR to capture hosted sandbox evidence.");
  const state = await openBooth(page);
  await page.setViewportSize({ width: 1440, height: 1100 });
  await page.screenshot({ path: `${evidenceDir}/hosted-sandbox-lease.png`, fullPage: true });

  state.mine = false;
  state.occupied = true;
  await page.reload();
  await expect(page.locator("#lease-label")).toContainText(/another visitor/i);
  await page.screenshot({ path: `${evidenceDir}/hosted-sandbox-spectator.png`, fullPage: true });

  state.mine = true;
  await page.reload();
  await page.getByRole("button", { name: "Start over" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Start over" }).click();
  await expect(page.locator("#reset-progress-label")).toHaveText("Ready. The park is quiet.");
  await page.screenshot({ path: `${evidenceDir}/hosted-sandbox-reset.png`, fullPage: true });
});
