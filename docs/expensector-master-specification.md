# EXPENSECTOR — MASTER PROJECT SPECIFICATION

You are working on **Expensector**, a native Android personal finance application focused on expense control, cash-flow planning, budgeting, debt tracking, credit-card commitments, and manually tracked investments.

This document is the authoritative functional and architectural specification for the project.

Do not attempt to implement the entire application in a single task unless explicitly instructed. The project must be developed incrementally by stages, with each stage compiling, being testable, and preserving the architecture established by previous stages.

The owner will manually review, approve, commit, merge, and push all changes. Do not perform Git commits, merges, pushes, branch creation, or destructive repository operations unless explicitly requested.

---

# 1. Product identity

Application name:

Expensector

Android package name:

com.danielgarcia.expensector

Platform:

Native Android

Primary language:

Kotlin

UI framework:

Jetpack Compose with Material 3

Build configuration:

Gradle Kotlin DSL

Minimum Android SDK:

API 36 — Android 16

Initial default currency:

MXN

The data model must still store an ISO 4217 currency code so additional currencies can be supported later.

Expensector is initially a single-user, completely local, offline application.

It must not require:

* a backend;
* cloud synchronization;
* external financial APIs;
* market-data APIs;
* bank integrations;
* internet access for its core functionality.

Do not add network permissions or remote services unless explicitly requested in a future task.

---

# 2. Product objective

Expensector must help the user understand not only what has already been spent, but also:

* how much money remains available in the current budget period;
* how much money is already committed;
* which future periods will be affected by current purchases;
* how much can safely be spent before exceeding a budget;
* how debts and investments are progressing;
* where the money available in a period came from.

The main product principle is:

“Expensector does not only record past expenses. It shows how much can still be spent today after considering current and future commitments.”

---

# 3. Initial operating model

The first user organizes finances in semimonthly periods:

* First period: day 1 through day 15.
* Second period: day 16 through the last day of the month.

If the salary payment date falls on a weekend, the deposit may be received on the previous Friday.

The application must distinguish:

* the formal budget-period start and end dates;
* the expected payment date;
* the actual deposit date.

An early salary deposit must not change the formal period boundaries.

The architecture must later support:

* weekly periods;
* biweekly periods;
* semimonthly periods;
* monthly periods;
* custom periods.

Do not hard-code semimonthly assumptions into the entire domain model.

---

# 4. Core financial concepts

The application must distinguish the following domain concepts.

## 4.1 Financial space

A financial space separates money belonging to different financial contexts.

Examples:

* Personal.
* Nutrition application project.
* Business.
* Household.
* Another project.

Initially, only the personal financial space may be used.

The model must allow future movements to belong to a different financial space without affecting the personal budget.

A financial space is not the same as a bank account.

## 4.2 Financial account

A financial account represents where money exists or moves.

Supported account concepts include:

* Cash.
* Checking or debit account.
* Savings account.
* Credit card.
* Digital wallet.
* Investment vehicle.

## 4.3 Movement

A financial movement may represent:

* Income.
* Expense.
* Transfer.
* Investment contribution.
* Investment withdrawal.
* Investment return.
* Debt payment.
* Credit-card payment.
* Balance adjustment.

Movement types must not be represented only as arbitrary text.

## 4.4 Future commitment

A future commitment represents money not yet paid but already expected or obligated.

Examples:

* Subscription.
* Recurring expense.
* Credit-card purchase.
* Installment purchase.
* Debt payment.
* Mandatory investment contribution.
* Scheduled payment.

## 4.5 Budget period

A budget period contains:

* start date;
* end date;
* expected income date;
* actual income date;
* ordinary income;
* extraordinary income;
* carried surplus;
* committed amounts;
* actual movements;
* category budgets;
* projected remaining capacity;
* final surplus or deficit.

---

# 5. Money availability model

For each period, Expensector must calculate the origin and use of available money.

Conceptual formula:

Ordinary income

