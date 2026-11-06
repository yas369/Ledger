# CLAUDE.md — SMS Ledger

This file is persistent context. Read it fully before every task. If a task
instruction contradicts this file, stop and ask which one wins.

---

## 1. What this is

An Android app that reads transactional SMS messages on the device, extracts
financial fields from them, and builds a local personal-finance ledger.

Everything happens on-device. There is no server, no account, no sync.

Distribution target is **Google Play** (decided 2026-11-06, replacing the earlier
sideload-only plan). Until the first Play release is being prepared, builds are
plain debug/signed APKs installed directly.

**Risk you must keep in view:** Play restricts `READ_SMS` / `RECEIVE_SMS`. It needs
a permission declaration form, a hosted privacy policy, and review, and Google may
refuse an expense tracker. Do not claim Play approval is likely. Do not add
Play-only work (Data Safety metadata, App Bundle config, declaration text) until
the owner asks for the release phase; Play Billing and Play Integrity are never
added.

---

## 2. Non-negotiable constraints

1. **No `INTERNET` permission.** It is not in the manifest and it never will be.
   If any library you propose requires network access at runtime, that library is
   rejected. This is a hard architectural boundary, not a preference — it makes
   data exfiltration structurally impossible and is the app's core privacy claim.
2. **No analytics, no crash reporting SDKs, no telemetry.** Crashes are read from
   logcat during development.
3. **No cloud ML.** All inference and all training happen on-device.
4. **SMS content never leaves the app's sandbox.** No content providers exported,
   no implicit intents carrying message bodies, no logging of raw message bodies
   at any log level other than a debug-only, explicitly-guarded logger.
5. **No fabricated bank SMS formats.** See §7 — this is the single most common way
   you will produce broken code here.

---

## 3. Stack

| Layer | Choice |
|---|---|
| Language | Kotlin (no Java sources) |
| UI | Jetpack Compose (foundation + ui only, no Material 3 dependency). Look and feel follow `design.md` in the owner's Ledger Project: iPhone style, custom theme and components. The iPhone look wins over Material defaults. |
| Min SDK | 26 |
| Target / Compile SDK | 36 |
| Architecture | MVVM + repository, unidirectional data flow |
| Async | Coroutines + Flow (no RxJava, no LiveData) |
| DI | Hilt |
| Database | Room (KSP, not KAPT) |
| Background work | WorkManager |
| Preferences | DataStore (Proto or Preferences — ask before choosing) |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) |
| Testing | JUnit4, Turbine, Robolectric where unavoidable, Room in-memory DB |

**Dependency rule:** you may not add a dependency that is not already in
`libs.versions.toml` without asking first. State what it does, its size, its
transitive deps, and what it would take to not use it.

---

## 4. Module and package layout

Single Gradle module (`:app`) until it demonstrably hurts. Package-by-feature
inside it:

```
dev.yas.ledger
├── ingest/          # SMS capture: receiver, observer, backfill, permissions
├── extract/         # regex rules, field parsing, extraction result types
├── classify/        # on-device classifier, feature encoding, learning store
├── ledger/          # transaction domain model, dedupe, aggregation
├── data/
│   ├── db/          # Room entities, DAOs, database, migrations
│   └── repo/        # repositories — the only thing UI layers may touch
├── ui/
│   ├── review/      # confirmation card / review queue
│   ├── ledger/      # transaction list, filters, summaries
│   ├── settings/
│   └── theme/
├── work/            # WorkManager workers
└── di/              # Hilt modules
```

Rules:
- `ui/` never imports from `data/db/` or `ingest/`. It talks to `data/repo/` only.
- `extract/` and `classify/` are pure Kotlin where possible — no Android imports,
  no `Context`. They must be unit-testable on the JVM with no emulator.
- Domain models in `ledger/` are separate types from Room entities. Map at the
  repository boundary. Do not annotate domain models with `@Entity`.

---

## 5. The processing pipeline

```
SMS arrives (BroadcastReceiver)  ─┐
                                  ├─> RawMessage (Room, immutable, never deleted)
Historical scan (ContentObserver) ┘
                                     │
                                     ▼
                          Sender allowlist / heuristic filter
                                     │  (drop OTPs, promos, non-financial)
                                     ▼
                          Regex extraction  ──> ExtractionResult
                                     │           (amount, direction, account
                                     │            tail, merchant, timestamp,
                                     │            per-field confidence)
                                     ▼
                          Classifier (category + is-transaction)
                                     │
                                     ▼
                    ┌────────────────┴────────────────┐
            high confidence                    low confidence
                    │                                 │
                    ▼                                 ▼
            auto-commit to ledger          Confirmation card (user reviews)
                    │                                 │
                    └────────────┬────────────────────┘
                                 ▼
                          Transaction (Room)
                                 │
                                 ▼
                    User correction ──> LearningExample (Room)
                                 │
                                 ▼
                    WorkManager periodic retrain (idle + charging)
```

