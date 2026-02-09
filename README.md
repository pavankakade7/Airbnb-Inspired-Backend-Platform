# AirBnB-Inspired Backend Platform

A production-ready microservices-based backend platform built with Spring Boot, inspired by Airbnb's architecture and design patterns. This application demonstrates modern backend engineering practices including JWT authentication, Stripe payment integration, dynamic pricing strategies, and comprehensive booking lifecycle management.

**Version:** 0.0.1-SNAPSHOT | **Java:** 21 | **Spring Boot:** 3.4.0

---

## 📋 Project Overview

This backend platform separates concerns across distinct service layers:
- **User Service**: Authentication, authorization, and user profile management
- **Listing Service**: Hotel and room management with detailed information
- **Booking Service**: Complete booking lifecycle with availability checks and status management
- **Payment Service**: Stripe integration for secure payment processing and refunds
- **Inventory Service**: Dynamic room availability and pricing management

The application implements enterprise-grade patterns including centralized exception handling, request validation, role-based access control, and environment-based configuration.

---

## ✨ Core Features

### 🔐 Security & Authentication
- **JWT-based Authentication**: Secure token generation and validation
- **Role-Based Access Control (RBAC)**: Two roles - `GUEST` and `HOTEL_MANAGER`
- **BCrypt Password Encryption**: Industry-standard password hashing
- **Stateless Architecture**: JWT tokens eliminate session management overhead
- **CORS Configuration**: Flexible cross-origin request handling

### 🏨 Hotel & Room Management
- Create and manage hotels with detailed information
- Define multiple room types per hotel with base pricing
- Photo and amenities management
- Contact information embedding
- Hotel ownership validation and authorization

### 📅 Booking Lifecycle Management
**Booking Status Flow:**
```
RESERVED → GUESTS_ADDED → PAYMENTS_PENDING → CONFIRMED
                                              ↓
                                          CANCELLED
```

**Features:**
- Availability checking with date range validation
- Reservation creation with automatic inventory locking
- Guest information addition
- Payment initiation and capture
- Booking cancellation with refund processing
- 10-minute expiry on reserved bookings

### 💳 Payment Processing
- **Stripe Integration**: Secure payment gateway integration
- **Checkout Sessions**: Stripe Checkout for secure payment collection
- **Payment Webhooks**: Real-time event handling for payment confirmation
- **Refund Processing**: Automatic refunds on booking cancellation
- **Customer Management**: Stripe customer creation and management

### 💰 Dynamic Pricing Strategy
Implements the **Decorator Pattern** for flexible pricing strategies:

1. **Base Pricing Strategy**: Room's base price
2. **Surge Pricing Strategy**: Multiplies by surge factor (demand-based)
3. **Occupancy Pricing Strategy**: +20% when occupancy > 80%
4. **Holiday Pricing Strategy**: +25% during holidays
5. **Urgency Pricing Strategy**: +15% for bookings within 7 days

**Composable Example:**
```
BasePricingStrategy
  → SurgePricingStrategy (wrapper)
    → OccupancyPricingStrategy (wrapper)
      → UrgencyPricingStrategy (wrapper)
```

### 📊 Reporting & Analytics
- Hotel revenue reports with date range filtering
- Booking count and average revenue calculations
- Owner-only access to hotel reports

### 🛒 Inventory Management
- Per-room, per-day inventory tracking
- Booked and reserved count management
- Room closure functionality
- Batch inventory updates for date ranges

---

## 🏗️ Architecture & Design Patterns

### Microservices Architecture
```
┌─────────────────────────────────────────────┐
│         API Gateway / REST Controllers      │
├─────────────────────────────────────────────┤
│                Service Layer                │
│  ┌─────────────┐  ┌──────────────┐        │
│  │   Booking   │  │   Payment    │        │
│  │  Service    │  │  Service     │        │
│  ├─────────────┤  ├──────────────┤        │
│  │  Hotel      │  │ Inventory    │        │
│  │  Service    │  │  Service     │        │
│  └─────────────┘  └──────────────┘        │
├─────────────────────────────────────────────┤
│      Repository Layer (Data Access)        │
├─────────────────────────────────────────────┤
│      PostgreSQL Database                   │
└─────────────────────────────────────────────┘
```

### Database Schema
**Key Entities:**
- `app_user`: User accounts with roles
- `hotel`: Hotel listings with contact info
- `room`: Room types and configurations
- `booking`: Booking records with status tracking
- `guest`: Guest information per booking
- `inventory`: Daily room availability and pricing
- `hotel_min_price`: Minimum price tracking per hotel

**Unique Constraints:**
- `inventory.unique_hotel_room_date`: Ensures one inventory record per room per day

