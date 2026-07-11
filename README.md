# 🚦 TRAFFIC_NAVIGATOR

> A Smart Traffic Command & Control System that provides real-time traffic monitoring, signal management, incident tracking, analytics, and dashboard visualization through an interactive web interface.

---

## 📖 Overview

TRAFFIC_NAVIGATOR is a full-stack smart traffic management platform designed to simplify the monitoring and control of city traffic infrastructure.

The application enables traffic operators to monitor junctions, manage traffic signals, track incidents, visualize congestion, receive alerts, and analyze traffic data through a centralized dashboard.

---

## ✨ Features

- 🚦 Real-time Traffic Monitoring
- 🛣️ Traffic Signal Control
- 📊 Interactive Dashboard
- 📈 Traffic Analytics
- 🚨 Incident Management
- 🔔 Alert Management
- 🗺️ Interactive Traffic Map
- 📍 Junction Details
- ⚡ Real-time updates using WebSockets
- 📦 REST APIs powered by Spring Boot

---

# 🏗️ Architecture

```
                    +----------------------+
                    |   Angular Frontend   |
                    +----------+-----------+
                               |
                          REST APIs
                               |
                    +----------▼-----------+
                    | Spring Boot Backend  |
                    +----------+-----------+
                               |
                +--------------+--------------+
                |                             |
        PostgreSQL / MySQL             WebSocket Server
                |                             |
                +--------------+--------------+
                               |
                     Traffic Monitoring Engine
```

---

# 🛠️ Tech Stack

## Frontend

- Angular 19
- TypeScript
- Tailwind CSS
- Chart.js
- Leaflet Maps
- RxJS
- STOMP WebSocket

## Backend

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- Spring WebSocket
- Maven

## Database

- MySQL / PostgreSQL (JPA Supported)

---

# 📂 Project Structure

```
TRAFFIC_NAVIGATOR
│
├── BACKEND
│   └── smart-traffic-management
│       ├── controller
│       ├── service
│       ├── repository
│       ├── model
│       ├── scheduler
│       ├── engine
│       ├── integration
│       └── config
│
└── traffic-control-ui
    ├── components
    ├── services
    ├── models
    ├── environments
    └── assets
```

---

# ⚙️ Backend Modules

The backend exposes REST APIs for:

- Dashboard
- Traffic Management
- Signal Control
- Analytics
- Incident Management
- Alert Management

It also includes:

- Traffic Engine
- Scheduled Tasks
- WebSocket Communication
- Exception Handling
- JPA Repository Layer

---

# 🎨 Frontend Modules

The Angular application contains dedicated pages for:

- Dashboard
- Traffic Map
- Analytics
- Alerts
- Incidents
- Junction Details
- Signal Lights
- Sidebar Navigation
- Header Components

---

# 🚀 Getting Started

## Clone Repository

```bash
git clone https://github.com/CODE-X-ABHIJIT/TRAFFIC_NAVIGATOR.git

cd TRAFFIC_NAVIGATOR
```

---

## Backend Setup

Navigate to backend

```bash
cd BACKEND/smart-traffic-management
```

Run

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

Backend runs on

```
http://localhost:8080
```

---

## Frontend Setup

Navigate to frontend

```bash
cd traffic-control-ui
```

Install dependencies

```bash
npm install
```

Run

```bash
ng serve
```

Frontend runs on

```
http://localhost:4200
```

---

# 📊 System Capabilities

- Live traffic visualization
- Signal status monitoring
- Congestion analytics
- Junction health monitoring
- Incident reporting
- Alert notifications
- Dashboard statistics
- Real-time communication using WebSockets

---

# 📸 Screenshots

> Add screenshots here

```
Dashboard

Traffic Map

Analytics

Signal Control

Incident Management
```

---

# 🔮 Future Enhancements

- AI-based traffic prediction
- Emergency vehicle priority routing
- Automatic signal optimization
- CCTV integration
- Google Maps integration
- Mobile application
- Role-Based Access Control (RBAC)
- Docker deployment
- Kubernetes deployment
- CI/CD Pipeline

---

# 🤝 Contributing

Contributions are welcome.

1. Fork the repository

2. Create a feature branch

```bash
git checkout -b feature-name
```

3. Commit your changes

```bash
git commit -m "Added new feature"
```

4. Push your branch

```bash
git push origin feature-name
```

5. Create a Pull Request

---

# 📜 License

This project is intended for educational and development purposes.

---

# 👨‍💻 Author

**Abhijit Sahu**

GitHub: https://github.com/CODE-X-ABHIJIT

LinkedIn: *https://www.linkedin.com/in/abhijitsahu570*

---

## ⭐ Support

If you found this project useful, consider giving it a **Star ⭐** on GitHub.
