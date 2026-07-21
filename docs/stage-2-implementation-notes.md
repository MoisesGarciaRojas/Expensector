# Stage 2 Implementation Notes

Stage 2 stores budget-period dates as `LocalDate` values converted to ISO-8601 text by Room. Period boundaries and payment dates are calendar dates, not truncated instants.

Persisted period status uses the stable `UPCOMING`, `CURRENT`, and `CLOSED` codes. The generator assigns a display-oriented status for generated records, but it does not automatically close periods as an irreversible lifecycle action.

Money is represented by `MoneyMinor`: integer minor units plus ISO 4217 currency code. MXN `$1.00` is stored as `100`; persisted monetary values do not use `Float`, `Double`, or SQLite `REAL`.

Semimonthly period IDs are deterministic from configuration ID and period key, while Room also enforces unique `(configurationId, periodKey)`. Re-running generation for the same range is idempotent.

Opening balances are separate records. Asset accounts interpret positive amounts as money owned; credit cards interpret positive amounts as opening outstanding debt.
