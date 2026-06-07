import { copyFile, mkdir } from "node:fs/promises";
import { dirname, resolve } from "node:path";

const source = resolve("node_modules/lucide-static/sprite.svg");
const target = resolve("app/web/src/main/resources/static/vendor/lucide/sprite.svg");

await mkdir(dirname(target), { recursive: true });
await copyFile(source, target);