* extraordinary income
* surplus carried from previous periods

- confirmed expenses
- committed expenses
- debt payments
- confirmed investments
  = remaining available money

The application must preserve the composition of available money.

Example:

Total available: $31,500 MXN

* Ordinary salary: $27,500 — 87.30%
* Previous-period surplus: $2,500 — 7.94%
* Extraordinary income: $1,500 — 4.76%

When a payment or expense is made, the user does not need to select which source is consumed first. The expense reduces the total available amount.

The source composition remains available for analytics and explanation.

---

# 6. Period rollover

At the end of a period, unspent money must be carried to the next period as:

Previous-period surplus

It must not be classified as:

* salary;
* ordinary income;
* extraordinary income.

The next period must show:

* the amount carried over;
* its percentage of total available money;
* its originating period.

If a historical movement is edited, cancelled, restored, or reassigned, all affected period balances and later rollovers must be recalculated.

Before changing a closed historical period, the user must be warned that later balances may change.

---

# 7. Budget configuration

A new budget period must automatically copy the reusable configuration from the previous comparable period.

Copy:

* budget allocations;
* category targets;
* active recurring commitments;
* active mandatory investments;
* reusable rules;
* active debt obligations.

Do not copy:

* one-time expenses;
* extraordinary income;
* manual transfers;
* voluntary investments;
* one-time debt prepayments;
* cancelled items.

The copied budget must remain editable without changing previous periods.

Budgets may be defined using:

* fixed amounts;
* percentages of period income;
* a combination of fixed and percentage allocations.

---

# 8. Expense classification

Every expense or allocation may belong to a top-level financial purpose:

* NEED
* WANT
* INVESTMENT

These codes must remain stable domain values and must not depend on translated display strings.

The user must also be able to define categories and subcategories.

Example:

NEED
└── Transportation
├── Fuel
├── Maintenance
├── Insurance
└── Parking

Visualizations must support drill-down:

Purpose
→ Category
→ Subcategory
→ Movement list
→ Movement detail

Categories and subcategories must support:

* active and inactive status;
* creation date;
* update date;
* aliases or normalized names when useful;
* safe merge operations;
* historical references.

Do not permanently delete categories that are referenced by historical data.

---

# 9. Reusable movement dictionary

Expensector must reuse previously entered values to reduce duplicate data entry.

Reusable concepts may include:

* merchants;
* recipients;
* movement descriptions;
* categories;
* subcategories;
* tags;
* transfer concepts.

The application must suggest existing values before creating new ones.

Duplicate detection should normalize values by considering:

* case;
* leading and trailing spaces;
* repeated spaces;
* accents where appropriate;
* simple singular or plural similarities when reliable.

The application must never silently merge categories or merchant records.

It may suggest:

“An existing category appears to match this value.”

The user must explicitly confirm creation or reuse.

A future category-merge operation must preserve all historical references.

---

# 10. Movement details

All applicable movements should support:

* stable identifier;
* financial space;
* movement type;
* amount;
* currency code;
* transaction date;
* effective budget date;
* description;
* notes;
* category;
* subcategory;
* source account;
* destination account;
* payment method;
* merchant or counterparty;
* reference;
* folio;
* ticket number;
* tags;
* creation timestamp;
* modification timestamp;
* status.

Do not store receipt photographs or invoice images.

Transfer-specific information may include:

* recipient bank;
* recipient name or alias;
* transfer concept;
* reference number;
* application date;
* optional last four digits of the destination account.

Do not encourage storing complete bank-account numbers.

---

# 11. Historical records and deletion behavior

Financial history must remain traceable.

Movements must not normally be permanently deleted from the user interface.

Supported lifecycle actions should include:

* Active.
* Cancelled.
* Archived.
* Moved to trash.
* Restored.

Permanent deletion may be reserved for special administrative cases and should not be the normal operation.

Cancelling or restoring a movement must update all affected calculations.

Historical movements should preserve relevant snapshots such as:

