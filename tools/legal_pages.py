"""Builds docs/privacy.html and docs/terms.html from the app's LegalTexts.kt, so the
GitHub Pages copy Play Store links to says exactly what the app shows.

Run from the repo root: python3 tools/legal_pages.py
"""
import html
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/java/mk/vesti/app/ui/LegalTexts.kt"
OUT = ROOT / "docs"

text = SRC.read_text(encoding="utf-8")
consts = dict(re.findall(r'private const val (\w+) = "([^"]*)"', text))


def literal(s):
    s = re.sub(r"\$(\w+)", lambda m: consts.get(m.group(1), m.group(0)), s)
    return s.replace('\\"', '"')


def document(name):
    start = text.index(f"val {name} = LegalDocument(")
    end = text.find("\nval ", start + 1)
    body = text[start:end if end != -1 else len(text)]
    title = literal(re.search(r'title = "([^"]*)"', body).group(1))
    blocks = []
    for m in re.finditer(r"LegalBlock\.(Heading|Paragraph|Bullet)\((.*?)\n?\s*\),?\n", body, re.S):
        kind, args = m.groups()
        bullet_title = re.search(r'title = "([^"]*)"', args)
        main = args[: bullet_title.start()] if bullet_title else args
        content = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', main))
        blocks.append((kind, literal(content), literal(bullet_title.group(1)) if bullet_title else None))
    return title, blocks


def render(name, filename):
    title, blocks = document(name)
    parts, in_list = [], False
    for kind, content, bold in blocks:
        if kind != "Bullet" and in_list:
            parts.append("</ul>")
            in_list = False
        if kind == "Heading":
            parts.append(f"<h2>{html.escape(content)}</h2>")
        elif kind == "Paragraph":
            parts.append(f"<p>{html.escape(content)}</p>")
        else:
            if not in_list:
                parts.append("<ul>")
                in_list = True
            lead = f"<strong>{html.escape(bold)}</strong> " if bold else ""
            parts.append(f"<li>{lead}{html.escape(content)}</li>")
    if in_list:
        parts.append("</ul>")
    page = PAGE.format(
        title=html.escape(title),
        effective=html.escape(consts["EFFECTIVE"]),
        body="\n".join(parts),
    )
    (OUT / filename).write_text(page, encoding="utf-8")


PAGE = """<!doctype html>
<html lang="mk">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} · Вести</title>
<link rel="stylesheet" href="style.css">
</head>
<body>
<main>
<a class="back" href="./">Вести</a>
<h1>{title}</h1>
<p class="effective">Во сила од: {effective}</p>
{body}
</main>
</body>
</html>
"""

OUT.mkdir(exist_ok=True)
render("PrivacyPolicy", "privacy.html")
render("TermsOfUse", "terms.html")
print("wrote docs/privacy.html, docs/terms.html")
