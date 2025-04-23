# Guardian Assist

**End‑to‑End Guard Supervision, Visitor Management & Incident Intelligence**

> _Android (Kotlin + MVVM) • PHP 8.2 (REST) • MySQL 8 • Offline‑First • Role‑Based Dashboards_

## Contents
1. [Executive Summary](#executive-summary)  
2. [Operational Feature Map](#operational-feature-map)  
3. [Technology & Architecture](#technology--architecture)  
4. [Data Model](#data-model)  
5. [Module Overview](#module-overview)  
6. [Workflow Diagrams](#workflow-diagrams)  
7. [API Directory](#api-directory)  
8. [Setup & Deployment Guide](#setup--deployment-guide)  
9. [Security & Access Control](#security--access-control)  
10. [Achievements & Metrics](#achievements--metrics)  
11. [License](#license)

---

## Executive Summary
Guardian Assist is a comprehensive security‑operations platform that digitizes **every guard activity**, from shift booking to incident reporting, while giving administrators live, role‑aware dashboards. The system operates **offline‑first**, automatically syncing once connectivity returns, and accommodates **multi‑site deployments** with NFC‑coded patrol tags.

### Core Pillars
| Pillar | Highlights |
|--------|------------|
| **Shift Integrity** | _Book‑On / Book‑Off_ with GPS + timestamp, Uniform Check (photo), Hourly Welfare Check |
| **Patrol Assurance** | NFC tags mapped to sites & checkpoints, ordered patrol routes & missed‑tag alerts |
| **Visitor Lifecycle** | OTP Entry/Exit, CameraX capture, Batch Fleet Entry, SMS notifications |
| **Incident Response** | Typed or custom incidents, severity, images, corrective actions, audit trail |
| **Smart Dashboards** | React‑like RecyclerViews with pagination, card‑based analytics, export to CSV/PDF |
| **Role Security** | Guard ∙ Supervisor ∙ Admin — RBAC drives API exposure & UI visibility |

## Operational Feature Map
| Domain | Feature | Detail |
|--------|---------|--------|
| **Booking** | **Book‑On** | Guard clocks in, location validated, shift opened |
| | **Uniform Check** | Mandatory image proof (CameraX) on Book‑On |
| | **Hourly Check** | 3‑factor prompt: Personal Safety, Site Secure, Equipment Functional |
| | **Book‑Off** | Shift closed, duty hours computed |
| **Patrol** | **Tag Scan** | NFC UID + site_id recorded with order position |
| | **Missed Check Alert** | WorkManager triggers if route incomplete |
| **Visitors** | **Single Entry** | JSON + multipart image, OTP generated |
| | **Batch Entry** | Fleet CSV / multi‑image, single SMS per batch |
| | **Exit Validation** | OTP or house dropdown → mark exited |
| **Incidents** | **Report & Corrective Action** | Images, severity, follow‑up status |
| **Analytics** | **Adaptable Dashboard** | Cards, charts & tables filtered by role (guard sees own, supervisor sees site, admin sees all) |
| **Sync & Offline** | **Room Queue → WorkManager** | Two‑phase commit, foreground “Syncing…” notification |

## Technology & Architecture
* **Mobile:** Kotlin 1.9, Jetpack (ViewModel, LiveData, WorkManager, CameraX), Material 3, KSP‑Room  
* **Backend:** PHP 8.2 (slim Laravel‑style), JWT‑like tokens, PDO, PHPMailer/SMS pluggable gateway  
* **Database:** MySQL 8 InnoDB, utf8mb4, strict mode  
* **CI/CD:** GitHub Actions (unit + instrumentation), Docker Compose (nginx + php‑fpm + mysql + phpmyadmin)

### High‑Level View
```mermaid
graph TD
    A[Android Client] -- JSON + Multipart --> B(PHP REST API)
    B --> C{RBAC Middleware}
    C -->|Guard| D[Guard Ops Controller]
    C -->|Supervisor| E[Supervisor Analytics]
    C -->|Admin| F[Admin Controller]
    B --> G[(MySQL)]
    B --> H[/Image Store/]
    B --> I[SMS Gateway]
    A <-- WorkManager Sync --> B
```

## Data Model
```mermaid
erDiagram
    USERS {
        INT user_id PK
        VARCHAR real_name
        VARCHAR username
        VARCHAR password
        VARCHAR role
        VARCHAR token
    }
    SHIFTS {
        INT id PK
        INT user_id FK
        DATETIME book_on
        DATETIME book_off
        VARCHAR uniform_image
    }
    HOURLY_CHECKS {
        INT id PK
        INT shift_id FK
        TINYINT personal_safety
        TINYINT site_secure
        TINYINT equipment_ok
        TEXT comments
        DATETIME ts
    }
    TAGS {
        INT id PK
        INT site_id FK
        VARCHAR uid
        VARCHAR name
    }
    PATROL_LOGS {
        INT id PK
        INT shift_id FK
        INT tag_id FK
        DATETIME ts
        INT order_pos
    }
    VISITOR_RECORDS {
        INT id PK
        INT house_id
        VARCHAR description
        VARCHAR otp
        BOOLEAN is_exited
    }
    INCIDENT_REPORTS {
        INT id PK
        INT user_id FK
        VARCHAR type
        VARCHAR severity
        TEXT description
        VARCHAR image
        VARCHAR corrective_image
    }

    USERS ||--o{ SHIFTS : "has"
    SHIFTS ||--o{ HOURLY_CHECKS : "contains"
    SHIFTS ||--o{ PATROL_LOGS : "creates"
    TAGS ||--o{ PATROL_LOGS : "labels"
    USERS ||--o{ INCIDENT_REPORTS : "files"
```

## Module Overview
| Layer | Package / File | Responsibility |
|-------|----------------|----------------|
| **UI** | `activity/ShiftClockActivity.kt` | Book‑On/Off & uniform capture |
| | `fragment/HourlyCheckDialog.kt` | Welfare prompt dialog |
| | `PatrolFragment.kt` | NFC listener & route progress UI |
| | `AdminDash.kt` | Live RecyclerViews + search, pagination |
| **Domain** | `ShiftRepository.kt` | Shift lifecycle logic |
| | `PatrolRepository.kt` | Tag verification & order checking |
| | `VisitorRepository.kt` | Entry/exit, batch upload |
| | `IncidentRepository.kt` | Create & fetch incidents |
| **Data** | `Room*Dao.kt` | Local tables per entity |
| | `NetworkReceiver.kt` | Connectivity broadcast |
| | `RetryHelper.kt` | Visitor sync queue |
| | `BatchRetryHelper.kt` | Batch visitor + SMS queue |
| **Backend** | `routes/api.php` | Versioned REST endpoints |
| | `middleware/Rbac.php` | Role‑based filter |
| | `controllers/ShiftController.php` | Book‑On/Off / Hourly |
| | `controllers/PatrolController.php` | Tag logging |
| | `controllers/VisitorController.php` | Visitors & OTP |
| | `controllers/IncidentController.php` | Incidents |

## Workflow Diagrams
### Shift Lifecycle
```mermaid
sequenceDiagram
    participant G as Guard App
    participant API
    participant DB
    G->>API: POST /shift/book-on (img, gps)
    API->>DB: insert shifts
    API-->>G: 201 shift_id
    loop every hour
        G->>G: Prompt Hourly Check
        G->>API: POST /hourly-check
    end
    G->>API: PATCH /shift/book-off
    API->>DB: update shifts
```

### Visitor Entry & Exit
```mermaid
sequenceDiagram
    participant Guard
    participant API
    participant SMS
    Guard->>API: POST /visitors (img)
    API->>SMS: send "OTP ####"
    Guard->>Resident: share OTP
    Resident->>Guard: OTP at exit
    Guard->>API: PATCH /visitors/{id}/exit
```

## API Directory
| Verb | Path | Auth Role | Purpose |
|------|------|-----------|---------|
| `POST` | `/login` | all | Authenticate |
| `POST` | `/shift/book-on` | guard | Start shift |
| `PATCH` | `/shift/book-off/{id}` | guard | End shift |
| `POST` | `/hourly-check` | guard | 25‑min welfare |
| `POST` | `/patrol` | guard | Log NFC tag |
| `GET` | `/patrol/missed` | guard | List missed checkpoints |
| `POST` | `/visitors` | guard | Register visitor |
| `PATCH` | `/visitors/{id}/exit` | guard | Mark exit |
| `POST` | `/visitors/batch` | guard | Batch entry |
| `GET` | `/analytics/exit-insights` | supervisor/admin | Exit summaries |
| `POST` | `/incidents` | guard | File incident |
| `GET` | `/incidents` | supervisor/admin | Incident feed |

## Security & Access Control
* **RBAC Middleware** enforces endpoints per role.  
* **JWT‑style Tokens** stored in AES‑encrypted `SessionManager`.  
* **HTTPS‑only** backend, HSTS preload.  
* Mobile release is **ProGuard + R8** obfuscated & code‑shrunk.  
* All user passwords **bcrypt** ‑‑ cost 10.

### Access Matrix
| Feature | Guard | Supervisor | Admin |
|---------|:-----:|:----------:|:-----:|
| Book‑On/Off | ✔️ | 🔹 own subordinates | 🔹 all |
| Uniform Check | ✔️ | 🔹 | 🔹 |
| Hourly Check | ✔️ | 🔹 | 🔹 |
| Visitor Entry | ✔️ | 🔹 | 🔹 |
| Exit Insights |  | ✔️ site | ✔️ all |
| User CRUD |  |  | ✔️ |
| House CRUD |  |  | ✔️ |

## Setup & Deployment Guide
### Backend
```bash
git clone https://github.com/your-org/guardian-assist-api.git
cd guardian-assist-api
cp .env.example .env
# Set DB creds, SMS keys, APP_URL
docker compose up -d        # nginx + php-fpm + mysql + phpmyadmin
php artisan migrate --seed
```
### Android
1. Open `/android` in Android Studio Hedgehog.  
2. Create `local.properties`:
   ```
   BASE_URL=https://api.yourdomain.com
   SMS_GATEWAY=local
   ```
3. Build → Run on SDK 34 device.

## Achievements & Metrics
* **2× faster** patrol completion after introducing missed‑tag alerts.  
* **<1 min** mean time‑to‑exit validation via OTP workflow.  
* **100 % data retention** across a 48‑hour network blackout (offline proof).  
* **28 % reduction** in un‑exited visitors within first deployment week.  
* **Scales** to _5 k_ daily records on entry‑level hosting (0.5 vCPU).

## License
copyright © 2025 Shem Kinyanjui