* category display name at the time of the movement when needed;
* expected amount;
* actual amount;
* applicable recurrence version;
* card statement assignment;
* budget-period assignment.

---

# 12. Recurring items

Recurring items may include:

* subscriptions;
* household services;
* salary;
* recurring debts;
* mandatory investments;
* recurring transfers;
* recurring expenses.

Supported recurrence patterns must eventually include:

* weekly;
* semimonthly;
* biweekly;
* monthly;
* every N months;
* yearly on a defined date;
* custom recurrence where practical.

Recurring items must generate pending occurrences.

Occurrence states may include:

* PENDING
* CONFIRMED
* MODIFIED
* SKIPPED
* CANCELLED

A recurring item must not automatically become a confirmed real movement.

The user must confirm the occurrence and may adjust its actual amount.

When changing a recurring amount, the application must offer:

* Change only this occurrence.
* Change this and future occurrences.

Historical occurrences must never be changed by a future price update.

Example:

Disney+:

* $179 valid through July 31.
* $219 valid from August 1 onward.

The recurrence configuration must support effective-date versioning.

Deleting or disabling a recurring item must affect only future occurrences and preserve history.

The initial telephone expense currently grouped in the source spreadsheet must eventually be represented as two independent recurring items:

* Mobile telephone.
* Home telephone.

---

# 13. Credit cards

Each credit card may store:

* name;
* issuer;
* alias;
* optional last four digits;
* currency code;
* credit limit;
* used credit;
* available credit;
* statement closing day;
* payment due day;
* annual or monthly interest information;
* minimum payment;
* payment required to avoid interest;
* active status.

The model must distinguish:

* purchase date;
* posting date;
* statement closing date;
* payment due date;
* affected budget period;
* actual payment date.

A purchase made by credit card has two different effects:

## Economic commitment

The amount reduces uncommitted spending capacity immediately after the purchase is recorded.

## Cash-flow impact

The actual cash outflow is assigned to the period in which the credit-card bill is expected to be paid.

The first installment of a purchase made before the statement closing date appears in the next statement.

Example:

* Purchase date: July 18.
* Statement closing date: July 27.
* Payment due date: August 12.
* Expected payment period: August 1–15.

The application should select the first income period that falls inside the valid payment window, while allowing the user to override the proposed period.

---

# 14. Credit-card statements and payments

Credit-card charges may progress through states such as:

* RECORDED
* INCLUDED_IN_STATEMENT
* PAYMENT_PENDING
* PARTIALLY_PAID
* PAID
* CANCELLED

A card statement should track:

* statement amount;
* minimum payment;
* payment required to avoid interest;
* total paid;
* remaining balance;
* closing date;
* due date;
* applied payments;
* statement status.

The user may make multiple partial payments.

For the first implementation, partial payments may be applied to the statement balance without allocating each payment to individual purchases.

Do not mark a statement as paid until its remaining balance reaches zero or an allowed tolerance.

---

# 15. Installment purchases

Expensector must support:

* single-payment card purchases;
* months without interest;
* months with interest;
* financed or restructured debts.

An installment purchase may store:

* original amount;
* financed amount;
* total amount payable;
* number of installments;
* installment amount;
* interest rate or financing cost;
* purchase date;
* first statement date;
* expected completion date;
* installments paid;
* installments remaining;
* outstanding balance;
* associated credit card.

Installment purchases must:

* appear as future card commitments;
* appear in total debt summaries;
* not be counted twice in consolidated totals.

---

# 16. Unexpected expenses and automatic recommendations

When an unexpected expense exceeds the currently unallocated amount, Expensector should recommend adjustments in this priority:

1. Reduce unspent WANT budget.
2. Reduce planned but unexecuted voluntary investment.
3. Suggest using the emergency-fund goal.
4. Show the remaining deficit.

The application must not automatically withdraw from or consume the emergency fund.

The user must approve any proposed redistribution.

---

# 17. Expense simulation

