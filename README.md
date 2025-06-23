
# 📦 Inventory Management System (Java 21 + Spring Boot)

This project implements a backend system for managing warehouse inventory for a business, using **Java 21**, **Spring Boot**, and **PostgreSQL**. The inventory is updated solely through transactions, each acting as an immutable ledger entry (similar to accounting records). Corrections to errors are made by creating **reverse transactions**, not by modifying existing ones.

---

## ⚙️ Features

- Record new warehouse transactions (`POST`)
- Correct previous transactions with new reversal transactions (`PATCH`)
- Ensure accurate updates in stock levels
- Maintain full audit trail of inventory changes
- Uses **PostgreSQL** with Docker container setup
- Supports two Spring profiles: `test` and `postgresql`
- Includes **unit and integration tests**

---

## 🧱 Data Model

### Transaction Table

| Column           | Type   | Description                        |
| ---------------- | ------ | ---------------------------------- |
| `id`             | Long   | Auto-generated ID                  |
| `item_name`      | String | Name of the item (e.g. "Potatoes") |
| `quantity`       | Double | Amount (e.g. 14.2)                 |
| `unit`           | Enum   | Unit of measure (e.g. KG, LB)      |
| `price_per_unit` | Double | Price for one unit                 |
| `warehouse_name` | String | Name of the warehouse              |

### Inventory (Stock) Table

Same structure as `Transaction`, with a **composite unique constraint** on:

```text
[item_name, warehouse_name, price_per_unit]
```

---

## 📱 API Endpoints

### 1. `POST /api/v1/transactions`

Creates one or more **new transactions** and updates stock levels accordingly.

**Request Body:**

```json
[
  {
    "itemName": "Potatoes",
    "quantity": 15,
    "unit": "KG",
    "pricePerUnit": 1.0,
    "warehouseName": "Storage"
  }
]
```

**Response:** `201 Created`

---

### 2. `PATCH /api/v1/transactions`

Creates **correction transactions** for previously recorded entries.

**Request Body:**

```json
[
  {
    "originalTransactionId": 5,
    "itemName": "Potatoes",
    "quantity": 13,
    "unit": "KG",
    "pricePerUnit": 1.0,
    "warehouseName": "Storage"
  }
]
```

**Response:** `202 Accepted`

> ⚠ Existing transactions are **never updated directly**. Only new correction transactions are created.

---

## 🧪 Testing

The project includes comprehensive **unit tests** and **integration tests** to verify the correctness of both business logic and REST API endpoints. Testing frameworks used:

- JUnit 5
- MockMvc
- Testcontainers (PostgreSQL)

Use the `test` profile when running tests.

```bash
./mvnw test -Dspring.profiles.active=test
```

---

## 📂 Project Structure

```bash
inventory-management/
├── src/
│   ├── main/
│   │   ├── java/com/inventory/
│   │   │   ├── controller/              
│   │   │   ├── dto/                    
│   │   │   ├── exception/      
│   │   │   ├── model/               
│   │   │   ├── repository/              
│   │   │   ├── service/                 
│   │   │   └── InventoryManagementApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-postgresql.yml
│   │       ├── application-test.yml
│   │       └── schema.sql
│   │
│   └── test/java/com/inventory/
│       ├── controller/TransactionControllerIT.java
│       └── service/TransactionServiceTest.java
│           ├── StockRepositoryTest.java
│           └── TransactionRepositoryTest.java
│
├── docker-ompose.yml
├── pom.xml
└── README.md
```

---

## 🛠 Technologies Used

- Java 21
- Spring Boot
- PostgreSQL (Docker container)
- JUnit 5
- Testcontainers
- Maven

---

## 🚀 Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/your-username/inventory-management.git
   cd inventory-management
   ```

2. **Start PostgreSQL via Docker**

   ```bash
   docker-compose -f docker/docker-compose.yml up -d
   ```

3. **Run the application**

   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=postgresql
   ```
   
4. **Run tests**

   ```bash
   ./mvnw test -Dspring-boot.run.profiles=test
   ```

---

