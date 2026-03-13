<div align="center">

<img src="https://img.shields.io/badge/SmartOps-Employee%20Workflow-4f7cff?style=for-the-badge&logoColor=white" alt="SmartOps"/>

# 🚀 SmartOps — Employee Workflow Automation

**A full-stack enterprise HR system built with Spring Boot + React.js**
JWT-secured · Role-based access · Leave management · Real-time dashboard
---
## ✨ Features

| Module | Capabilities |
|--------|-------------|
| 🔐 **Authentication** | JWT login, BCrypt passwords, role-based access control |
| 👥 **Employee Management** | Register, update, assign managers, department grouping |
| 📋 **Leave Management** | Apply, approve/reject, cancel, overlap detection |
| 📊 **Dashboard** | Live stats — headcount, leave summary, personal metrics |
| 🛡️ **3-Tier Roles** | Admin · Manager · Employee — each with scoped permissions |

---

## 🏗️ Architecture

```
smartops/
├── backend/                     ← Spring Boot (Java 17)
│   └── src/main/java/com/smartops/
│       ├── Models.java          ← JPA entities + enums
│       ├── Repositories.java    ← Spring Data JPA queries
│       ├── Services.java        ← Business logic
│       ├── Controllers.java     ← REST endpoints + DTOs
│       └── Security.java        ← JWT + Spring Security
│   └── src/main/resources/
│       └── application.properties
│
└── frontend/                    ← React 18
    └── src/
        ├── App.js               ← Router + AuthContext + Axios
        ├── Pages.js             ← All components
        └── styles.css           ← Dark theme UI
```

> **9 files. Full-stack. Production-ready.**

---

## ⚡ Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- MySQL 8+

### 1. Clone

```bash
git clone https://github.com/yourusername/smartops.git
cd smartops
```

### 2. Configure Database

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartops_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

### 3. Run Backend

```bash
cd backend
mvn spring-boot:run
```

> 🟢 Spring auto-creates all tables and seeds the default admin on first run.

### 4. Run Frontend

```bash
cd frontend
npm install
npm start
```

> App opens at **http://localhost:3000**

---

## 🔑 Default Credentials

| Role | Email | Password |
|------|-------|----------|
| **Admin** | admin@smartops.com | Admin@123 |

> ⚠️ Change the default password immediately in production.

---

## 🌐 API Reference

### Auth
| Method | Endpoint | Access |
|--------|----------|--------|
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/register` | Admin only |

### Employees
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | `/api/employees` | Admin, Manager |
| GET | `/api/employees/:id` | All |
| PUT | `/api/employees/:id` | Admin, Manager |
| DELETE | `/api/employees/:id` | Admin only |

### Leaves
| Method | Endpoint | Access |
|--------|----------|--------|
| POST | `/api/leaves/apply` | All |
| GET | `/api/leaves/my` | All |
| GET | `/api/leaves/team` | Manager, Admin |
| GET | `/api/leaves/pending` | Manager, Admin |
| GET | `/api/leaves/all` | Admin only |
| PUT | `/api/leaves/:id/review` | Manager, Admin |
| PUT | `/api/leaves/:id/cancel` | Owner only |

### Dashboard
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | `/api/dashboard/stats` | All |

---

## 🔐 Role Permissions

```
ROLE_ADMIN    → Full access — all employees, all leaves, register users
ROLE_MANAGER  → Team scope — view/approve team leaves, view team employees
ROLE_EMPLOYEE → Self scope — apply leave, view own history, personal dashboard
```

---

## 🗄️ Database Schema

```sql
users          → id, employee_id, first_name, last_name, email, password,
                 phone, department, designation, status, manager_id
roles          → id, name (ROLE_EMPLOYEE | ROLE_MANAGER | ROLE_ADMIN)
user_roles     → user_id, role_id
leave_requests → id, employee_id, leave_type, start_date, end_date,
                 total_days, reason, status, reviewed_by, review_comments
```

> JPA `ddl-auto=update` creates all tables automatically on first run.

---

## 🛠️ Tech Stack

**Backend**
- Java 17 + Spring Boot 3.2
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA + Hibernate
- MySQL 8 · Lombok · Maven

**Frontend**
- React 18 + React Router v6
- Axios (interceptors for JWT)
- React Hot Toast
- Pure CSS dark theme (no UI library)

---

## 📝 Environment Variables

| Key | Default | Description |
|-----|---------|-------------|
| `spring.datasource.url` | localhost:3306 | MySQL connection URL |
| `spring.datasource.username` | root | DB username |
| `spring.datasource.password` | — | DB password |
| `app.jwt.secret` | (set in props) | 256-bit HS256 secret |
| `app.jwt.expiration-ms` | 86400000 | Token TTL (24h) |
| `app.cors.allowed-origins` | localhost:3000 | React dev origin |

---

## 🚢 Production Checklist

- [ ] Change default admin password
- [ ] Set a strong JWT secret (32+ random chars)
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Use environment variables for credentials (never commit)
- [ ] Enable HTTPS
- [ ] Set `app.cors.allowed-origins` to your production domain

---

## 🤝 Contributing

1. Fork the repo
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit: `git commit -m 'Add amazing feature'`
4. Push: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

<div align="center">

Built with ❤️ using Spring Boot + React

⭐ Star this repo if you found it helpful!

</div>