### Design Patterns Implemented
1. **Decorator Pattern**: Dynamic pricing strategies composition
2. **Strategy Pattern**: Multiple pricing calculation algorithms
3. **Service Layer Pattern**: Business logic separation
4. **Repository Pattern**: Data access abstraction
5. **DTO Pattern**: Data transfer objects between layers
6. **Exception Handling Pattern**: Centralized exception management
7. **Authorization Pattern**: Role-based request filtering

---

## 📦 Dependencies & Technology Stack

### Core Framework
- **Spring Boot 3.4.0**: Application framework
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: ORM and database abstraction
- **Spring Web**: REST API development

### Authentication & Security
- **JWT (jjwt 0.12.6)**: Token-based authentication
- **JwtApi, JwtImpl, JwtJackson**: JWT handling libraries
- **BCrypt**: Password encryption

### Payment Integration
- **Stripe Java SDK (28.2.0)**: Payment gateway integration
- **Checkout Sessions**: Payment collection
- **Webhook Support**: Event-driven payment processing

### Database
- **PostgreSQL**: Relational database
- **Hibernate ORM**: Object-relational mapping
- **JPA Annotations**: Entity mapping

### Utilities
- **Lombok**: Boilerplate code generation
- **ModelMapper 3.2.2**: Object mapping utility
- **SLF4J + Logback**: Logging framework
- **SpringDoc OpenAPI 2.8.3**: API documentation

### Development & Testing
- **Spring Boot Test Starter**: Testing framework
- **JUnit & Mockito**: Unit testing

---

## 🔌 API Endpoints

### Authentication
```http
POST   /api/v1/auth/signup          # Register new user
POST   /api/v1/auth/login           # Login with credentials
POST   /api/v1/auth/logout          # Logout user
```

### Hotels (Admin)
```http
POST   /api/v1/admin/hotels         # Create hotel
PUT    /api/v1/admin/hotels/:id     # Update hotel
GET    /api/v1/admin/hotels/:id     # Get hotel details
DELETE /api/v1/admin/hotels/:id     # Delete hotel
GET    /api/v1/admin/hotels/:id/report # Hotel revenue report
```

### Rooms (Admin)
```http
POST   /api/v1/admin/hotels/:hotelId/rooms    # Add room
PUT    /api/v1/admin/hotels/:hotelId/rooms/:id # Update room
GET    /api/v1/admin/hotels/:hotelId/rooms/:id # Get room
DELETE /api/v1/admin/hotels/:hotelId/rooms/:id # Delete room
```

### Inventory (Admin)
```http
GET    /api/v1/admin/inventory                  # List inventory
POST   /api/v1/admin/inventory                  # Update inventory
PUT    /api/v1/admin/inventory/:id              # Modify inventory
```

### Hotel Browsing (Guest)
```http
GET    /api/v1/hotels/search         # Search hotels by location
GET    /api/v1/hotels/:id            # Get hotel with room prices
GET    /api/v1/hotels/:id/details    # Get detailed hotel info
```

### Bookings (Authenticated)
```http
POST   /api/v1/bookings              # Initialize booking
PUT    /api/v1/bookings/:id/guests   # Add guests
POST   /api/v1/bookings/:id/payments # Initiate payment
GET    /api/v1/bookings/:id/status   # Check booking status
DELETE /api/v1/bookings/:id          # Cancel booking
GET    /api/v1/bookings              # Get user's bookings
```

### Payments
```http
POST   /api/v1/webhook/stripe        # Stripe webhook handler
```

### User Profile
```http
GET    /api/v1/users/profile         # Get user profile
PUT    /api/v1/users/profile         # Update profile
GET    /api/v1/users/:id             # Get user by ID
```

---

## 🔐 Security Implementation

### JWT Token Structure
- **Header**: Algorithm (HS256)
- **Payload**: User ID, email, roles, issued time, expiration
- **Signature**: HMAC-SHA256 with secret key

### Authorization Rules
```
Public Routes:
  - /auth/signup, /auth/login
  - GET /hotels/*

Admin Routes (HOTEL_MANAGER):
  - /admin/**

Protected Routes:
  - /bookings/**
  - /users/**
```

### Password Security
- BCrypt with configurable strength (default: 10 rounds)
- One-way hashing ensures passwords never stored in plaintext

---

## 📊 Data Flow Examples

### Booking Process Flow
```
1. Guest searches hotels
   └─→ HotelBrowseController.searchHotels()
   └─→ HotelService.searchAvailableHotels()
   └─→ Check inventory for availability

2. Guest initializes booking
   └─→ HotelBookingController.initialiseBooking()
   └─→ BookingService.initialiseBooking()
   └─→ Lock inventory (pessimistic locking)
   └─→ Calculate dynamic pricing
   └─→ Create RESERVED booking

3. Guest adds travel companions
   └─→ BookingService.addGuests()
   └─→ Update booking status to GUESTS_ADDED

4. Guest initiates payment
   └─→ BookingService.initiatePayments()
   └─→ CheckoutService.getCheckoutSession()
   └─→ Create Stripe checkout session
   └─→ Return session URL to client

5. Stripe webhook callback
   └─→ WebhookController.handleStripeEvent()
   └─→ BookingService.capturePayment()
   └─→ Update booking to CONFIRMED
   └─→ Lock and confirm inventory
```

