#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="${1:?Android project directory is required}"
python3 - "$PROJECT_DIR" <<'PY'
from pathlib import Path
import sys

root = Path(sys.argv[1])

def source(relative):
    path = root / relative
    if not path.is_file():
        raise SystemExit(f"Required source file is missing: {relative}")
    return path.read_text(encoding="utf-8")

def require(relative, marker):
    if marker not in source(relative):
        raise SystemExit(f"Required implementation is missing in {relative}: {marker}")

require("app/src/main/java/com/example/ui/navigation/MainAppScaffold.kt",
        "val layoutDirection = WebI18n.layoutDirection()")
require("app/src/main/java/com/example/finance/ui/FinanceViewModel.kt",
        "MutableStateFlow(WebI18n.read(application))")
require("app/src/main/java/com/example/finance/ui/FinanceViewModel.kt",
        "WebI18n.write(getApplication(), lang)")
require("app/src/main/java/com/example/finance/ui/FinanceViewModel.kt",
        'preferences.getBoolean("dark_mode", true)')
require("app/src/main/java/com/example/finance/ui/FinanceViewModel.kt",
        'preferences.edit().putBoolean("dark_mode", dark).apply()')
require("app/src/main/java/com/example/finance/i18n/WebI18n.kt",
        'const val LOCALE_KEY = "bye-pro-finance.locale"')
require("app/src/main/java/com/example/finance/i18n/WebI18n.kt",
        '.getString(LOCALE_KEY, AR)')
require("app/src/main/java/com/example/finance/i18n/WebI18n.kt",
        '.putString(LOCALE_KEY, locale(language))')
require("app/src/main/java/com/example/ui/screens/SettingsScreen.kt",
        'testTag("theme_mode_switch")')
require("app/src/main/java/com/example/finance/i18n/Localization.kt",
        '"dashboard" to "لوحة التحكم"')
require("app/src/main/java/com/example/finance/i18n/Localization.kt",
        '"dashboard" to "Dashboard"')
require("app/src/main/java/com/example/ui/theme/DesignTokens.kt",
        "val background = Color(0xFF0B0A0C)")
require("app/src/main/java/com/example/ui/theme/Color.kt",
        "val DarkBgMain = Color(0xFF0B0A0C)")

print("PASS: Arabic-first locale persistence, fixed RTL, persistent theme, and web-matched design tokens.")
PY
