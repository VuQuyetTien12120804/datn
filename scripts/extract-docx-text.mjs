import fs from "fs";
import JSZip from "jszip";

const docxPath = process.argv[2] || "d:/datn/VuQuyetTien_223630716_KHMT_K63_BaoCaoDoAn_KHUNG.docx";
const outPath = process.argv[3] || "d:/datn/.chapter_extract.txt";
const mode = process.argv[4] || "ch1"; // "ch1" | "ch2" | "full"

const buf = fs.readFileSync(docxPath);
const zip = await JSZip.loadAsync(buf);
const xml = await zip.file("word/document.xml").async("string");

const paras = [];
for (const p of xml.split(/<\/w:p>/)) {
  const texts = [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map((m) => m[1]);
  const line = texts.join("").replace(/\s+/g, " ").trim();
  if (line) paras.push(line);
}

let chunk = paras;
if (mode === "ch1" || mode === "ch2") {
  const chapterIdx = (label) => {
    const occ = [];
    paras.forEach((l, i) => {
      if (new RegExp(`^CHƯƠNG\\s*${label}(\\b|\\.|\\s)`, "i").test(l)) occ.push(i);
    });
    return occ;
  };
  const ch1 = chapterIdx(1);
  const ch2 = chapterIdx(2);
  const ch3 = chapterIdx(3);
  if (mode === "ch1") {
    const start = ch1.length >= 2 ? ch1[1] : ch1[0] ?? 0;
    const end = ch2.length >= 2 ? ch2[1] : ch2[0] ?? paras.length;
    chunk = paras.slice(start, end);
  } else {
    const start = ch2.length >= 2 ? ch2[1] : ch2[0] ?? 0;
    const end = ch3.length >= 2 ? ch3[1] : ch3[0] ?? paras.length;
    chunk = paras.slice(start, end);
  }
}

fs.writeFileSync(outPath, chunk.join("\n\n"), "utf8");
console.log(`Extracted ${chunk.length} paragraphs (mode=${mode}) to ${outPath}`);
