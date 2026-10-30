#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$1"
if [[ -z "$PROJECT_DIR" ]]; then echo "Android project directory is required" >&2; exit 2; fi
python3 - "$PROJECT_DIR" <<'PY'
from pathlib import Path
import re, sys
root = Path(sys.argv[1])

scaffold = root / "app/src/main/java/com/example/ui/navigation/MainAppScaffold.kt"
s = scaffold.read_text(encoding="utf-8")
s = re.sub(
    r"val layoutDirection = if \(lang == AppLanguage\.AR\) LayoutDirection\.Rtl else LayoutDirection\.Ltr",
    "val layoutDirection = LayoutDirection.Rtl",
    s,
)
if "val layoutDirection = LayoutDirection.Rtl" not in s:
    raise SystemExit("Could not establish fixed RTL layout direction")
if "Language changes text only;" not in s:
    s = s.replace(
        "val layoutDirection = LayoutDirection.Rtl",
        "// Language changes text only; the app shell and all navigation remain RTL.\n    val layoutDirection = LayoutDirection.Rtl",
        1,
    )
scaffold.write_text(s, encoding="utf-8")

vm = root / "app/src/main/java/com/example/finance/ui/FinanceViewModel.kt"
s = vm.read_text(encoding="utf-8")
s = s.replace(
    "private val _isDarkMode = MutableStateFlow(true)",
    'private val preferences = application.getSharedPreferences("bye_pro_finance_preferences", Application.MODE_PRIVATE)\n    private val _isDarkMode = MutableStateFlow(preferences.getBoolean("dark_mode", true))',
)
s = s.replace(
    "fun setDarkMode(dark: Boolean) {\n        _isDarkMode.value = dark\n    }",
    'fun setDarkMode(dark: Boolean) {\n        _isDarkMode.value = dark\n        preferences.edit().putBoolean("dark_mode", dark).apply()\n    }',
)
s = s.replace(
    "fun toggleTheme() {\n        _isDarkMode.value = !_isDarkMode.value\n    }",
    "fun toggleTheme() {\n        setDarkMode(!_isDarkMode.value)\n    }",
)
if 'preferences.getBoolean("dark_mode", true)' not in s:
    raise SystemExit("Theme preference persistence patch failed")
vm.write_text(s, encoding="utf-8")

replacements = {
    "GoldPrimary": "ByeProTheme.colors.goldPrimary",
    "FinancialGreen": "ByeProTheme.colors.financialGreen",
    "FinancialRed": "ByeProTheme.colors.financialRed",
    "ExecutiveGold": "ByeProTheme.colors.goldPrimary",
    "ExecutiveCyan": "ByeProTheme.colors.goldPrimary",
    "EmeraldMint": "ByeProTheme.colors.financialGreen",
    "GoldMuted": "ByeProTheme.colors.goldMuted",
    "DarkCardSecondary": "ByeProTheme.colors.cardSecondary",
}
names = set(replacements)
for subdir in ("screens", "components", "navigation"):
    for path in (root / "app/src/main/java/com/example/ui" / subdir).rglob("*.kt"):
        content = path.read_text(encoding="utf-8")
        original = content
        lines = []
        for line in content.splitlines():
            stripped = line.strip()
            if stripped.startswith("import com.example.ui.theme.") and any(
                re.search(r"\b" + re.escape(n) + r"\s*$", stripped) for n in names
            ):
                continue
            lines.append(line)
        content = "\n".join(lines) + "\n"
        for old, new in replacements.items():
            content = re.sub(r"\b" + re.escape(old) + r"\b", new, content)
        content = content.replace("Color(0xFF111015)", "MaterialTheme.colorScheme.onPrimary")
        content = content.replace("Color(0xFF0B0A0C)", "MaterialTheme.colorScheme.onPrimary")
        content = content.replace(
            "Color.Black.copy(alpha = 0.7f)",
            "MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)",
        )
        content = content.replace("Color.Black", "MaterialTheme.colorScheme.onPrimary")
        content = content.replace("Color(0xFFFF5252)", "MaterialTheme.colorScheme.error")
        content = content.replace("Color.DarkGray.copy(alpha = 0.3f)", "ByeProTheme.colors.borderBase.copy(alpha = 0.65f)")
        if content != original:
            if "import com.example.ui.theme.ByeProTheme" not in content:
                content = content.replace(
                    "\n\n", "\n\nimport com.example.ui.theme.ByeProTheme\n", 1
                )
            path.write_text(content, encoding="utf-8")