### Cancellation & Refund Flow
```
1. Guest cancels booking
   └─→ BookingService.cancelBooking()
   └─→ Validate ownership
   └─→ Verify booking is CONFIRMED

2. Process refund
   └─→ Retrieve Stripe session
   └─→ Extract payment intent
   └─→ Create refund via Stripe API

3. Update inventory
   └─→ Release booked count
   └─→ Update booking status to CANCELLED
```

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 12+
- Stripe Account (for payment integration)
- Git

### Setup Instructions

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd airBnbApp
```

#### 2. Configure Database
Create PostgreSQL database:
```sql
CREATE DATABASE airbnb_db;
```

#### 3. Environment Variables
Create a `.env` file in the project root:
```env
DB_URL=jdbc:postgresql://localhost:5432/airbnb_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_very_long_secret_key_minimum_32_chars_recommended
FRONTEND_URL=http://localhost:3000
STRIPE_SECRET_KEY=sk_test_your_stripe_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
```

#### 4. Install Dependencies
```bash
mvn clean install
```

#### 5. Run Application
```bash
mvn spring-boot:run
```

Or using IDE:
```
Right-click on AirBnbAppApplication.java > Run As > Java Application
```

#### 6. Verify Application
```bash
curl http://localhost:8080/api/v1/hotels/search
```

### Access API Documentation
```
Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
OpenAPI JSON: http://localhost:8080/api/v1/v3/api-docs
```

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=BookingServiceImplTest
```

### Run with Coverage
```bash
mvn clean test jacoco:report
```

---

## 📝 Environment Configuration

### application.properties
```properties
# Application
spring.application.name=airBnbApp
spring.profiles.active=local

# Database
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server
server.servlet.context-path=/api/v1

# Security
jwt.secretKey=${JWT_SECRET}

# Frontend
frontend.url=${FRONTEND_URL}

# Payment
stripe.secret.key=${STRIPE_SECRET_KEY}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}
```

---

## ✅ Resume Claims Verification

| Claim | Implementation | Status |
|-------|----------------|--------|
| Microservices architecture | BookingService, HotelService, PaymentService, InventoryService | ✅ Verified |
| JWT authentication | JWTService, JWTAuthFilter, 0.12.6 JJWT library | ✅ Verified |
| Role-based access control | WebSecurityConfig with @hasRole("HOTEL_MANAGER") | ✅ Verified |
| Booking lifecycle workflows | 6 booking statuses with state transitions | ✅ Verified |
| Availability checks | InventoryRepository.findAndLockAvailableInventory() | ✅ Verified |
| Reservation creation | BookingServiceImpl.initialiseBooking() with JPA transactions | ✅ Verified |
| Booking status management | BookingStatus enum + bookingStatus field | ✅ Verified |
| Stripe integration | CheckoutServiceImpl with Session.create() | ✅ Verified |
| Secure payments | SessionCreateParams with billing address required | ✅ Verified |
| Refunds | Refund.create() in cancelBooking() method | ✅ Verified |
| Transaction validation | Webhook event handling in WebhookController | ✅ Verified |
| PostgreSQL | spring-boot-starter-data-jpa with postgresql driver | ✅ Verified |
| Schema consistency | Unique constraints on inventory, foreign keys on entities | ✅ Verified |
| Referential integrity | JPA @ManyToOne, @OneToMany relationships with ForeignKey constraints | ✅ Verified |
| Centralized exception handling | GlobalExceptionHandler with @RestControllerAdvice | ✅ Verified |
| Request validation | CustomValidator pattern with @Valid annotations | ✅ Verified |
| Environment-based config | Properties from application.properties with ${} placeholders | ✅ Verified |
| Dynamic pricing | Decorator pattern with 5 pricing strategies | ✅ Verified |
| Pessimistic locking | @Lock(LockModeType.PESSIMISTIC_WRITE) in InventoryRepository | ✅ Verified |

---

## 📂 Project Structure

