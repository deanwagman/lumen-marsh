import { expect, type Page, type Route } from "@playwright/test";
import { test } from "@playwright/test";

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-methods": "GET, POST, OPTIONS",
  "access-control-allow-headers": "authorization, content-type, accept",
};

const scenarios = {
  "lightning-hold": "http://localhost:8000/api/v1/simulation/scenarios/hold-conditions",
  "clear-storm": "http://localhost:8000/api/v1/simulation/scenarios/clearance-period",
  "cypress-vibration": "http://localhost:8200/simulation/scenarios/cypress-coil-vibration",
  "mangrove-disruption": "http://localhost:8100/simulation/scenarios/mangrove-disruption",
} as const;

type Call = { url: string; method: string; body: string | null };

type BoothState = {
  dashboard: Record<string, unknown> | null;
  advisories: unknown[] | null;
  guidance: unknown[] | null;
  recommendationStatus: string | null;
  failStatus: number | null;
  calls: Call[];
  dashboardAuth: string;
};

const emptyDashboard = () => ({
  pendingWeatherRecommendations: [],
  needsAttention: [],
});

const newState = (): BoothState => ({
  dashboard: emptyDashboard(),
  advisories: [],
  guidance: [],
  recommendationStatus: null,
  failStatus: null,
  calls: [],
  dashboardAuth: "",
});

async function fulfillJson(route: Route, status: number, body: unknown) {
  if (route.request().method() === "OPTIONS") {
    await route.fulfill({ status: 204, headers: cors });
    return;
  }
  await route.fulfill({
    status,
    contentType: "application/json",
    headers: cors,
    body: JSON.stringify(body),
  });
}

