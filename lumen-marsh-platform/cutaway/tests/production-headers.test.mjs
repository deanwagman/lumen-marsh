import assert from "node:assert/strict";
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

console.log("production console and hosted Cutaway headers stay strict");