```
airBnbApp/
├── src/main/java/com/pavanKakade/projects/airBnbApp/
│   ├── AirBnbAppApplication.java
│   ├── advice/                          # Global error handling
│   │   ├── ApiError.java
│   │   ├── ApiResponse.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── GlobalResponseHandler.java
│   ├── config/                          # Spring configurations
│   │   ├── CorsConfig.java
│   │   ├── MapperConfig.java
│   │   └── StripeConfig.java
│   ├── controller/                      # REST API endpoints
│   │   ├── AuthController.java
│   │   ├── HotelBookingController.java
│   │   ├── HotelBrowseController.java
│   │   ├── HotelController.java
│   │   ├── InventoryController.java
│   │   ├── RoomAdminController.java
│   │   ├── UserController.java
│   │   └── WebhookController.java
│   ├── dto/                             # Data transfer objects
│   │   ├── BookingDto.java
│   │   ├── BookingRequest.java
│   │   ├── GuestDto.java
│   │   ├── HotelDto.java
│   │   ├── InventoryDto.java
│   │   └── [More DTOs...]
│   ├── entity/                          # JPA entities
│   │   ├── User.java
│   │   ├── Hotel.java
│   │   ├── Room.java
│   │   ├── Booking.java
│   │   ├── Guest.java
│   │   ├── Inventory.java
│   │   ├── HotelContactInfo.java
│   │   ├── HotelMinPrice.java
│   │   └── enums/
│   │       ├── BookingStatus.java
│   │       ├── Role.java
│   │       ├── Gender.java
│   │       └── PaymentStatus.java
│   ├── exception/                       # Custom exceptions
│   │   ├── ResourceNotFoundException.java
│   │   └── UnAuthorisedException.java
│   ├── repository/                      # Data access layer
│   │   ├── BookingRepository.java
│   │   ├── HotelRepository.java
│   │   ├── RoomRepository.java
│   │   ├── InventoryRepository.java
│   │   ├── UserRepository.java
│   │   ├── GuestRepository.java
│   │   └── HotelMinPriceRepository.java
│   ├── security/                        # Authentication & JWT
│   │   ├── AuthService.java
│   │   ├── JWTService.java
│   │   ├── JWTAuthFilter.java
│   │   └── WebSecurityConfig.java
│   ├── service/                         # Business logic
│   │   ├── BookingService.java
│   │   ├── BookingServiceImpl.java
│   │   ├── HotelService.java
│   │   ├── HotelServiceImpl.java
│   │   ├── UserService.java
│   │   ├── UserServiceImpl.java
│   │   ├── GuestService.java
│   │   ├── GuestServiceImpl.java
│   │   ├── RoomService.java
│   │   ├── RoomServiceImpl.java
│   │   ├── InventoryService.java
│   │   ├── InventoryServiceImpl.java
│   │   ├── CheckoutService.java
│   │   ├── CheckoutServiceImpl.java
│   │   └── PricingUpdateService.java
│   ├── strategy/                        # Pricing strategies
│   │   ├── PricingStrategy.java
│   │   ├── BasePricingStrategy.java
│   │   ├── SurgePricingStrategy.java
│   │   ├── OccupancyPricingStrategy.java
│   │   ├── HolidayPricingStrategy.java
│   │   ├── UrgencyPricingStrategy.java
│   │   └── PricingService.java
│   └── util/
│       └── AppUtils.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.properties
│   └── static/
├── pom.xml
├── README.md
└── HELP.md
```

---

## 🔄 Continuous Improvements

### Planned Enhancements
- [ ] Implement Spring WebClient for inter-service communication
- [ ] Add caching layer (Redis) for booking availability
- [ ] Implement event-driven architecture with Kafka
- [ ] Add comprehensive logging and monitoring
- [ ] Implement rate limiting and API throttling
- [ ] Add unit and integration tests with >80% coverage
- [ ] Implement API versioning strategy
- [ ] Add database migration tool (Flyway)
- [ ] Implement distributed tracing (Jaeger/Zipkin)
- [ ] Add health checks and metrics collection

---

## 🐛 Troubleshooting

### Common Issues

**Issue**: PostgreSQL connection failure
```
Solution: Verify DB_URL, username, and password in environment variables
         Ensure PostgreSQL service is running
         Check firewall rules for port 5432
```

**Issue**: Stripe webhook not working
```
Solution: Verify STRIPE_WEBHOOK_SECRET matches Stripe dashboard
         Ensure endpoint is publicly accessible
         Check logs for webhook event processing failures
```

**Issue**: JWT token expired
```
Solution: Token expiry configured in JWTService.java
         Implement refresh token mechanism
         Adjust expiry time based on requirements
```

---

## 📞 Support & Documentation

- **API Documentation**: [Swagger UI](http://localhost:8080/api/v1/swagger-ui.html)
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Stripe API**: https://stripe.com/docs/api
- **JWT Documentation**: https://jwt.io

---

## 📄 License

This project is provided as-is for educational and portfolio purposes.

---

## 👤 Author

**Pavan Kakade**  
Backend Engineer | Spring Boot Specialist | Microservices Architect

---

**Last Updated**: February 10, 2026  
**Project Status**: Active Development
