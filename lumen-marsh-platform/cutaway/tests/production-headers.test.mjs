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

console.log("production console headers stay denied");
