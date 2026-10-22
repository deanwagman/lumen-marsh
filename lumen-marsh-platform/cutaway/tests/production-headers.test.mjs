import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const platform = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const repo = join(platform, "..");

const productionNginx = readFileSync(join(repo, "venueops-console/docker/nginx.conf"), "utf8");
const framedNginx = readFileSync(join(repo, "venueops-console/docker/nginx.framed.conf"), "utf8");
const compose = readFileSync(join(platform, "compose.yaml"), "utf8");
const override = readFileSync(join(platform, "compose.override.yaml"), "utf8");
const cutawayNginx = readFileSync(join(platform, "cutaway", "nginx.conf"), "utf8");
const cutawayIndex = readFileSync(join(platform, "cutaway", "public", "index.html"), "utf8");
const cutawayScript = readFileSync(join(platform, "cutaway", "public", "cutaway.js"), "utf8");
const demoCompose = readFileSync(join(platform, "compose.demo.yaml"), "utf8");
const caddy = readFileSync(join(platform, "demo", "Caddyfile"), "utf8");
const acceptance = readFileSync(
  join(platform, "scripts", "cutaway-sandbox-acceptance.sh"),
  "utf8",
);

assert.match(productionNginx, /add_header X-Frame-Options "DENY" always;/);
assert.match(productionNginx, /frame-ancestors 'none'/);
assert.equal(productionNginx.split("add_header X-Frame-Options").length - 1, 2);

const framedCsp = framedNginx.split("\n").find((line) => line.includes("Content-Security-Policy"));
assert.ok(framedCsp);
assert.match(framedCsp, /frame-ancestors __CONSOLE_FRAME_ANCESTORS__/);
assert.doesNotMatch(framedCsp, /frame-ancestors 'none'/);
assert.equal(framedNginx.split("add_header X-Frame-Options").length - 1, 1);
assert.match(framedNginx, /location = \/health[\s\S]*X-Frame-Options "DENY"/);

assert.doesNotMatch(compose, /CONSOLE_FRAME_ANCESTORS/);
assert.doesNotMatch(compose, /\n {2}cutaway:/);
assert.match(override, /CONSOLE_FRAME_ANCESTORS/);
assert.match(override, /\n {2}cutaway:/);

