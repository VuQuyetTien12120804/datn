/**
 * Generate ClinicBooking launcher assets from SVG (no checkerboard transparency).
 * Foreground: white text on transparent — for adaptive icon mask (round/square/squircle).
 * Background: solid navy in ic_launcher_background.xml
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..", "frontendbookingcare", "app", "src", "main", "res");
const nodpi = path.join(root, "drawable-nodpi");

const NAVY = "#1E4FA3";

const foregroundSvg = (size) => `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">
  <text x="50%" y="44%" font-family="Arial, Helvetica, sans-serif" font-weight="700"
        font-size="${Math.round(size * 0.11)}" fill="#FFFFFF" text-anchor="middle" dominant-baseline="middle">Clinic</text>
  <text x="50%" y="58%" font-family="Arial, Helvetica, sans-serif" font-weight="700"
        font-size="${Math.round(size * 0.11)}" fill="#FFFFFF" text-anchor="middle" dominant-baseline="middle">Booking</text>
</svg>`;

const fullIconSvg = (size, radius) => `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">
  <rect width="${size}" height="${size}" rx="${radius}" fill="${NAVY}"/>
  <text x="50%" y="44%" font-family="Arial, Helvetica, sans-serif" font-weight="700"
        font-size="${Math.round(size * 0.11)}" fill="#FFFFFF" text-anchor="middle" dominant-baseline="middle">Clinic</text>
  <text x="50%" y="58%" font-family="Arial, Helvetica, sans-serif" font-weight="700"
        font-size="${Math.round(size * 0.11)}" fill="#FFFFFF" text-anchor="middle" dominant-baseline="middle">Booking</text>
</svg>`;

const densities = [
  { folder: "mipmap-mdpi", size: 48 },
  { folder: "mipmap-hdpi", size: 72 },
  { folder: "mipmap-xhdpi", size: 96 },
  { folder: "mipmap-xxhdpi", size: 144 },
  { folder: "mipmap-xxxhdpi", size: 192 },
];

async function main() {
  const sharp = (await import("sharp")).default;
  fs.mkdirSync(nodpi, { recursive: true });

  // Adaptive foreground: 432px, transparent + white text only
  await sharp(Buffer.from(foregroundSvg(432)))
    .png({ compressionLevel: 9, force: true })
    .toFile(path.join(nodpi, "ic_launcher_foreground_img.png"));

  // In-app logo: 192px squircle
  await sharp(Buffer.from(fullIconSvg(192, 42)))
    .png({ compressionLevel: 9 })
    .toFile(path.join(nodpi, "ic_launcher_logo.png"));

  // Splash center icon: 288px transparent + text
  await sharp(Buffer.from(foregroundSvg(288)))
    .png({ compressionLevel: 9 })
    .toFile(path.join(nodpi, "ic_splash_icon_img.png"));

  for (const { folder, size } of densities) {
    const dir = path.join(root, folder);
    fs.mkdirSync(dir, { recursive: true });
    const r = Math.round(size * 0.22);
    await sharp(Buffer.from(fullIconSvg(size, r)))
      .png({ compressionLevel: 9 })
      .toFile(path.join(dir, "ic_launcher.png"));
    await sharp(Buffer.from(fullIconSvg(size, r)))
      .png({ compressionLevel: 9 })
      .toFile(path.join(dir, "ic_launcher_round.png"));
  }

  console.log("Generated launcher icons OK");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