Design invariants:
- `RawMessage` is the source of truth and is **append-only**. Every `Transaction`
  carries the id of the `RawMessage` it came from. Re-running extraction over
  history must be possible without losing user corrections.
- Every user correction writes a `LearningExample`. Corrections are never
  overwritten by a later model.
- Confidence thresholds live in one constants file, are named, and are
  documented with the reasoning for their value. Do not scatter magic numbers.

---

## 6. Known hard problems — do not paper over these

**OEM background restrictions.** Xiaomi (MIUI/HyperOS), Samsung, Oppo, Vivo, and
Realme aggressively kill background processes and silently drop broadcast
delivery. You cannot verify behaviour on these devices. You have no device.

When you touch anything in `ingest/`:
- Do not claim the receiver "will work reliably."
- Do implement the belt-and-braces design: `BroadcastReceiver` for live capture
  **plus** a `ContentObserver` **plus** a periodic `WorkManager` reconciliation
  pass that scans the SMS provider for anything missed since the last watermark.
- Do write the manual device test steps into `docs/DEVICE_TESTING.md` and tell me
  which ones I need to run.

**Dual-SIM, RCS, and OEM message apps** change what lands in the SMS provider.
Flag assumptions; don't hide them.

**Default-SMS-app status.** `RECEIVE_SMS` does not require being the default SMS
app; `SMS_DELIVER` does. We are not becoming the default SMS app. If you think a
feature requires it, stop and ask.

---

## 7. Rules of engagement

**Ask, don't assume.** Specifically, stop and ask me when:
- You need a real bank/UPI SMS format. **Never invent one.** Every regex you
  write must be derived from a sample I gave you, and the sample must be pasted
  into the test fixtures as an anonymized golden case. A regex written against an
  imagined format is worse than no regex — it fails silently in production.
- A task would add a dependency.
- A task would change a schema that already has data (migration decisions are mine).
- Two reasonable designs exist and the choice is not obvious from this file.

**Do not write placeholder code.** No `TODO()`, no stub functions that return
empty lists, no "this would call the real implementation" comments. If you can't
build something because you're missing input, stop and ask for the input.

**Scope discipline.** Do exactly the phase you were given. Do not build ahead. If
you notice something broken outside the current phase, note it at the end of your
response instead of fixing it.

**Verification honesty.** End every task with three sections:
1. *What I built* — files touched, one line each.
2. *What I verified* — commands you actually ran and their real output.
3. *What I could not verify* — anything needing a physical device, a real SMS, an
   OEM behaviour, or my input. Be specific. "Requires device testing" is useless;
   "the ContentObserver backfill has never been run against a provider with more
   than 0 rows" is useful.

Never report a build as passing unless you ran `./gradlew` and read the output.

**Commits.** One logical change per commit. Conventional commit messages
(`feat(ingest): ...`). Do not commit unless I ask.

---

## 8. Code style

- `ktlint`-compatible formatting, 4-space indent, 120-column soft limit.
- Explicit visibility on public API surfaces; prefer `internal` by default.
- Sealed interfaces for state and result types. No nullable-returns-as-error.
- Comments explain *why*, never *what*. Delete any comment that restates the line
  below it.
- Compose: stateless composables, state hoisted to ViewModel, `@Preview` for
  every screen-level composable.
- No `!!`. No `runBlocking` outside tests. No `GlobalScope`.

---

## 9. Testing expectations

Non-negotiable coverage:
- `extract/` — golden-file tests over an anonymized SMS corpus in
  `app/src/test/resources/sms_corpus/`. Every supported sender format gets at
  least one positive case and one near-miss negative case.
- `classify/` — deterministic tests with a fixed seed and a fixed training set.
- `ledger/` — dedupe logic, especially the "same transaction reported by both bank
  SMS and UPI app SMS" case.
- Room — migration tests for every schema version bump.

UI tests are optional until the UI stabilizes. Say so rather than writing brittle
ones.

---

## 10. Non-goals (do not build these)

- Multi-user, accounts, login
- Cloud sync or backup to a remote service
- Bill splitting, budgeting goals, investment tracking
- Notification-listener-based capture (SMS only, for now)
- Widgets, Wear OS, tablet-specific layouts
- Localization beyond English strings (but: no hardcoded strings in composables —
  use `strings.xml` from day one)

---

## 11. Placeholders to confirm with me

These are my defaults, not decisions. Confirm before relying on them:
- Application ID: `dev.yas.ledger`
- App display name: "Ledger"
- Currency: INR, single-currency assumption throughout
- Locale/timezone: `Asia/Kolkata`