assert.match(cutawayNginx, /location \/api\/v1\/demo\//);
assert.match(cutawayNginx, /location \/venueops\//);
assert.match(cutawayNginx, /location \/guest\//);
assert.match(cutawayNginx, /location \/tower\//);
assert.match(cutawayNginx, /frame-ancestors 'none'/);
assert.match(cutawayNginx, /X-Content-Type-Options "nosniff"/);
assert.match(cutawayNginx, /Permissions-Policy/);
assert.doesNotMatch(cutawayNginx, /localhost|127\.0\.0\.1/);
assert.doesNotMatch(cutawayIndex, /localhost|127\.0\.0\.1/);
assert.doesNotMatch(cutawayScript, /localhost|127\.0\.0\.1|local-development-token|Authorization/);

for (const service of [
  "environmental-monitor",
  "park-flow-intelligence",
  "reliability-intelligence",
]) {
  const block = demoCompose.match(new RegExp(`  ${service}:[\\s\\S]*?(?=\\n  [a-z]|$)`))?.[0] ?? "";
  assert.match(block, /ports: !reset \[\]/);
}
const demoVenueOps = demoCompose.match(/  venueops-api:[\s\S]*?(?=\n  [a-z]|$)/)?.[0] ?? "";
assert.match(demoVenueOps, /VENUEOPS_ISSUER_URI: \$\{VENUEOPS_ISSUER_URI:-\$\{DEMO_ORIGIN\}\}/);
assert.match(demoVenueOps, /VENUEOPS_HISTORICAL_SEED: "true"/);
const demoOrigin = "https://demo.example.com";
const cognitoIssuer = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example";
const renderedIssuer = (issuer) => {
  const env = {
    ...process.env,
    DEMO_HOST: "demo.example.com",
    DEMO_ORIGIN: demoOrigin,
    DEMO_SECURITY_MODE: "LOCAL_JWT",
    DEMO_RESET_SECRET: "0123456789abcdef0123456789abcdef",
    DEMO_VENUEOPS_DB_PASSWORD: "venueops-secret",
    DEMO_ENVIRONMENTAL_DB_PASSWORD: "environmental-secret",
    NWS_USER_AGENT: "LumenMarshDemo/1.0 (operator@example.com)",
  };
  if (issuer === undefined) {
    delete env.VENUEOPS_ISSUER_URI;
  } else {
    env.VENUEOPS_ISSUER_URI = issuer;
  }
  const rendered = execFileSync(
    "docker",
    ["compose", "-f", "compose.yaml", "-f", "compose.demo.yaml", "config", "--format", "json"],
    { cwd: platform, encoding: "utf8", env },
  );
  return JSON.parse(rendered).services["venueops-api"].environment.VENUEOPS_ISSUER_URI;
};
assert.equal(renderedIssuer(), demoOrigin);
assert.equal(renderedIssuer(cognitoIssuer), cognitoIssuer);
assert.match(caddy, /\{\$DEMO_HOST\} \{/);
assert.doesNotMatch(caddy, /tls\s+internal/);
assert.match(caddy, /auto_https disable_redirects/);
assert.match(caddy, /http:\/\/\{\$DEMO_HOST\} \{\s+redir \{\$DEMO_ORIGIN\}\{uri\} 308\s+\}/);
const healthHandle = caddy.indexOf("handle /gateway-health");
const cutawayCatchAll = caddy.lastIndexOf("handle {");
assert.ok(healthHandle >= 0 && healthHandle < cutawayCatchAll);
assert.match(caddy.slice(healthHandle, cutawayCatchAll), /respond "ok" 200/);
const gateway = demoCompose.match(/  gateway:[\s\S]*?(?=\nnetworks:)/)?.[0] ?? "";
assert.match(gateway, /DEMO_ORIGIN: \$\{DEMO_ORIGIN:\?set DEMO_ORIGIN\}/);
assert.match(gateway, /DEMO_ACME_EMAIL: \$\{DEMO_ACME_EMAIL:-\}/);
assert.doesNotMatch(gateway, /DEMO_ACME_EMAIL:\?/);
assert.match(gateway, /\/demo\/prepare-gateway\.sh/);
const prepareGateway = readFileSync(join(platform, "demo", "prepare-gateway.sh"), "utf8");
const render = (email) =>
  execFileSync("sh", [join(platform, "demo", "prepare-gateway.sh"), "--render"], {
    encoding: "utf8",
    env: { ...process.env, DEMO_ACME_EMAIL: email, CADDYFILE_SOURCE: join(platform, "demo", "Caddyfile") },
  });
assert.equal(render(""), caddy);
assert.match(render("visit@example.com"), /\temail visit@example.com\n/);
assert.throws(() => render("not an email"));
assert.match(prepareGateway, /DEMO_ACME_EMAIL/);
assert.match(acceptance, /curl_tls=\(-kfsS/);
assert.match(acceptance, /--resolve/);
assert.match(acceptance, /\[\[ "\$gateway_health" == \$'ok\\n200' \]\]/);
assert.match(acceptance, /\[\[ "\$redirect_location" == "\$\{DEMO_ORIGIN\}\/" \]\]/);
assert.match(acceptance, /docker inspect --format '\{\{json \.HostConfig\.PortBindings\}\}'/);
assert.match(acceptance, /bindings not in \(None, \{\}\)/);
assert.doesNotMatch(acceptance, /"\$\{COMPOSE\[@\]\}" port/);

console.log("production console and hosted Cutaway headers stay strict");
