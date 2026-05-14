import pathlib
import re
import zipfile


def docx_to_text(path: pathlib.Path) -> str:
    with zipfile.ZipFile(path) as z:
        xml = z.read("word/document.xml").decode("utf-8", errors="ignore")

    # Paragraph ends
    xml = xml.replace("</w:p>", "\n")
    # Tabs
    xml = xml.replace("</w:tab>", "\t")

    # Strip XML tags
    text = re.sub(r"<[^>]+>", "", xml)

    # Unescape a minimal set of entities
    text = (
        text.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", '"')
        .replace("&apos;", "'")
    )

    # Normalize whitespace
    text = re.sub(r"[ \t\r\f\v]+", " ", text)
    text = re.sub(r"\n\s*\n+", "\n\n", text).strip()
    return text


def main() -> None:
    base = pathlib.Path(__file__).resolve().parents[1]
    inputs = [
        base / "DuKien_ChuNang _version_3.docx",
        base / "CN-VuQuyetTien-23630716-KHMT-K63-De-cuong-DATN.docx",
    ]
    for p in inputs:
        if not p.exists():
            print(f"Missing: {p}")
            continue
        out = base / f"{p.stem}.extracted.txt"
        out.write_text(docx_to_text(p), encoding="utf-8")
        print(f"Wrote: {out}")


if __name__ == "__main__":
    main()