Expensector must provide a simulator that evaluates a potential purchase before recording it.

Inputs may include:

* amount;
* currency;
* date;
* payment method;
* account or card;
* single payment or installments;
* purpose;
* category;
* affected period.

The simulation should show:

* current remaining availability;
* remaining availability after the purchase;
* effect on NEED, WANT, or INVESTMENT budgets;
* future committed amount;
* affected card statement;
* affected future budget periods;
* percentage of remaining budget consumed;
* warnings or recommendations.

Example message:

“This purchase would consume 71% of the remaining WANT budget and reduce the next period’s free spending capacity to $600 MXN.”

The user must then be able to:

* record the purchase;
* change the payment method;
* simulate installments;
* cancel the simulation.

---

# 18. Debts

Debt records may store:

* original principal;
* current balance;
* currency;
* interest rate;
* periodic payment;
* payment frequency;
* start date;
* expected completion date;
* creditor;
* notes;
* extraordinary payments;
* active status.

The application must support:

* payment history;
* remaining payments;
* projected completion;
* total paid;
* total remaining;
* progress percentage;
* extraordinary-payment simulations.

Example:

“If you pay an additional $1,000 per period, the debt may be completed four months earlier.”

Installment purchases may be summarized as debt but must remain linked to their credit-card commitments.

---

# 19. Investments

Expensector must distinguish between mandatory and voluntary investments.

## Mandatory investment

Example:

* PPR.

Characteristics:

* recurring commitment;
* included in future projections;
* generates pending occurrences;
* may be confirmed, modified, or skipped.

## Voluntary investment

Examples:

* GBM.
* Supertasas.
* Kubo.
* Extraordinary investment contributions.

Characteristics:

* does not automatically affect future capacity unless scheduled;
* affects the period only when recorded or explicitly planned.

Investment vehicles are not expense categories.

An investment vehicle may store:

* name;
* type;
* description;
* currency;
* active status;
* manually calculated total balance;
* notes.

Each investment operation may store:

* investment vehicle;
* contribution date;
* invested capital;
* expected return, optional;
* actual return;
* maturity date, optional;
* current manually entered value;
* withdrawn amount;
* reinvested amount;
* return available for reinvestment;
* status;
* notes.

The application must distinguish:

* expected return;
* generated return;
* realized return;
* unrealized return;
* reinvested return;
* return available for reinvestment.

No market APIs must be integrated.

All values are entered or updated manually.

---

# 20. Savings goals

The emergency fund must be represented as a savings goal, not merely as an expense category.

Savings goals may eventually store:

* name;
* target amount;
* current accumulated amount;
* target date;
* currency;
* linked account;
* contribution history;
* active status.

Expensector may recommend using a savings goal, but must require user confirmation.

---

# 21. Notifications

Users must eventually be able to create multiple configurable alerts for each budget.

Examples:

* 50% consumed.
* 75% consumed.
* 90% consumed.
* 100% consumed.
* Budget exceeded.
* 25% remaining.

Example:

“Expense alert: only 25% of your WANT budget remains.”

The application should also support predictive messages:

* projected budget exhaustion date;
* future period already heavily committed;
* purchase simulation impact;
* upcoming recurring payment;
* upcoming card due date.

Positive progress notifications may include:

* debt progress;
* remaining payments;
* investment growth;
* increased accumulated investment;
* return available for reinvestment;
* savings-goal progress.

Notifications must avoid misleading financial claims and must be based only on locally stored user data.

---

# 22. Dashboard and reporting

The home screen should answer:

1. How much money remains in the current period?
2. How much remains for wants?
3. Which payments are upcoming?
4. How much is already committed in future periods?
5. How are debts, savings, and investments progressing?

Visualizations may include:

* progress bars;
* budget-versus-actual bars;
* distribution pie charts;
* historical time series;
* projected time series;
* stacked investment time series.

Charts must allow values to be viewed as:

* currency amounts;
* percentages.

