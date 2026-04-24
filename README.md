# Bledna Gestion — Waste Management System

A JavaFX 17 desktop application for managing **Waste Collection** and **Prévue Collection** data using a MySQL database (XAMPP/MariaDB).

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 17.0.18 |
| Apache Maven | 3.8+ |
| XAMPP (MariaDB) | 10.4+ |

---

## 1 — Database Setup

1. **Start XAMPP** → start **Apache** and **MySQL**.
2. Open **phpMyAdmin** → `http://localhost/phpmyadmin`
3. Create a new database named **`bledna`**.
4. Import **`bledna.sql`** (the schema).
5. Import **`seed.sql`** (the static user + sample data).

---

## 2 — Run the Application

```bash
cd  c:\Users\melek\Desktop\BLEDNA_AZMI_GESTION
mvn javafx:run
```

> Make sure Maven (`mvn -v`) is available in your PATH.  
> The first run downloads dependencies (~30 MB). Subsequent runs are instant.

---

## 3 — Import into IntelliJ IDEA (alternative)

1. **File → Open** → select the project folder.
2. IntelliJ detects `pom.xml` → click **"Load Maven Project"**.
3. Run `MainApp.java` directly (right-click → Run).

> If you see *"Error: JavaFX runtime components are missing"*, add VM options:  
> `--module-path <PATH_TO_JAVAFX_SDK>/lib --add-modules javafx.controls,javafx.fxml`

---

## 4 — Application Features

### Sidebar
| Button | View |
|--------|------|
| 🗑  Waste Collection | Full Waste CRUD |
| 📋  Prévue Collection | Full Prévue CRUD |

### Waste Collection
- List all waste collections in a sortable table
- **Status badges**: PENDING / IN_PROGRESS / COMPLETED / CANCELLED
- **Type badges**: PLASTIC / ORGANIC / METAL / GLASS / ELECTRONIC / OTHER
- **New** → form dialog with all fields (location, type, qty, unit, date, GPS, status, description)
- **Edit** → pre-filled dialog
- **Delete** → confirmation dialog

### Prévue Collection
- List all prévue collections
- Linked to a Waste Collection via dropdown
- Same CRUD flow

---

## 5 — Project Structure

```
BLEDNA_AZMI_GESTION/
├── pom.xml
├── bledna.sql          ← Database schema
├── seed.sql            ← Static user + sample data
└── src/main/
    ├── java/com/bledna/
    │   ├── MainApp.java
    │   ├── db/DatabaseConnection.java
    │   ├── model/
    │   │   ├── WasteCollection.java
    │   │   └── PrevueCollection.java
    │   ├── dao/
    │   │   ├── WasteCollectionDAO.java
    │   │   └── PrevueCollectionDAO.java
    │   ├── controller/
    │   │   ├── MainController.java
    │   │   ├── WasteController.java
    │   │   └── PrevueController.java
    │   └── util/AlertUtil.java
    └── resources/com/bledna/
        ├── main.fxml
        ├── waste_view.fxml
        ├── prevue_view.fxml
        └── styles.css
```

---

## 6 — Static User

The app uses **collector_id = 1** (Azmi Bledna) for all new Waste Collection entries.  
This user is created by `seed.sql`.

---

## DB Connection (DatabaseConnection.java)

```
URL  : jdbc:mysql://localhost:3306/bledna
User : root
Pass : (blank — XAMPP default)
```

To change credentials, edit `src/main/java/com/bledna/db/DatabaseConnection.java`.
