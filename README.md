# BYE PRO FINANCE

> **Production-Grade Financial Management System for Android & Cross-Platform**
>
> An executive, secure, multi-currency financial ledger and intelligence platform built with Jetpack Compose, Kotlin, Room SQLite, and Google Gemini AI.

---

## 🌟 Key Features

1. **Executive Dashboard**:
   - Total Liquid Balance, Net Worth, Monthly Income, Monthly Expense, and Net Cash Flow.
   - **Financial Health Score (0-100)**: Real-time calculation based on savings rate, debt-to-asset ratio, and budget adherence.
   - Interactive Cash Flow comparison charts.
   - Live Accounts carousel and recent transactions ledger.

2. **Precision Finance Engine (`com.example.finance.engine`)**:
   - Zero floating-point rounding errors using `BigDecimal` arithmetic.
   - Sequential transaction balance recalculation.
   - Net Worth evaluation: `Total Liquid Accounts + Total Valued Assets - Total Outstanding Debts`.
   - Budget threshold monitors with proactive alerts at 50%, 75%, 90%, and 100%.
   - Debt payoff countdown and loan amortization.
   - Savings goal progress with required monthly deposit calculation.

3. **Multi-Account & Atomic Transfers**:
   - Bank accounts, Cash wallets, Digital wallets, Savings accounts, and Investment portfolios.
   - **Atomic Transfers**: Deducts amount & fee from source and deposits into target atomically, logging both transaction sides and audit logs.

4. **Smart Scan (Gemini Multimodal OCR)**:
   - Scans receipt/invoice images and extracts: Merchant, Total Amount, VAT/Tax, Date, Category, and itemized summaries.
   - Pre-save user confirmation card: Users review and edit extracted numbers before committing to the financial ledger.

5. **AI Financial Assistant & Spending Insights**:
   - Conversational assistant powered by Google Gemini, grounded strictly in user's active account balances, monthly spending, and budget status.
   - Pre-configured financial query chips.
   - Proactive Spending Insights & Anomaly Detection (identifies overspending risks and estimated monthly savings).

6. **Bilingual RTL/LTR Architecture**:
   - Full native Arabic (العربية - Default) with RTL layout direction.
   - Instant English (LTR) toggle without app restart.

7. **Security, RBAC & Audit Trail**:
   - Role-Based Access Control (Admin / User).
   - Tamper-evident Audit Logs (`audit_logs`) tracking creates, updates, deletes, transfers, and security events with timestamps.
   - AES-256 local storage encryption compatibility.

8. **Export & Portability**:
   - Export financial transactions to standard CSV.
   - Export executive financial summaries to JSON.

---

## 🛠 Tech Stack

- **Platform**: Android (Min SDK 24, Target SDK 36)
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Language**: Kotlin 2.2.10
- **Local Database**: Room 2.7.0 with SQLite & KSP Code Generation
- **Networking & AI**: OkHttp, Retrofit, Moshi, Google Gemini API
- **Testing**: JUnit 4, Robolectric 4.16.1

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or AI Studio Build Cloud Environment.
- Java 17 / 21 SDK.

### Build and Run
```bash
gradle assembleDebug
gradle :app:testDebugUnitTest
```