Historical and future periods must both be supported.

---

# 23. Movement list and detail

The application must provide a movement-list screen with:

* chronological list;
* text search;
* period filter;
* movement-type filter;
* account filter;
* credit-card filter;
* purpose filter;
* category filter;
* subcategory filter;
* status filter;
* financial-space filter;
* sorting by date or amount.

Selecting a movement must open a detailed view containing:

* all movement fields;
* notes;
* related account or card;
* budget-period impact;
* recurrence;
* statement;
* payment status;
* modification history where available.

---

# 24. Security

Expensector contains sensitive personal financial information.

The first version must support:

* six-digit local PIN;
* Android biometric authentication through BiometricPrompt;
* supported biometric methods exposed by the device, such as fingerprint or secure face recognition;
* secure fallback;
* automatic locking.

Default automatic lock:

One minute after the application enters the background.

The user may later configure this duration.

The application should also lock after:

* process restart;
* device restart where applicable;
* explicit manual lock;
* security-setting changes.

Do not store the PIN in plain text.

Use an appropriately salted, computationally expensive PIN verification mechanism.

Use Android Keystore for sensitive cryptographic key material where appropriate.

Do not implement fake email authentication.

A local owner profile may contain an email address for identification and future compatibility, but authentication is performed locally through PIN and biometrics.

---

# 25. Local persistence

All operational data must remain on the device.

Use a local relational database suitable for Android, preferably Room unless an explicitly approved alternative is required.

The database must support:

* migrations;
* foreign keys;
* indexes;
* stable IDs;
* transactional updates;
* historical versioning;
* recalculation of dependent balances.

Use DataStore for lightweight application preferences when appropriate.

Avoid storing structured financial domain records only in DataStore or JSON preference files.

The database model must not use floating-point types for money.

Store monetary values using a precise representation, such as integer minor units or another deliberate exact decimal strategy.

Document the selected strategy.

---

# 26. Starting balances

The user will not import the original Excel history.

Initial setup must allow manually entered opening balances for:

* cash;
* bank accounts;
* savings accounts;
* credit cards;
* outstanding debts;
* investment vehicles;
* savings goals.

Opening balances must be represented explicitly and remain traceable.

Do not fabricate historical transactions before the selected opening date.

---

# 27. Currency support

Every monetary record must include or derive a valid ISO 4217 currency code.

Initial user currency:

MXN

The first UI may support only MXN, but the domain and persistence layers must not assume that all records will always use MXN.

Do not implement automatic currency conversion unless explicitly requested later.

---

# 28. CSV export

Expensector must eventually export structured CSV data for Power BI.

Do not export all data into a single denormalized file by default.

A future export package may include:

* transactions.csv
* accounts.csv
* credit_cards.csv
* credit_card_statements.csv
* credit_card_payments.csv
* categories.csv
* budget_periods.csv
* budgets.csv
* recurring_items.csv
* recurring_occurrences.csv
* debts.csv
* debt_payments.csv
* investment_vehicles.csv
* investment_operations.csv
* investment_returns.csv
* financial_spaces.csv
* savings_goals.csv

Use stable identifiers to preserve relationships.

The final export may be packaged in a ZIP file.

CSV output must be deterministic, documented, and Power BI friendly.

---

# 29. Encrypted backup

Expensector must eventually support a complete encrypted backup for restoration on another device.

The backup must:

* include all local application data and required configuration;
* use a defined application backup format;
* include schema version;
* include creation date;
* include integrity validation;
* require a user-provided backup password;
* use authenticated encryption;
* never store the application PIN in readable form;
* validate the backup before replacing current data.

Before restoring, the application should:

1. Select the backup.
2. Request the backup password.
3. Validate integrity.
4. Show metadata.
5. Warn about replacement.
6. Create a preventive backup when feasible.
7. Restore transactionally.

---

# 30. Architecture principles

Use a maintainable layered architecture appropriate for a local Android application.

Preferred conceptual layers:

