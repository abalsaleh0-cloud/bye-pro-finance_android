#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="${1:?Android project directory is required}"
python3 - "$PROJECT_DIR" <<'PY'
from pathlib import Path
import sys
root = Path(sys.argv[1])

scaffold = root / "app/src/main/java/com/example/ui/navigation/MainAppScaffold.kt"
text = scaffold.read_text(encoding="utf-8")
old = "    val layoutDirection = if (lang == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr"
new = "    // Keep the application shell RTL for both languages; language selection changes text only.\n    val layoutDirection = LayoutDirection.Rtl"
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise SystemExit("Expected language-dependent layout direction declaration was not found")
scaffold.write_text(text, encoding="utf-8")

localization = root / "app/src/main/java/com/example/finance/i18n/Localization.kt"
text = localization.read_text(encoding="utf-8")
text = text.replace('"english" to "English (LTR)"', '"english" to "English"')
localization.write_text(text, encoding="utf-8")

readme = root / "README.md"
text = readme.read_text(encoding="utf-8")
text = text.replace(
    "6. **Bilingual RTL/LTR Architecture**:\\n   - Full native Arabic (العربية - Default) with RTL layout direction.\\n   - Instant English (LTR) toggle without app restart.",
    "6. **Bilingual Arabic/English Interface**:\\n   - Arabic is the default language and the application layout remains RTL.\\n   - Switching to English translates interface text only; navigation, controls, component placement, and screen layout stay RTL without an app restart."
)
readme.write_text(text, encoding="utf-8")
PY