components = root / "app/src/main/java/com/example/ui/components/FinancialComponents.kt"
s = components.read_text(encoding="utf-8")
s = s.replace(
    '        label = "healthScore"\n    )\n\n    Card(',
    '        label = "healthScore"\n    )\n    val healthColors = ByeProTheme.colors\n\n    Card(',
    1,
)
s = s.replace(
    "listOf(ByeProTheme.colors.goldPrimary, ByeProTheme.colors.financialGreen, ByeProTheme.colors.goldMuted)",
    "listOf(healthColors.goldPrimary, healthColors.financialGreen, healthColors.goldMuted)",
    1,
)
s = s.replace("color = Color.DarkGray.copy(alpha = 0.4f)", "color = healthColors.borderBase.copy(alpha = 0.65f)", 1)
components.write_text(s, encoding="utf-8")

loc = root / "app/src/main/java/com/example/finance/i18n/Localization.kt"
s = loc.read_text(encoding="utf-8")
s = s.replace(
    '"language" to "اللغة (Language)",',
    '"language" to "اللغة",\n'
    '        "appearance_title" to "مظهر التطبيق",\n'
    '        "dark_mode" to "الوضع الداكن",\n'
    '        "light_mode" to "الوضع الفاتح",\n'
    '        "appearance_hint" to "اختر ألوان BYE PRO FINANCE المناسبة لك.",',
    1,
)
s = s.replace(
    '"language" to "Language (اللغة)",',
    '"language" to "Language",\n'
    '        "appearance_title" to "Appearance",\n'
    '        "dark_mode" to "Dark mode",\n'
    '        "light_mode" to "Light mode",\n'
    '        "appearance_hint" to "Choose your preferred BYE PRO FINANCE color theme.",',
    1,
)
s = s.replace('"arabic" to "العربية (RTL)",', '"arabic" to "العربية",', 1)
s = s.replace('"arabic" to "العربية (RTL)",', '"arabic" to "Arabic",', 1)
s = s.replace('"english" to "English (LTR)",', '"english" to "English",')
loc.write_text(s, encoding="utf-8")

theme = root / "app/src/main/java/com/example/ui/theme/Theme.kt"
theme_source = theme.read_text(encoding="utf-8")
light_marker = "val ByeProLightTokens ="
if light_marker not in theme_source:
    raise SystemExit("Light theme token block was not found")
dark_part, light_part = theme_source.split(light_marker, 1)
light_part = light_part.replace("goldMuted = GoldMuted,", "goldMuted = LightGoldFeatured,", 1)
light_part = light_part.replace("goldDark = GoldDark,", "goldDark = LightGoldPrimary,", 1)
light_part = light_part.replace("financialGreen = FinancialGreen,", "financialGreen = LightStatusSuccess,", 1)
light_part = light_part.replace("financialRed = FinancialRed,", "financialRed = LightStatusError,", 1)
theme.write_text(dark_part + light_marker + light_part, encoding="utf-8")

settings = root / "app/src/main/java/com/example/ui/screens/SettingsScreen.kt"
s = settings.read_text(encoding="utf-8")
if "import androidx.compose.material3.Switch" not in s:
    s = s.replace(
        "import androidx.compose.material3.Text",
        "import androidx.compose.material3.Text\nimport androidx.compose.material3.Switch",
    )
if "val isDark by viewModel.isDarkMode.collectAsState()" not in s:
    s = s.replace(
        "    val lang by viewModel.appLanguage.collectAsState()\n",
        "    val lang by viewModel.appLanguage.collectAsState()\n"
        "    val isDark by viewModel.isDarkMode.collectAsState()\n",
        1,
    )
s = s.replace(
    'Text("العربية (RTL)", fontWeight = FontWeight.Bold)',
    'Text(Strings.get("arabic", lang), fontWeight = FontWeight.Bold)',
)
s = s.replace(
    'Text("English (LTR)", fontWeight = FontWeight.Bold)',
    'Text(Strings.get("english", lang), fontWeight = FontWeight.Bold)',
)
marker = "        // App System Info\n"
block = """        // Appearance / Theme Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.get("appearance_title", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings.get("appearance_hint", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Strings.get(if (isDark) "dark_mode" else "light_mode", lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isDark,
                            onCheckedChange = viewModel::setDarkMode,
                            modifier = Modifier.testTag("theme_mode_switch")
                        )
                    }
                }
            }
        }

"""
if marker in s and "theme_mode_switch" not in s:
    s = s.replace(marker, block + marker, 1)
if "theme_mode_switch" not in s:
    raise SystemExit("Settings theme switch was not inserted")
settings.write_text(s, encoding="utf-8")

assert "val layoutDirection = LayoutDirection.Rtl" in scaffold.read_text(encoding="utf-8")
assert 'preferences.getBoolean("dark_mode", true)' in vm.read_text(encoding="utf-8")
assert "theme_mode_switch" in settings.read_text(encoding="utf-8")
assert '"appearance_title" to "Appearance"' in loc.read_text(encoding="utf-8")
print("PASS: Arabic-first fixed RTL, persisted light/dark mode, theme-aware brand colors, bilingual Settings selector.")
PY