async function installBooth(page: Page, state: BoothState) {
  await page.route("http://localhost:3000/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<!doctype html><html><body><h1>Today</h1></body></html>",
    }),
  );
  await page.route("http://localhost:3001/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: `<!doctype html><html><head><style>
        html, body { margin: 0; }
        .notice { display: none; }
        @media (max-width: 52.5rem) { .notice { display: block; } }
        .above { height: 40rem; }
      </style></head><body>
        <h1>Operations dashboard</h1>
        <p class="notice">Designed for operator workstations. This layout is compacted for a narrower display.</p>
        <div class="above"></div>
        <section id="needs-attention"><h2>Needs attention</h2></section>
      </body></html>`,
    }),
  );

  await page.route("http://localhost:8080/**", async (route) => {
    const url = route.request().url();
    if (url === "http://localhost:8080/api/v1/operator/dashboard") {
      if (route.request().method() === "GET") {
        state.dashboardAuth = route.request().headers().authorization ?? "";
      }
      await fulfillJson(route, state.dashboard ? 200 : 500, state.dashboard);
      return;
    }
    if (url === "http://localhost:8080/api/v1/advisories") {
      await fulfillJson(route, state.advisories ? 200 : 500, state.advisories);
      return;
    }
    if (url === "http://localhost:8080/api/v1/flow/recommendations") {
      await fulfillJson(route, state.guidance ? 200 : 500, state.guidance);
      return;
    }
    const recommendation = url.match(
      /^http:\/\/localhost:8080\/api\/v1\/operator\/flow\/recommendations\/([^/]+)$/,
    );
    if (recommendation) {
      if (!state.recommendationStatus) {
        await fulfillJson(route, 404, { detail: "missing" });
        return;
      }
      await fulfillJson(route, 200, {
        recommendationId: recommendation[1],
        recommendation: { status: state.recommendationStatus, summary: "Mangrove Run" },
      });
      return;
    }
    await fulfillJson(route, 404, {});
  });

  for (const url of Object.values(scenarios)) {
    await page.route(url, async (route) => {
      if (route.request().method() === "OPTIONS") {
        await route.fulfill({ status: 204, headers: cors });
        return;
      }
      state.calls.push({
        url: route.request().url(),
        method: route.request().method(),
        body: route.request().postData(),
      });
      if (state.failStatus) {
        await fulfillJson(route, state.failStatus, { detail: "no" });
        return;
      }
      const recommendationId = url.endsWith("/mangrove-disruption") ? "rec-mangrove" : null;
      await fulfillJson(route, 200, {
        scenario: url.split("/").pop(),
        ...(recommendationId ? { recommendationId } : {}),
      });
    });
  }
}

test.beforeEach(async ({ page }) => {
  const state = newState();
  await installBooth(page, state);
  await page.goto("/");
  await expect(page.locator("#last-recommendation")).toHaveText("None");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
  (page as Page & { booth?: BoothState }).booth = state;
});

const booth = (page: Page) => (page as Page & { booth: BoothState }).booth;

const frameUrl = (page: Page, port: string) =>
  page.frames().find((frame) => frame.url().includes(`:${port}`))?.url() ?? "";

test("docs view covers how to use the booth", async ({ page }) => {
  await page.getByRole("link", { name: "Docs" }).click();
  await expect(page).toHaveURL(/\/docs\.html$/);
  await expect(page.getByRole("heading", { level: 1, name: "Using Cutaway" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "On this page" })).toBeVisible();

  const guide = page.locator(".guide-article");
  for (const name of [
    "Opening",
    "What Cutaway is",
    "How to open it",
    "What each rail button does",
    "What the operator still clicks",
    "What the guest must not see until publish",
  ]) {
    await expect(guide.getByRole("heading", { level: 2, name })).toBeVisible();
  }

  const opening = page.locator("#opening");
  await expect(opening).toContainText("Start here");
  await expect(opening).toContainText("press Lightning hold");
  await expect(opening).toContainText(
    "Control Tower will get a decision, and the guest room will not, until someone publishes",
  );
  await expect(opening).toContainText("look right. That row is the ask. Leave it unpublished, then look left");
  await expect(opening).toContainText("the ride still looks open. That is the published picture");
  await expect(opening).toContainText("review the weather recommendation and publish the guest advisory");
  await expect(opening).toContainText("sign in with your operator account");
  await expect(opening).toContainText("press Clear the storm");
  await expect(opening).toContainText("The strip is the seam. Pending on one side, published on the other.");

  const guideText = await page.locator(".guide").innerText();
  await expect(page.locator("#rail-buttons")).toContainText("Lightning hold");
  await expect(page.locator("#rail-buttons")).toContainText("Clear the storm");
  await expect(page.locator("#rail-buttons")).toContainText("Cypress Coil vibration");
  await expect(page.locator("#rail-buttons")).toContainText("Mangrove disruption");
  await expect(page.locator("#rail-buttons")).toContainText("pending weather");
  await expect(page.locator("#how-to-open")).toContainText("Choose Cutaway in the header");
  await expect(page.locator("#guest-must-not-see")).toContainText("until a supervisor publishes");
  await expect(page.locator("#operator-clicks")).toContainText("Control Tower");
  for (const leaked of [
    ".env",
    "NWS_USER_AGENT",
    "POST http",
    "compose.yaml",
    "dart-define",
    "hold-conditions",
    "frame-ancestors",
    "dev-up.sh",
    "localhost:8080",
  ]) {
    expect(guideText).not.toContain(leaked);
  }
  for (const name of ["Place weather hold", "Accept", "Dismiss", "Publish", "Confirm command"]) {
    await expect(page.getByRole("button", { name, exact: true })).toHaveCount(0);
  }

  await page.getByRole("link", { name: "Cutaway", exact: true }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("heading", { level: 1, name: "Cutaway", exact: true })).toBeVisible();
  await expect(page.locator("#guest-frame")).toBeVisible();
});

const holdWeather = () => ({
  pendingWeatherRecommendations: [
    {
      id: "wx-1",
      recommendedAction: "Hold outdoor attractions",
      updatedAt: "2026-10-15T18:00:00Z",
    },
  ],
  needsAttention: [],
});

test("quiet park points the cue at Lightning hold", async ({ page }) => {
  const opening = page.locator("#opening");
  await expect(opening).toBeVisible();
  await expect(opening).toHaveAttribute("data-beat", "quiet");
  await expect(page.locator("#opening-cue")).toHaveText("Press Lightning hold.");
  await expect(page.locator("#opening-detail")).toHaveText(
    "Control Tower will get a decision, and the guest room will not, until someone publishes.",
  );
  await expect(page.locator("#opening-seam")).toHaveText(
    "The strip is the seam. Pending on one side, published on the other.",
  );
  await expect(page.locator("#opening-kicker")).toHaveText("Start here");
  await expect(page.locator("#lightning-start")).toBeVisible();
  await expect(page.locator("#lightning-start")).toHaveText("Start here");
  await expect(page.locator("#trigger-lightning-hold")).toHaveClass(/is-cued/);
  for (const id of ["trigger-clear-storm", "trigger-cypress-vibration", "trigger-mangrove-disruption"]) {
    await expect(page.locator(`#${id}`)).not.toHaveClass(/is-cued/);
    await expect(page.locator(`#${id} .start-here`)).toHaveCount(0);
  }
  await expect(page.locator("#guest-room")).not.toHaveClass(/is-cued/);
  await expect(page.locator("#tower-room")).not.toHaveClass(/is-cued/);
  await expect(page.getByRole("button", { name: "Next", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Publish", exact: true })).toHaveCount(0);
});

test("a hold points the cue at Control Tower while the guest picture stays unpublished", async ({ page }) => {
  const state = booth(page);
  state.dashboard = holdWeather();

  await page.getByRole("button", { name: "Lightning hold" }).click();
  await expect(page.locator("#pending-weather")).toHaveText("Hold outdoor attractions");
  await expect(page.locator("#guest-advisory")).toHaveText("Quiet");
  await expect(page.locator("#guest-best-next")).toHaveText("Quiet");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
  await expect(page.locator("#opening")).toHaveAttribute("data-beat", "operator");
  await expect(page.locator("#opening-cue")).toHaveText("Look right. That row is the ask.");
  await expect(page.locator("#opening-detail")).toHaveText("Leave it unpublished, then look left.");
  await expect(page.locator("#tower-room")).toHaveClass(/is-cued/);
  await expect(page.locator("#strip-operator")).toHaveClass(/is-cued/);
  await expect(page.locator("#guest-room")).not.toHaveClass(/is-cued/);
  await expect(page.locator("#strip-guest")).not.toHaveClass(/is-cued/);
  await expect(page.locator("#lightning-start")).toBeHidden();
  await expect(page.locator("#trigger-lightning-hold")).not.toHaveClass(/is-cued/);
  await expect(page.getByRole("button", { name: "Publish", exact: true })).toHaveCount(0);

  state.advisories = [{ title: "Weather hold", message: "Outdoor attractions are paused." }];
  await expect(page.locator("#guest-advisory")).toHaveText("Weather hold: Outdoor attractions are paused.", {
    timeout: 6000,
  });
  await expect(page.locator("#guest-projection")).toHaveText("Changed: advisory");
  await expect(page.locator("#opening")).toHaveAttribute("data-beat", "clear");
  await expect(page.locator("#opening-cue")).toHaveText("Press Clear the storm.");
  await expect(page.locator("#trigger-clear-storm")).toHaveClass(/is-cued/);
  await expect(page.locator("#tower-room")).not.toHaveClass(/is-cued/);
  await expect(page.locator("#guest-room")).not.toHaveClass(/is-cued/);
});

test("an unpublished hold walks from Control Tower to the guest room without a Next button", async ({ page }) => {
  test.setTimeout(20_000);
  const state = booth(page);
  state.dashboard = holdWeather();
  await page.reload();
  await expect(page.locator("#opening")).toHaveAttribute("data-beat", "operator");
  await expect(page.locator("#lightning-start")).toBeHidden();
  await expect(page.getByRole("button", { name: "Next", exact: true })).toHaveCount(0);

  await expect(page.locator("#opening")).toHaveAttribute("data-beat", "guest", { timeout: 8000 });
  await expect(page.locator("#opening-cue")).toHaveText("The ride still looks open.");
  await expect(page.locator("#opening-detail")).toHaveText("That is the published picture.");
  await expect(page.locator("#guest-room")).toHaveClass(/is-cued/);
  await expect(page.locator("#strip-guest")).toHaveClass(/is-cued/);
  await expect(page.locator("#tower-room")).not.toHaveClass(/is-cued/);
  await expect(page.locator("#strip-operator")).not.toHaveClass(/is-cued/);
  await expect(page.locator("#guest-advisory")).toHaveText("Quiet");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");

  await expect(page.locator("#opening")).toHaveAttribute("data-beat", "publish", { timeout: 8000 });
  await expect(page.locator("#opening-cue")).toHaveText(
    "In Control Tower, review the weather recommendation and publish the guest advisory.",
  );
  await expect(page.locator("#opening-detail")).toContainText("VenueOps sign in");
  await expect(page.locator("#opening-detail")).toContainText("sign in with your operator account to open the console");
  await expect(page.locator("#tower-room")).toHaveClass(/is-cued/);
  await expect(page.locator("#guest-room")).not.toHaveClass(/is-cued/);
  await expect(page.locator("#guest-advisory")).toHaveText("Quiet");
  await expect(page.getByRole("button", { name: "Publish", exact: true })).toHaveCount(0);
});

test("shell shows both frames, the rail, and the strip", async ({ page }) => {
  await expect(page.getByRole("heading", { level: 1, name: "Cutaway" })).toBeVisible();
  const rail = page.getByRole("complementary", { name: "Producer triggers" });
  await expect(page.getByRole("link", { name: "Docs" })).toBeVisible();
  await expect(rail.getByRole("link", { name: "Docs" })).toHaveCount(0);
  await expect(rail).toBeVisible();
  await expect(rail.locator("iframe")).toHaveCount(0);
  await expect(rail.getByRole("button")).toHaveCount(4);
  await expect(page.locator("iframe")).toHaveCount(2);

  const guest = page.locator("#guest-frame");
  const tower = page.locator("#console-frame");
  await expect(guest).toHaveAttribute("src", "http://localhost:3000/today");
  await expect(guest).toHaveAttribute("title", "Guest app, Today");
  await expect(tower).toHaveAttribute("src", "http://localhost:3001/dashboard");
  await expect(tower).toHaveAttribute("title", "Control Tower");
  await expect(page.frameLocator("#guest-frame").getByRole("heading", { name: "Today" })).toBeVisible();
  await expect(
    page.frameLocator("#console-frame").getByRole("heading", { name: "Operations dashboard" }),
  ).toBeVisible();

  await expect(page.getByRole("button", { name: "Lightning hold" })).toBeEnabled();
  await expect(page.getByRole("region", { name: "VenueOps projection" })).toBeVisible();
  await expect(page.locator("#booth-offline")).toBeHidden();
  for (const name of ["Place weather hold", "Accept", "Dismiss", "Publish", "Confirm command"]) {
    await expect(page.getByRole("button", { name, exact: true })).toHaveCount(0);
  }
});

test("rail buttons post the four simulation calls", async ({ page }) => {
  const state = booth(page);
  const buttons = [
    ["Lightning hold", "lightning-hold"],
    ["Clear the storm", "clear-storm"],
    ["Cypress Coil vibration", "cypress-vibration"],
    ["Mangrove disruption", "mangrove-disruption"],
  ] as const;

  for (const [name, id] of buttons) {
    await page.getByRole("button", { name }).click();
    await expect(page.locator(`[data-result="${id}"]`)).toHaveText("Fired");
    const call = state.calls.at(-1);
    expect(call?.url).toBe(scenarios[id]);
    expect(call?.method).toBe("POST");
    expect(call?.body).toBe("{}");
  }
  expect(state.dashboardAuth).toBe("Bearer local-development-token");
});

test("lightning shows pending weather while the guest stays quiet", async ({ page }) => {
  const state = booth(page);
  state.dashboard = {
    pendingWeatherRecommendations: [
      {
        id: "wx-1",
        recommendedAction: "Hold outdoor attractions",
        updatedAt: "2026-10-15T18:00:00Z",
        evidence: "Lightning within 8 miles",
      },
    ],
    needsAttention: [],
  };

  await page.getByRole("button", { name: "Lightning hold" }).click();
  await expect(page.locator("[data-result='lightning-hold']")).toHaveText("Fired");
  await expect(page.locator("#pending-weather")).toHaveText("Hold outdoor attractions");
  await expect(page.locator("#last-recommendation")).toHaveText("Pending weather: Hold outdoor attractions");
  await expect(page.locator("#guest-advisory")).toHaveText("Quiet");
  await expect(page.locator("#guest-best-next")).toHaveText("Quiet");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
});

test("vibration shows pending reliability while the guest stays quiet", async ({ page }) => {
  const state = booth(page);
  state.dashboard = {
    pendingWeatherRecommendations: [],
    needsAttention: [
      {
        kind: "PENDING_RELIABILITY_RECOMMENDATION",
        subjectLabel: "Vibration",
        reason: "Pending reliability recommendation: Inspect the wheel",
        updatedAt: "2026-10-15T18:05:00Z",
      },
    ],
  };

  await page.getByRole("button", { name: "Cypress Coil vibration" }).click();
  await expect(page.locator("[data-result='cypress-vibration']")).toHaveText("Fired");
  await expect(page.locator("#pending-reliability")).toHaveText("Vibration");
  await expect(page.locator("#last-recommendation")).toHaveText("Pending reliability: Vibration");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
  await expect(page.locator("#strip")).not.toContainText("Inspect the wheel");
});

test("mangrove disruption shows unpublished flow while Best Next stays quiet", async ({ page }) => {
  const state = booth(page);
  state.recommendationStatus = "PENDING_REVIEW";
  state.dashboard = {
    pendingWeatherRecommendations: [],
    needsAttention: [
      {
        kind: "UNPUBLISHED_FLOW_RECOMMENDATION",
        subjectLabel: "Mangrove Run boarding is down",
        reason: "Unpublished flow recommendation: operator explanation",
        updatedAt: "2026-10-15T18:06:00Z",
      },
    ],
  };
  await page.getByRole("button", { name: "Mangrove disruption" }).click();
  await expect(page.locator("[data-result='mangrove-disruption']")).toHaveText("Fired");
  await expect(page.locator("#unpublished-flow")).toHaveText("Mangrove Run boarding is down");
  await expect(page.locator("#last-recommendation")).toHaveText(
    "Unpublished flow: Mangrove Run boarding is down",
  );
  await expect(page.locator("#guest-best-next")).toHaveText("Quiet");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
  await expect(page.locator("#reset-note")).toBeHidden();
  await expect(page.locator("#strip")).not.toContainText("operator explanation");
});

test("published advisory and Best Next mark the guest projection changed", async ({ page }) => {
  const state = booth(page);
  state.advisories = [
    {
      title: "Weather hold",
      message: "Outdoor attractions are paused.",
      explanation: "INTERNAL ONLY",
    },
  ];
  state.guidance = [
    {
      guestMessage: "Mangrove Run is temporarily unavailable.",
      explanation: "queue model",
      confidence: 0.8,
      queueLength: 12,
    },
  ];

  await page.getByRole("button", { name: "Lightning hold" }).click();
  await expect(page.locator("#guest-advisory")).toHaveText("Weather hold: Outdoor attractions are paused.");
  await expect(page.locator("#guest-best-next")).toHaveText("Mangrove Run is temporarily unavailable.");
  await expect(page.locator("#guest-projection")).toHaveText("Changed: advisory and Best Next");
  await expect(page.locator("#strip")).not.toContainText("INTERNAL ONLY");
  await expect(page.locator("#strip")).not.toContainText("queue model");
  await expect(page.locator("#strip")).not.toContainText("queueLength");
});

test("a spent mangrove disruption shows the reset note", async ({ page }) => {
  const state = booth(page);
  state.recommendationStatus = "DISMISSED";

  await page.getByRole("button", { name: "Mangrove disruption" }).click();
  await expect(page.locator("#reset-note")).toBeVisible();
  await expect(page.locator("#reset-note")).toContainText("./scripts/reset-demo.sh --confirm");
  await expect(page.getByRole("button", { name: /reset/i })).toHaveCount(0);
});

test("a failed simulation call shows failed", async ({ page }) => {
  const state = booth(page);
  state.failStatus = 503;

  await page.getByRole("button", { name: "Clear the storm" }).click();
  await expect(page.locator("[data-result='clear-storm']")).toHaveText("Failed (503)");
  expect(state.calls[0]?.body).toBe("{}");
});

test("an unread guest payload is not described as quiet", async ({ page }) => {
  const state = booth(page);
  state.advisories = null;
  state.guidance = null;

  await page.getByRole("button", { name: "Lightning hold" }).click();
  await expect(page.locator("#guest-advisory")).toHaveText("Unread");
  await expect(page.locator("#guest-projection")).toHaveText("Unread");
  await expect(page.locator("#booth-offline")).toBeHidden();
  await expect(page.getByRole("button", { name: "Lightning hold" })).toBeEnabled();
});

test("producer cues reveal the matching tower card and keep Today", async ({ page }) => {
  const state = booth(page);
  const guest = page.locator("#guest-frame");

  await guest.evaluate((frame: HTMLIFrameElement) => {
    frame.src = "http://localhost:3000/field-guide";
  });
  await expect.poll(() => frameUrl(page, "3000")).toContain("/field-guide");
  state.dashboard = {
    pendingWeatherRecommendations: [
      { id: "wx-1", recommendedAction: "Hold outdoor attractions", updatedAt: "2026-10-15T18:00:00Z" },
    ],
    needsAttention: [],
  };
  await page.getByRole("button", { name: "Lightning hold" }).click();
  await expect.poll(() => frameUrl(page, "3000")).toBe("http://localhost:3000/today");
  await expect.poll(() => frameUrl(page, "3001")).toBe("http://localhost:3001/dashboard#weather-card-wx-1");
  await expect(page.locator("#guest-advisory")).toHaveText("Quiet");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");

  await guest.evaluate((frame: HTMLIFrameElement) => {
    frame.src = "http://localhost:3000/attractions";
  });
  await expect.poll(() => frameUrl(page, "3000")).toContain("/attractions");
  state.dashboard = {
    pendingWeatherRecommendations: [],
    needsAttention: [
      {
        kind: "PENDING_RELIABILITY_RECOMMENDATION",
        subjectId: "rec-vib",
        subjectLabel: "Vibration",
        updatedAt: "2026-10-15T18:05:00Z",
      },
    ],
  };
  await page.getByRole("button", { name: "Cypress Coil vibration" }).click();
  await expect.poll(() => frameUrl(page, "3000")).toBe("http://localhost:3000/today");
  await expect
    .poll(() => frameUrl(page, "3001"))
    .toBe("http://localhost:3001/dashboard#attention-card-PENDING_RELIABILITY_RECOMMENDATION-rec-vib");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");

  await guest.evaluate((frame: HTMLIFrameElement) => {
    frame.src = "http://localhost:3000/inbox";
  });
  await expect.poll(() => frameUrl(page, "3000")).toContain("/inbox");
  state.recommendationStatus = "PENDING_REVIEW";
  state.dashboard = {
    pendingWeatherRecommendations: [],
    needsAttention: [
      {
        kind: "UNPUBLISHED_FLOW_RECOMMENDATION",
        subjectId: "rec-mangrove",
        subjectLabel: "Mangrove Run boarding is down",
        updatedAt: "2026-10-15T18:06:00Z",
      },
    ],
  };
  await page.getByRole("button", { name: "Mangrove disruption" }).click();
  await expect.poll(() => frameUrl(page, "3000")).toBe("http://localhost:3000/today");
  await expect
    .poll(() => frameUrl(page, "3001"))
    .toBe("http://localhost:3001/dashboard#attention-card-UNPUBLISHED_FLOW_RECOMMENDATION-rec-mangrove");
  await expect(page.locator("#guest-best-next")).toHaveText("Quiet");
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
});

test("clear the storm leaves both frames where they are", async ({ page }) => {
  const guest = frameUrl(page, "3000");
  const tower = frameUrl(page, "3001");
  await page.getByRole("button", { name: "Clear the storm" }).click();
  await expect(page.locator("[data-result='clear-storm']")).toHaveText("Fired");
  expect(frameUrl(page, "3000")).toBe(guest);
  expect(frameUrl(page, "3001")).toBe(tower);
});

test("control tower is wide enough to leave the compact layout", async ({ page }) => {
  await page.setViewportSize({ width: 2100, height: 1400 });
  const box = await page.locator("#console-frame").boundingBox();
  expect(box?.width).toBeGreaterThan(72 * 16);
  const tower = page.frameLocator("#console-frame");
  await expect(tower.getByText("compacted for a narrower display")).toBeHidden();
  await expect(tower.locator("#needs-attention")).toBeInViewport();
});

test("a down VenueOps stack covers both rooms and disables the rail", async ({ page }) => {
  const state = booth(page);
  state.dashboard = null;
  state.advisories = null;
  state.guidance = null;

  await page.getByRole("button", { name: "Lightning hold" }).click();
  await expect(page.locator("#booth-offline")).toBeVisible();
  await expect(page.locator("#booth-offline")).toContainText("both rooms stay covered");
  await expect(page.locator("#last-recommendation")).toHaveText("VenueOps is offline.");
  await expect(page.locator("#guest-projection")).toHaveText("Offline");
  await expect(page.locator("#guest-advisory")).not.toHaveText("Quiet");
  for (const name of ["Lightning hold", "Clear the storm", "Cypress Coil vibration", "Mangrove disruption"]) {
    await expect(page.getByRole("button", { name })).toBeDisabled();
  }
  await expect(page.frameLocator("#guest-frame").getByRole("heading", { name: "Today" })).toBeHidden();
  await expect(
    page.frameLocator("#console-frame").getByRole("heading", { name: "Operations dashboard" }),
  ).toBeHidden();

  const calls = state.calls.length;
  await page.getByRole("button", { name: "Clear the storm" }).click({ force: true });
  expect(state.calls.length).toBe(calls);

  state.dashboard = emptyDashboard();
  state.advisories = [];
  state.guidance = [];
  await expect(page.locator("#booth-offline")).toBeHidden({ timeout: 6000 });
  await expect(page.getByRole("button", { name: "Lightning hold" })).toBeEnabled();
  await expect(page.frameLocator("#guest-frame").getByRole("heading", { name: "Today" })).toBeVisible();
  await expect(
    page.frameLocator("#console-frame").getByRole("heading", { name: "Operations dashboard" }),
  ).toBeVisible();
  await expect(page.locator("#guest-projection")).toHaveText("Unchanged");
});
