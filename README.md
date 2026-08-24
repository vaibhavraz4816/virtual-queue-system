# Virtual Queue & Token System

Join a shop's queue from your phone, watch your position and estimated
wait update live, and get called automatically — with a built-in
no-show handler that skips you and moves the queue on if you miss your
turn.

Built with core Java EE: **Servlets + JSP (JSTL/EL, no scriptlet
spaghetti) + JDBC**, backed by MySQL.

---

## Why this project is more than a CRUD app

Two pieces of real logic sit underneath the forms:

1. **Live wait-time estimation.** A customer's estimated wait is never
   stored — it's recalculated on every poll from `people ahead × the
   shop's average service time`, so it's always accurate as the queue
   moves (see `TokenDAO.countPeopleAhead` and `TokenStatusServlet`).

2. **A background no-show scheduler.** `AutoSkipListener` starts a
   `ScheduledExecutorService` when the app boots. Every 30 seconds it
   checks for any customer whose 5-minute grace period has expired
   after being called, automatically marks them `SKIPPED`, and advances
   the queue — no manual intervention needed. This is what makes the
   queue self-healing instead of just a fancy list.

Token numbering and the "call next" transition are wrapped in a JVM
lock + a real JDBC transaction (see `TokenDAO`) so two customers
joining at once, or a shop owner double-clicking "Call Next", can't
corrupt the queue.

---

## Tech stack

| Layer      | Technology                                           |
|------------|-------------------------------------------------------|
| Controller | Java Servlets (`javax.servlet`, annotation-based)     |
| View       | JSP + JSTL/EL (no scriptlets except one line per page for the `<title>`) |
| Data       | JDBC + MySQL, hand-written DAO classes                |
| Security   | BCrypt password hashing (`jBCrypt`), `PreparedStatement` everywhere |
| JSON API   | Gson, for the two AJAX polling endpoints              |
| Background job | `ScheduledExecutorService` via a `ServletContextListener` |
| Build      | Maven (WAR packaging)                                 |

---

## Project structure

```
virtual-queue-system/
├── pom.xml
├── database/
│   └── schema.sql                 <- run this first
├── src/main/resources/
│   └── db.properties              <- edit with your MySQL credentials
└── src/main/
    ├── java/com/queueapp/
    │   ├── model/                 Shop.java, Token.java
    │   ├── dao/                   ShopDAO.java, TokenDAO.java  <- core logic lives here
    │   ├── dto/                   JSON response shapes for the AJAX endpoints
    │   ├── servlet/                14 servlets (auth, queue, dashboard, JSON APIs)
    │   ├── listener/              AutoSkipListener.java        <- the background scheduler
    │   └── util/                  DBConnection.java, PasswordUtil.java
    └── webapp/
        ├── index.jsp, login.jsp, register.jsp
        ├── WEB-INF/web.xml
        ├── WEB-INF/jsp/           shops, shop_details, my_token, dashboard, display, error
        ├── WEB-INF/jsp/common/    header.jspf / footer.jspf (shared chrome)
        └── css/style.css
```

---

## Setup

### Prerequisites
- JDK 11+
- Maven 3.6+
- MySQL 8.x
- Apache Tomcat 9.x

### 1. Create the database
```bash
mysql -u root -p < database/schema.sql
```
This creates `virtual_queue_db`, the `shops`/`tokens` tables, and a demo
shop account (`demo_clinic` / `demo123`) so you have something to click
on immediately.

### 2. Configure your DB credentials
Edit `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/virtual_queue_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=your_mysql_password
```

### 3. Build
```bash
mvn clean package
```
This produces `target/virtual-queue-system.war`.

### 4. Deploy
Drop the WAR into Tomcat's `webapps/` folder (or deploy it from your
IDE — Eclipse/IntelliJ with a Tomcat server works fine too), then visit:
```
http://localhost:8080/virtual-queue-system/
```

---

## Walking through the demo

1. Open the site → **Find a Queue to Join** → pick "Sunrise Family
   Clinic" → join with your name → you land on your live token page.
2. In another browser tab, go to **Shop Login** and log in as
   `demo_clinic` / `demo123` → you're on the owner dashboard.
3. Click **Call Next** — watch your customer tab flip to "You're up!"
   within a few seconds, without refreshing.
4. Open **Open Public Display** from the dashboard — this is the
   big-screen "Now Serving" board you'd put on a TV at the counter.
5. To see the auto-skip in action: join the queue, get called, then
   just wait 5 minutes without clicking anything on the dashboard —
   the background job will skip you and call the next customer on its
   own. (Feel free to shrink `GRACE_PERIOD_MINUTES` in `TokenDAO.java`
   to something like 1 minute while demoing this.)

---

## Design decisions worth mentioning in an interview

- **Why derive wait time instead of storing it?** A stored estimate
  goes stale the moment anyone ahead in line gets served or skipped.
  Deriving it fresh on every request trades a slightly heavier query
  for correctness — the right call at this scale.
- **Why a JVM lock instead of just relying on the DB transaction?**
  Because two near-simultaneous `INSERT`s could both read the same
  `MAX(token_number)` before either commits, producing duplicate
  token numbers. The lock plus transaction closes that window. It's
  documented in `TokenDAO` as a known simplification — the natural
  upgrade for a multi-instance deployment is a `SELECT ... FOR UPDATE`
  or a DB sequence.
- **Why does the public `/api/queueStatus` endpoint only expose token
  numbers, never names or phone numbers?** It's unauthenticated by
  design (anyone can point a browser at the display screen), so it
  deliberately returns the minimum data needed.
- **Why BCrypt instead of storing/hashing passwords another way?**
  Salted, adaptive hashing — resistant to rainbow tables and tunable
  work factor as hardware improves.

---

## Possible extensions (good "future work" talking points)

- SMS notifications (Twilio) instead of relying on the customer
  keeping a browser tab open.
- Multiple counters/staff per shop, each pulling from the same queue.
- Analytics: average wait time by day/hour, no-show rate by shop.
- Swap the single-JVM lock for `SELECT ... FOR UPDATE` to support
  horizontal scaling across multiple app server instances.
- Connection pooling (HikariCP) instead of a raw `DriverManager`
  connection per request.

---

## Suggested resume bullet points

> Built a full-stack queue management system (Java Servlets, JSP,
> JDBC, MySQL) that lets customers join a shop's queue remotely and
> tracks live wait times, reducing physical wait-room crowding.

> Designed a background scheduling service (`ScheduledExecutorService`)
> that automatically detects and skips no-show customers, keeping the
> queue moving without staff intervention.

> Implemented BCrypt password hashing and parameterized JDBC queries
> throughout to prevent SQL injection and protect shop owner credentials.

> Built two polling JSON REST endpoints consumed by vanilla JS on the
> front end for live queue updates, without any client-side framework.

## About the Developer

Built by **Vaibhav**, MCA (Vellore Institute of Technology). This project also underpins an academic dissertation on reinforcement-learning-based adaptive fitness training.

Feel free to connect:

[![LinkedIn](https://www.linkedin.com/in/vaibhav-singh-b7363a211/)](#)