* UI and presentation.
* Domain.
* Data.
* Security.
* Platform services.

Use:

* unidirectional data flow;
* immutable UI state;
* ViewModels;
* Kotlin coroutines;
* Flow where useful;
* repository abstractions;
* dependency injection;
* explicit domain models;
* mappers between database and domain models where appropriate.

Avoid unnecessary enterprise complexity.

Do not create a separate Gradle module for every small feature without a clear benefit.

A modular monolith or well-organized single application module is acceptable initially.

The architecture must remain testable and capable of later feature extraction.

---

# 31. User experience principles

Recording a routine expense must be fast.

Avoid forcing the user through many screens for common actions.

The main screen should provide a clearly visible action to register a movement.

Forms must:

* reuse previous values;
* offer sensible defaults;
* show validation close to the field;
* preserve unfinished state when practical;
* avoid silent destructive actions;
* explain financial impact.

The application must distinguish:

* actual money;
* projected money;
* committed money;
* available money;
* debt;
* credit availability.

Do not present credit-card available credit as actual spendable income.

---

# 32. Quality requirements

Each implementation stage must:

* compile successfully;
* follow existing repository conventions;
* avoid unrelated refactoring;
* include meaningful tests;
* include accessibility labels;
* handle configuration changes;
* avoid hard-coded user-facing strings;
* use Android string resources;
* use stable domain codes separately from translated labels;
* include clear error states;
* avoid placeholder logic presented as complete functionality.

When a requested feature cannot be completed safely within the current stage, create a clear interface or TODO only when necessary and document why.

Do not create fake implementations that silently return success.

---

# 33. Testing strategy

Use an appropriate combination of:

* domain unit tests;
* repository tests;
* database migration tests;
* ViewModel tests;
* Compose UI tests;
* security-related tests where practical.

Financial calculations must have explicit tests for:

* exact monetary arithmetic;
* period boundaries;
* leap years;
* month-end dates;
* weekend-adjusted payment dates;
* surplus rollover;
* future commitment assignment;
* historical recalculation;
* partial payments;
* installment schedules.

---

# 34. Development stages

The planned implementation stages are:

## Stage 1 — Local foundation and security

* Project foundation.
* Architecture.
* Local profile.
* Room baseline.
* Preferences.
* Six-digit PIN.
* Biometrics.
* Automatic lock.
* Onboarding.
* Settings foundation.
* Currency foundation.
* Navigation foundation.
* Initial tests.

## Stage 2 — Periods, accounts, categories, and opening balances

## Stage 3 — Income, expenses, transfers, movement list, and movement detail

## Stage 4 — Budgets, rollover, projections, dashboard, and purchase simulation

## Stage 5 — Credit cards, statements, partial payments, MSI, and MCI

## Stage 6 — Recurring items, occurrence versioning, alerts, and notifications

## Stage 7 — General debts and payoff simulations

## Stage 8 — Investments, returns, savings goals, and investment reporting

## Stage 9 — Drill-down reports, Power BI CSV export, encrypted backup, and restoration

Only implement the stage explicitly requested in the current task.

Do not pre-implement later business features simply because they appear in this master specification.

However, do not make architectural decisions that clearly prevent later stages.

---

# 35. Required behavior when receiving an implementation task

Before modifying code:

1. Inspect the current repository.
2. Identify the existing architecture and versions.
3. Reuse compatible current dependencies.
4. Do not arbitrarily upgrade Gradle, Kotlin, Android Gradle Plugin, Compose, or other libraries.
5. State any material conflict between the repository and this specification.
6. Implement only the requested scope.

After modifying code:

1. Run the relevant build.
2. Run tests.
3. Run static checks already configured in the project.
4. Summarize files created or modified.
5. Explain important design decisions.
6. Report commands executed and their results.
7. Report any incomplete item honestly.
8. Do not commit or push changes.

This master specification must remain the source of truth unless the owner explicitly changes a requirement.
