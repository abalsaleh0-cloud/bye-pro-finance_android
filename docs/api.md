# External & Internal Service Integrations - BYE PRO FINANCE

## AI Service (Google Gemini)

### Endpoint
`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={GEMINI_API_KEY}`

### Protocols
- **Multimodal OCR**: Processes receipt and invoice images or textual transcripts to extract structured JSON entities:
  - `merchant`: Payee identity
  - `amount`: Total decimal value
  - `tax`: Calculated or parsed VAT
  - `category`: Financial category
- **Contextual Grounding**: Injects live user ledger totals, active accounts, and budget health into the system prompt to prevent hallucination.
- **Safety Layer**: Includes mandatory client-side disclaimer notifying the user that responses are analytical estimations and not professional licensed financial advice.

## Local Ledger API (`FinanceRepository`)

All mutations are atomic:
- `addTransaction(TransactionEntity)`
- `executeTransfer(fromId, toId, amount, fee, ...)`
- `recordDebtPayment(debtId, amount, notes)`
- `markBillPaid(billId, accountId)`
- `contributeToSavings(goalId, amount, accountId)`
- `logAudit(action, resource, details)`
