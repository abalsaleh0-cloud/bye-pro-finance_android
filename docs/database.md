# Database Schema & Data Models - BYE PRO FINANCE

The application uses an ACID-compliant Room SQLite database (`bye_pro_finance.db`).

## Tables & Entities

### 1. `users`
- `id`: Long (Primary Key)
- `name`: String
- `email`: String
- `role`: Enum (`ADMIN`, `USER`)
- `currencyPreference`: String
- `languagePreference`: Enum (`AR`, `EN`)
- `createdAt`: Long

### 2. `accounts`
- `id`: Long (Primary Key)
- `name`: String
- `type`: Enum (`BANK`, `CASH`, `WALLET`, `CREDIT_CARD`, `SAVINGS`, `INVESTMENT`, `OTHER`)
- `currency`: String
- `balance`: Double
- `accountNumberSafe`: String (Masked)
- `institution`: String
- `isArchived`: Boolean
- `notes`: String
- `colorHex`: String

### 3. `transactions`
- `id`: Long (Primary Key)
- `accountId`: Long
- `type`: Enum (`INCOME`, `EXPENSE`, `TRANSFER`, `REFUND`, `ADJUSTMENT`)
- `amount`: Double
- `currency`: String
- `category`: String
- `date`: Long
- `description`: String
- `merchant`: String
- `targetAccountId`: Long?

### 4. `transfers`
- `id`: Long (Primary Key)
- `fromAccountId`: Long
- `toAccountId`: Long
- `amount`: Double
- `fee`: Double
- `currency`: String
- `date`: Long
- `description`: String
- `reference`: String

### 5. `budgets`
- `id`: Long (Primary Key)
- `name`: String
- `category`: String
- `period`: Enum (`WEEKLY`, `MONTHLY`, `YEARLY`, `CUSTOM`)
- `limitAmount`: Double
- `currency`: String
- `startDate`: Long
- `endDate`: Long

### 6. `debts` & `debt_payments`
- `id`: Long (Primary Key)
- `name`: String
- `creditor`: String
- `principal`: Double
- `remaining`: Double
- `interestRate`: Double
- `dueDate`: Long
- `minimumPayment`: Double
- `status`: Enum (`ACTIVE`, `PAID`, `DEFAULTED`)

### 7. `bills`
- `id`: Long (Primary Key)
- `name`: String
- `category`: String
- `amount`: Double
- `dueDate`: Long
- `isPaid`: Boolean

### 8. `savings_goals`
- `id`: Long (Primary Key)
- `name`: String
- `targetAmount`: Double
- `currentAmount`: Double
- `deadline`: Long

### 9. `assets`
- `id`: Long (Primary Key)
- `name`: String
- `type`: Enum (`PROPERTY`, `VEHICLE`, `CASH`, `INVESTMENTS`, `GOLD`, `OTHER`)
- `value`: Double

### 10. `household_members` & `household_expenses`
- Split tracking, member percentage, and family expense contributions.

### 11. `documents`
- `id`: Long (Primary Key)
- `title`: String
- `type`: Enum (`RECEIPT`, `INVOICE`, `STATEMENT`, `OTHER`)
- `merchant`: String
- `totalAmount`: Double
- `taxAmount`: Double

### 12. `notifications`
- Alerts for budget thresholds, bill dues, debts, and anomalies.

### 13. `audit_logs`
- Tamper-evident financial audit trail (`action`, `resource`, `details`, `userRole`, `timestamp`).
