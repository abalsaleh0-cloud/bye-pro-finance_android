# Architecture Overview - BYE PRO FINANCE

## Architectural Pattern
The application strictly adheres to Clean Architecture and MVVM (Model-View-ViewModel) with unidirectional data flow (UDF).

```
UI Layer (Jetpack Compose)
       │ ▲
       │ │ StateFlow (Unidirectional Flow)
       ▼ │
ViewModel (FinanceViewModel)
       │ ▲
       │ │ Coroutines & Flow
       ▼ │
Repository (FinanceRepository)
   ┌───┴───────────────┬──────────────────┐
   ▼                   ▼                  ▼
Room Local DB     Finance Engine    Gemini AI Service
(ACID SQLite)     (BigDecimal Math)  (OCR & Chat)
```

## Layers

1. **Presentation Layer (`com.example.ui`)**:
   - `MainAppScaffold`: Handles top bar, bottom navigation, drawer hub, and dynamic RTL/LTR layout direction.
   - Distinct screens: Dashboard, Accounts, Transactions, Transfers, Budgets, Debts & Bills, Savings & Assets, Reports, Household, Smart Scan, AI Assistant, Notifications & Admin, Settings.
   - Centralized `MyApplicationTheme` adhering to Material 3 with an executive dark financial palette (`#08101D`, `#00D09C`, `#F1B942`).

2. **Domain & Calculation Engine (`com.example.finance.engine`)**:
   - `FinanceEngine`: Pure Kotlin mathematical functions using `BigDecimal` with `HALF_EVEN` bankers' rounding.
   - Guarantees zero IEEE 754 floating-point inaccuracies for currency calculations.

3. **Data Layer (`com.example.finance.data`)**:
   - `AppDatabase`: Room database managing 14 distinct entities with TypeConverters.
   - `FinanceDao`: Reactive queries returning `Flow<List<T>>` for automatic Compose UI recomposition.
   - `FinanceRepository`: Encapsulates atomic balance adjustments, database operations, and automatic audit logging.

4. **Intelligence Layer (`com.example.finance.ai`)**:
   - `GeminiFinanceService`: Handles multimodal receipt OCR extraction and grounded conversational financial assistance.
