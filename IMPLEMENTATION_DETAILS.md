# IMPLEMENTATION_DETAILS.md

## Technical Deep Dive - Airbnb-Inspired Backend Platform

This document provides detailed technical implementation information for each component of the AirBnB backend platform.

---

## Table of Contents
1. [Authentication & Security](#authentication--security)
2. [Booking Service Implementation](#booking-service-implementation)
3. [Payment Integration](#payment-integration)
4. [Dynamic Pricing System](#dynamic-pricing-system)
5. [Database Schema & Relationships](#database-schema--relationships)
6. [Transaction Management](#transaction-management)
7. [Exception Handling](#exception-handling)
8. [Service Layer Details](#service-layer-details)
9. [API Documentation](#api-documentation)
10. [Performance Optimizations](#performance-optimizations)

---

## Authentication & Security

### JWT Token Generation & Validation

**JWTService.java** Responsibilities:
- Generate access tokens with user credentials
- Generate refresh tokens for token renewal
- Extract claims from tokens
- Validate token signatures
- Handle token expiration

**Token Structure:**
```
Header:  {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "user@example.com",
  "userId": 1,
  "iat": 1707552000,
  "exp": 1707555600
}

Signature: HMAC-SHA256(header + "." + payload, secret)
```

**Implementation Details:**
```java
public String generateAccessToken(User user) {
    // Uses SecretKey derived from JWT_SECRET
    // HS256 algorithm for signature
    // Standard claims: subject, issued at, expiration
    // Custom claims: userId for database lookup
}

private SecretKey getSecretKey() {
    // Converts JWT_SECRET (String) to SecretKey
    // Uses HMAC-SHA256 with minimum 256-bit key
    // Cached after first generation
}
```

**Security Considerations:**
- JWT_SECRET minimum 32 characters recommended
- HMAC-SHA256 prevents token tampering
- Token expiration enforced on validation
- No sensitive data stored in JWT claims

### JWTAuthFilter Implementation

**Filter Chain Position:**
```
Request → CorsFilter → JWTAuthFilter → DispatcherServlet → Controller
Response ← Response               ← Response
```

**Filter Logic:**
```
1. Extract JWT from Authorization header (Bearer token)
2. Validate token signature and expiration
3. Extract user email from claims
4. Load User entity from database
5. Create UsernamePasswordAuthenticationToken
6. Set in SecurityContext for request
7. Pass to next filter
   └─ On error: Delegate to HandlerExceptionResolver
```

### WebSecurityConfig Configuration

**Key Security Rules:**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/admin/**").hasRole("HOTEL_MANAGER")
    .requestMatchers("/bookings/**").authenticated()
    .requestMatchers("/users/**").authenticated()
    .anyRequest().permitAll()
)
```

**What This Means:**
- `/admin/*` requires `ROLE_HOTEL_MANAGER`
- `/bookings/*` requires authenticated user with any role
- `/users/*` requires authentication
- All other routes publicly accessible

**Note:** Roles stored in User entity as enum, converted to GrantedAuthority at runtime

---

## Booking Service Implementation

### Booking Lifecycle State Machine

**State Diagram:**
```
┌─────────────────────────────────────┐
│         RESERVED                    │
│    (Inventory locked)               │
└──────────────┬──────────────────────┘
               │ addGuests()
               ▼
┌─────────────────────────────────────┐
│      GUESTS_ADDED                   │
│   (Guest details collected)         │
└──────────────┬──────────────────────┘
               │ initiatePayments()
               ▼
┌─────────────────────────────────────┐
│    PAYMENTS_PENDING                 │
│   (Waiting for Stripe callback)     │
└──────────────┬──────────────────────┘
        │              │
   Payment    Payment
   Success    Failed
        │              │
        ▼              ▼
  ┌─────────────┐  ┌──────────────┐
  │ CONFIRMED   │  │ EXPIRED      │
  │ (Booked)    │  │ (10min wait) │
  └──────┬──────┘  └──────────────┘
         │
    cancelBooking()
         │
         ▼
  ┌──────────────┐
  │ CANCELLED    │
  │ (Refunded)   │
  └──────────────┘
```

### initialiseBooking() Implementation

**Step 1: Validation**
```java
// Check hotel exists
Hotel hotel = hotelRepository.findById(hotelId)
    .orElseThrow(() -> new ResourceNotFoundException(...))

// Check room exists
Room room = roomRepository.findById(roomId)
    .orElseThrow(() -> new ResourceNotFoundException(...))
```

**Step 2: Pessimistic Locking (Critical!)**
```java
// Acquires write lock on inventory records
List<Inventory> inventoryList = inventoryRepository
    .findAndLockAvailableInventory(
        roomId, 
        checkInDate, 
        checkOutDate, 
        roomsCount
    )
```

**Database-Level Lock:**
```sql
SELECT * FROM inventory 
WHERE room_id = ? 
  AND date BETWEEN ? AND ? 
  AND (booked_count + reserved_count + ?) <= total_count
FOR UPDATE  -- This acquires exclusive lock!
```

**Why Pessimistic Locking?**
- Prevents double-booking in concurrent scenarios
- Avoids dirty reads of inventory data
- Transaction-scoped lock automatically released on commit

**Step 3: Dynamic Pricing Calculation**
```java
// Uses Decorator pattern to apply all strategies
List<Inventory> inventoriesForDateRange = [
    {date: 2024-02-10, price: 100, surgeFactor: 1.2},
    {date: 2024-02-11, price: 100, surgeFactor: 1.0},
    {date: 2024-02-12, price: 100, surgeFactor: 1.0}
]

// BasePricingStrategy: 100 + 100 + 100 = 300
// SurgePricingStrategy: 120 + 100 + 100 = 320
// OccupancyPricingStrategy: 120 + 100 + 100 = 320 (assume <80% occupancy)
// UrgencyPricingStrategy: 120 + 100 + 100 = 320 (>7 days out)

Total for 1 room: 320
Total for N rooms: 320 * N
```

**Step 4: Inventory Update**
```java
// Marks rooms as reserved without changing state in memory
inventoryRepository.initBooking(
    roomId, 
    checkInDate, 
    checkOutDate, 
    roomsCount  // increment reserved_count
)
```

**Step 5: Booking Persistence**
```java
Booking booking = Booking.builder()
    .bookingStatus(BookingStatus.RESERVED)
    .hotel(hotel)
    .room(room)
    .checkInDate(checkInDate)
    .checkOutDate(checkOutDate)
    .user(getCurrentUser())
    .roomsCount(roomsCount)
    .amount(totalPrice)  // From step 3
    .build()

// @Transactional ensures all-or-nothing
bookingRepository.save(booking)
```

### addGuests() Implementation

**Authorization Check:**
```java
User currentUser = getCurrentUser()

if (!currentUser.equals(booking.getUser())) {
    throw new UnAuthorisedException(
        "Booking does not belong to this user"
    )
}
```

**State Validation:**
```java
if (booking.getBookingStatus() != BookingStatus.RESERVED) {
    throw new IllegalStateException(
        "Booking is not under reserved state, cannot add guests"
    )
}

if (hasBookingExpired(booking)) {  // 10-minute window
    throw new IllegalStateException(
        "Booking has already expired"
    )
}
```

**Guest Association:**
```java
// Each guest must be created beforehand via GuestService
for (Long guestId : guestIdList) {
    Guest guest = guestRepository.findById(guestId)
        .orElseThrow(...)
    
    // ManyToMany relationship - add to Set
    booking.getGuests().add(guest)
}

booking.setBookingStatus(BookingStatus.GUESTS_ADDED)
bookingRepository.save(booking)
```

### initiatePayments() Implementation

**Purpose:**
- Create Stripe checkout session
- Transition booking state
- Return payment URL to client

**Implementation:**
```java
String sessionUrl = checkoutService.getCheckoutSession(
    booking,
    // success_url and cancel_url point to frontend
    frontendUrl + "/payments/" + bookingId + "/status",
    frontendUrl + "/payments/" + bookingId + "/status"
)

booking.setPaymentSessionId(sessionUrl)
booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING)
bookingRepository.save(booking)

return sessionUrl
```

**Client Flow:**
```
1. Receive sessionUrl from /bookings/:id/payments
2. Redirect user to sessionUrl
3. Stripe checkout page loads
4. Customer enters payment details
5. On success: Stripe calls webhook
6. Client polls /bookings/:id/status for confirmation
```

### capturePayment() Implementation

**Triggered by:** Stripe webhook event `checkout.session.completed`

**Process:**
```java
// 1. Deserialize Stripe event
Session session = event.getDataObjectDeserializer().getObject()
String sessionId = session.getId()

// 2. Find booking by session ID
Booking booking = bookingRepository.findByPaymentSessionId(sessionId)
    .orElseThrow(...)

// 3. Update booking state
booking.setBookingStatus(BookingStatus.CONFIRMED)

// 4. Lock reserved inventory
inventoryRepository.findAndLockReservedInventory(
    bookingId, dateRange, roomsCount
)

// 5. Confirm booking (move from reserved_count to booked_count)
inventoryRepository.confirmBooking(...)

// 6. Persist changes
bookingRepository.save(booking)
```

**Database Update (confirmBooking):**
```sql
UPDATE inventory 
SET reserved_count = reserved_count - ?
  , booked_count = booked_count + ?
WHERE room_id = ? 
  AND date BETWEEN ? AND ?
```

### cancelBooking() Implementation

**Preconditions:**
```java
// User must own booking
if (!currentUser.equals(booking.getUser())) {
    throw new UnAuthorisedException(...)
}

// Booking must be confirmed
if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
    throw new IllegalStateException(...)
}
```

**Refund Process:**
```java
Session session = Session.retrieve(booking.getPaymentSessionId())

RefundCreateParams refundParams = RefundCreateParams.builder()
    .setPaymentIntent(session.getPaymentIntent())
    .build()

Refund.create(refundParams)  // Stripe API call
```

**Inventory Release:**
```java
// Lock inventory records
inventoryRepository.findAndLockReservedInventory(...)

// Release booked count back to available
inventoryRepository.cancelBooking(...)

// Update booking
booking.setBookingStatus(BookingStatus.CANCELLED)
bookingRepository.save(booking)
```

---

## Payment Integration

### Stripe Configuration

**StripeConfig.java:**
```java
@Configuration
public class StripeConfig {
    public StripeConfig(@Value("${stripe.secret.key}") String stripeSecretKey) {
        Stripe.apiKey = stripeSecretKey;  // Set globally for SDK
    }
}
```

### CheckoutServiceImpl Details

**Customer Creation:**
```java
CustomerCreateParams customerParams = CustomerCreateParams.builder()
    .setName(user.getName())
    .setEmail(user.getEmail())
    .build()

Customer customer = Customer.create(customerParams)
```

**Session Creation:**
```java
SessionCreateParams sessionParams = SessionCreateParams.builder()
    .setMode(SessionCreateParams.Mode.PAYMENT)  // Not SUBSCRIPTION
    .setBillingAddressCollection(
        SessionCreateParams.BillingAddressCollection.REQUIRED
    )
    .setCustomer(customer.getId())
    .setSuccessUrl(successUrl)
    .setCancelUrl(failureUrl)
    .addLineItem(
        SessionCreateParams.LineItem.builder()
            .setQuantity(1L)  // 1 booking, not per room
            .setPriceData(
                SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("inr")  // Indian Rupees
                    .setUnitAmount(
                        booking.getAmount()
                            .multiply(BigDecimal.valueOf(100))
                            .longValue()  // Stripe uses cents
                    )
                    .setProductData(
                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(
                                booking.getHotel().getName() 
                                + " : " 
                                + booking.getRoom().getType()
                            )
                            .setDescription(
                                "Booking ID: " + booking.getId()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()

Session session = Session.create(sessionParams)
```

**Currency & Amount Conversion:**
```
Amount: 5000 INR
In Stripe cents: 5000 * 100 = 500,000 (smallest unit)
```

### WebhookController Events

**Webhook Security:**
```java
@Value("${stripe.webhook.secret}")
private String endpointSecret;

// Verify signature
String sigHeader = request.getHeader("Stripe-Signature")
Event event = Webhook.constructEvent(
    payload, 
    sigHeader, 
    endpointSecret
)
```

**Event Types Handled:**
- `checkout.session.completed`: Payment successful
- `charge.refunded`: Refund completed
- `invoice.payment_failed`: Payment failed

---

## Dynamic Pricing System

### Pricing Strategy Architecture

**Strategy Pattern Structure:**
```
interface PricingStrategy
    ↑
    ├─ BasePricingStrategy
    └─ DecoratorPricingStrategy (abstract)
        ├─ SurgePricingStrategy
        ├─ OccupancyPricingStrategy
        ├─ HolidayPricingStrategy
        └─ UrgencyPricingStrategy
```

### Individual Strategy Implementations

**BasePricingStrategy:**
```java
public BigDecimal calculatePrice(Inventory inventory) {
    return inventory.getRoom().getBasePrice()
}
```

**SurgePricingStrategy:**
```java
public BigDecimal calculatePrice(Inventory inventory) {
    BigDecimal price = wrapped.calculatePrice(inventory)
    // Multiply by surge factor (1.0 = no surge, 1.5 = 50% surge)
    return price.multiply(inventory.getSurgeFactor())
}
```

**OccupancyPricingStrategy:**
```java
public BigDecimal calculatePrice(Inventory inventory) {
    BigDecimal price = wrapped.calculatePrice(inventory)
    
    // occupancyRate = booked / total (0.0 to 1.0)
    double occupancyRate = 
        (double) inventory.getBookedCount() 
        / inventory.getTotalCount()
    
    if (occupancyRate > 0.8) {  // >80% booked
        price = price.multiply(BigDecimal.valueOf(1.2))  // +20%
    }
    return price
}
```

**UrgencyPricingStrategy:**
```java
public BigDecimal calculatePrice(Inventory inventory) {
    BigDecimal price = wrapped.calculatePrice(inventory)
    
    LocalDate today = LocalDate.now()
    LocalDate bookingDate = inventory.getDate()
    
    // If booking is within 7 days, apply urgency multiplier
    if (!bookingDate.isBefore(today)  // Not in past
        && bookingDate.isBefore(today.plusDays(7))) {  // Within 7 days
        price = price.multiply(BigDecimal.valueOf(1.15))  // +15%
    }
    return price
}
```

### Composition Example

**PricingService.calculateDynamicPricing:**
```java
public BigDecimal calculateDynamicPricing(Inventory inventory) {
    // Start with base strategy
    PricingStrategy strategy = new BasePricingStrategy()
    
    // Wrap with decorators (order matters!)
    strategy = new SurgePricingStrategy(strategy)
    strategy = new OccupancyPricingStrategy(strategy)
    strategy = new UrgencyPricingStrategy(strategy)
    
    // Return final composed price
    return strategy.calculatePrice(inventory)
}
```

**Calculation Flow (Example):**
```
Input: Inventory {
    basePrice: 100,
    surgeFactor: 1.5,
    bookedCount: 85 (85% occupancy),
    totalCount: 100,
    date: 2024-02-15 (3 days from now)
}

1. BasePricingStrategy: 100
2. SurgePricingStrategy: 100 * 1.5 = 150
3. OccupancyPricingStrategy: 150 * 1.2 = 180 (>80% occupancy)
4. UrgencyPricingStrategy: 180 * 1.15 = 207 (within 7 days)

Final Price: 207 INR
```

### Price Aggregation

**calculateTotalPrice(List<Inventory>):**
```java
public BigDecimal calculateTotalPrice(List<Inventory> inventoryList) {
    return inventoryList.stream()
        .map(this::calculateDynamicPricing)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
}
```

For 5-day booking:
```
Day 1: 200 (base + surge)
Day 2: 200 (base + surge)
Day 3: 230 (base + surge + urgency)
Day 4: 230
Day 5: 230

Total: 200 + 200 + 230 + 230 + 230 = 1090 INR per room
```

---

## Database Schema & Relationships

### Entity Relationships Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         User (PK: id)                       │
│─────────────────────────────────────────────────────────────│
│ - id (SEQ)              - email (UNIQUE)                    │
│ - name                  - password (BCrypt)                 │
│ - gender (ENUM)         - dateOfBirth                       │
│ - roles (Set<Role>)     - createdAt (TIMESTAMP)             │
└────────────┬──────────────────────────────────────┬─────────┘
             │                                      │
      1 : N  │ owns                          1 : N  │ books
             │                                      │
    ┌────────▼────────────────────┐      ┌─────────▼──────────┐
    │     Hotel (PK: id)          │      │  Booking (PK: id)  │
    ├─────────────────────────────┤      ├────────────────────┤
    │ - id (SEQ)                  │      │ - id (SEQ)         │
    │ - name                      │      │ - hotelId (FK)     │
    │ - city                      │      │ - roomId (FK)      │
    │ - photos (TEXT[])           │      │ - userId (FK)      │
    │ - amenities (TEXT[])        │      │ - checkInDate      │
    │ - contactInfo (EMBEDDED)    │      │ - checkOutDate     │
    │ - active (BOOL)             │      │ - roomsCount       │
    │ - ownerId (FK → User)       │      │ - amount (DECIMAL) │
    └────────┬────────────────────┘      │ - status (ENUM)    │
             │                            │ - paymentSessionId │
      1 : N  │ contains                   └────────┬───────────┘
             │                                     │
    ┌────────▼────────────────────┐      ┌────────▼──────────┐
    │     Room (PK: id)          │      │  Guest (PK: id)   │
    ├─────────────────────────────┤      ├───────────────────┤
    │ - id (SEQ)                  │      │ - id (SEQ)        │
    │ - hotelId (FK)              │      │ - userId (FK)     │
    │ - type (VARCHAR)            │      │ - name (VARCHAR)  │
    │ - basePrice (DECIMAL)       │      │ - gender (ENUM)   │
    │ - photos (TEXT[])           │      │ - dateOfBirth     │
    │ - amenities (TEXT[])        │      │ - createdAt       │
    │ - totalCount (INT)          │      └───────────┬───────┘
    │ - capacity (INT)            │                  │
    │                             │    M : N (JOIN)  │
    └────────┬────────────────────┘                  │
             │                                       │
      1 : N  │ tracks                         ┌──────▼──────┐
             │                                │ booking_guest
    ┌────────▼──────────────────────┐       │ (JOIN TABLE)
    │   Inventory (PK: id)          │       └──────────────┘
    ├───────────────────────────────┤
    │ - id (SEQ)                    │
    │ - hotelId (FK)                │
    │ - roomId (FK)                 │
    │ - date (DATE)                 │
    │ - totalCount (INT)            │
    │ - bookedCount (INT)           │
    │ - reservedCount (INT)         │
    │ - surgeFactor (DECIMAL)       │
    │ - price (DECIMAL)             │
    │ - closed (BOOL)               │
    │ - createdAt (TIMESTAMP)       │
    │ - updatedAt (TIMESTAMP)       │
    │ CONSTRAINT: UNIQUE(hotelId,   │
    │             roomId, date)     │
    └───────────────────────────────┘
```

### Critical Constraints

**Unique Constraint (Inventory):**
```sql
ALTER TABLE inventory 
ADD CONSTRAINT unique_hotel_room_date 
UNIQUE(hotel_id, room_id, date)
```

This prevents:
```
❌ Two records for Hotel1, Room A, 2024-02-15
✅ Hotel1, Room A, 2024-02-15
✅ Hotel1, Room B, 2024-02-15
✅ Hotel2, Room A, 2024-02-15
```

### Fetch Type Strategies

**LAZY Fetching (Default):**
```
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "hotel_id")
private Hotel hotel

// Benefits:
// - Avoids N+1 queries
// - Reduces memory overhead
// - Requires open session for access
```

**EAGER Fetching (When needed):**
```
// Not used in this project (good!)
// Would load related entities immediately
// Risks N+1 queries if not careful
```

---

## Transaction Management

### @Transactional Annotation Details

**BookingServiceImpl Methods:**
```java
@Transactional  // REQUIRED by default
public BookingDto initialiseBooking(BookingRequest bookingRequest) {
    // All operations within this method form single transaction
    // Commits on successful return
    // Rolls back on exception
}
```

**Transaction Boundaries:**
```
1. initialiseBooking()
   ├─ Find Hotel (read lock)
   ├─ Find Room (read lock)
   ├─ Lock available inventory (WRITE lock)
   ├─ Calculate pricing
   ├─ Create Booking entity
   └─ Save to database → COMMIT

2. If exception at any point → ROLLBACK all changes
```

### Pessimistic Locking Example

**Problem without locking:**
```
Thread 1: Read Inventory(available=5)
Thread 2: Read Inventory(available=5)
Thread 1: Reserve 5 rooms → Update inventory(available=0)
Thread 2: Reserve 5 rooms → Update inventory(available=0)
Result: Double-booking! 10 rooms reserved but only 5 exist
```

**Solution with pessimistic locking:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE ...")
List<Inventory> findAndLockAvailableInventory(...)

// Thread 1: Acquires exclusive lock on Inventory records
// Thread 2: Waits for Thread 1 to release lock
// Thread 1: Updates inventory and commits
// Thread 2: Acquires lock, operates on latest data
// No double-booking!
```

**SQL Under the Hood:**
```sql
-- JPA generates:
SELECT id, room_id, date, ... FROM inventory 
WHERE room_id = ? AND date BETWEEN ? AND ?
FOR UPDATE  -- Acquires exclusive lock

-- Other transactions cannot:
-- - Read this row (dirty read prevention)
-- - Write to this row (phantom read prevention)
```

---

## Exception Handling

### GlobalExceptionHandler Implementation

**Architecture:**
```
Request → Controller → Service → throws Exception
                                      │
                                      └→ GlobalExceptionHandler
                                           │
                                           ├─ ResourceNotFoundException
                                           ├─ UnAuthorisedException
                                           ├─ JwtException
                                           ├─ AuthenticationException
                                           ├─ AccessDeniedException
                                           └─ Others
```

### Exception Mapping

**ResourceNotFoundException:**
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
    ResourceNotFoundException ex) {
    
    ApiError error = ApiError.builder()
        .status(HttpStatus.NOT_FOUND)
        .message(ex.getMessage())
        .build()
    
    return new ResponseEntity<>(
        new ApiResponse<>(error),
        HttpStatus.NOT_FOUND
    )
}
```

**Response Format:**
```json
{
  "timeStamp": "2024-02-10T10:30:45.123456",
  "data": null,
  "error": {
    "status": "NOT_FOUND",
    "message": "Hotel not found with id: 123",
    "subErrors": null
  }
}
```

**UnAuthorisedException:**
```java
@ExceptionHandler(UnAuthorisedException.class)
public ResponseEntity<ApiResponse<Void>> handleUnAuthorised(
    UnAuthorisedException ex) {
    
    // 403 FORBIDDEN for authorization failures
    ApiError error = ApiError.builder()
        .status(HttpStatus.FORBIDDEN)
        .message(ex.getMessage())
        .build()
    
    return new ResponseEntity<>(
        new ApiResponse<>(error),
        HttpStatus.FORBIDDEN
    )
}
```

**JwtException:**
```java
@ExceptionHandler(JwtException.class)
public ResponseEntity<ApiResponse<Void>> handleJwtException(
    JwtException ex) {
    
    // 401 UNAUTHORIZED for authentication failures
    ApiError error = ApiError.builder()
        .status(HttpStatus.UNAUTHORIZED)
        .message("Invalid or expired token")
        .build()
    
    return new ResponseEntity<>(
        new ApiResponse<>(error),
        HttpStatus.UNAUTHORIZED
    )
}
```

### GlobalResponseHandler

**Purpose:** Wrap all successful responses in ApiResponse

```java
@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {
    
    @Override
    public Object beforeBodyWrite(Object body, ...) {
        if (body instanceof ApiResponse) {
            return body  // Already wrapped
        }
        
        return new ApiResponse<>(body)  // Wrap response
    }
}
```

**Output:**
```js
// Before (raw)
{
  "id": 1,
  "name": "Hotel A"
}

// After (wrapped)
{
  "timeStamp": "2024-02-10T10:30:45...",
  "data": {
    "id": 1,
    "name": "Hotel A"
  },
  "error": null
}
```

---

## Service Layer Details

### UserServiceImpl

**Key Responsibilities:**
```java
public UserDetails loadUserByUsername(String email) {
    // Implements UserDetailsService
    // Called by Spring Security during authentication
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException(...))
}

public UserDto getUserProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(...)
    return modelMapper.map(user, UserDto.class)
}

public UserDto updateProfile(Long userId, ProfileUpdateRequestDto dto) {
    User user = userRepository.findById(userId)...
    
    if (dto.getName() != null) user.setName(dto.getName())
    if (dto.getDateOfBirth() != null) 
        user.setDateOfBirth(dto.getDateOfBirth())
    if (dto.getGender() != null) user.setGender(dto.getGender())
    
    return modelMapper.map(userRepository.save(user), UserDto.class)
}
```

### HotelServiceImpl

**Key Methods:**
```java
public HotelDto createHotel(HotelDto hotelDto) {
    Hotel hotel = modelMapper.map(hotelDto, Hotel.class)
    hotel.setOwner(getCurrentUser())
    hotel.setActive(true)
    return modelMapper.map(hotelRepository.save(hotel), HotelDto.class)
}

public HotelInfoDto getHotelInfo(
    Long hotelId, 
    HotelInfoRequestDto infoRequest) {
    
    // Get hotel
    Hotel hotel = hotelRepository.findById(hotelId)...
    
    // Get available rooms with pricing
    List<RoomPriceResponseDto> rooms = 
        inventoryRepository.findAvailableRoomsWithPricing(
            hotelId,
            infoRequest.getStartDate(),
            infoRequest.getEndDate(),
            infoRequest.getRoomsCount()
        )
    
    return new HotelInfoDto(
        modelMapper.map(hotel, HotelDto.class),
        rooms
    )
}
```

### GuestServiceImpl

**Guest Management:**
```java
public GuestDto addNewGuest(GuestDto guestDto) {
    Guest guest = modelMapper.map(guestDto, Guest.class)
    guest.setUser(getCurrentUser())  // Link to current user
    guest.setCreatedAt(LocalDateTime.now())
    
    return modelMapper.map(
        guestRepository.save(guest), 
        GuestDto.class
    )
}
```

**Why Separate from User?**
- User = authentication principal
- Guest = traveler associated with booking
- One User can have multiple Guest profiles

### InventoryServiceImpl

**Key Operations:**
```java
@Transactional
public void updateInventoryBatch(
    Long hotelId, 
    UpdateInventoryRequestDto updateRequest) {
    
    // Update surge factor for date range
    inventoryRepository.updateSurgeFactor(
        hotelId,
        updateRequest.getStartDate(),
        updateRequest.getEndDate(),
        updateRequest.getSurgeFactor()
    )
    
    // Close rooms for specified dates
    if (updateRequest.getClosed()) {
        inventoryRepository.closeRooms(...)
    }
}
```

### RoomServiceImpl

**Room CRUD:**
```java
public RoomDto addRoom(Long hotelId, RoomDto roomDto) {
    Hotel hotel = hotelRepository.findById(hotelId)...
    
    // Only hotel owner can add rooms
    User owner = getCurrentUser()
    if (!owner.equals(hotel.getOwner())) {
        throw new UnAuthorisedException(...)
    }
    
    Room room = modelMapper.map(roomDto, Room.class)
    room.setHotel(hotel)
    
    return modelMapper.map(
        roomRepository.save(room), 
        RoomDto.class
    )
}
```

---

## API Documentation

### Authentication Endpoints

**POST /api/v1/auth/signup**
```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123",
    "name": "John Doe"
  }'
```

**Response (201 Created):**
```json
{
  "timeStamp": "2024-02-10T10:30:45.123456",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "gender": null,
    "dateOfBirth": null
  },
  "error": null
}
```

**POST /api/v1/auth/login**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123"
  }'
```

**Response (200 OK):**
```json
{
  "timeStamp": "2024-02-10T10:30:45.123456",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  },
  "error": null
}
```

**Headers for Protected Routes:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Booking Flow Example

**1. Initialize Booking**
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelId": 1,
    "roomId": 1,
    "checkInDate": "2024-02-15",
    "checkOutDate": "2024-02-20",
    "roomsCount": 2
  }'
```

**Response:**
```json
{
  "timeStamp": "2024-02-10T10:30:45...",
  "data": {
    "id": 100,
    "roomsCount": 2,
    "checkInDate": "2024-02-15",
    "checkOutDate": "2024-02-20",
    "createdAt": "2024-02-10T10:30:45...",
    "bookingStatus": "RESERVED",
    "amount": 1090.00,
    "guests": []
  },
  "error": null
}
```

**2. Add Guests**
```bash
curl -X PUT http://localhost:8080/api/v1/bookings/100/guests \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "guestIdList": [1, 2, 3, 4]
  }'
```

**3. Initiate Payment**
```bash
curl -X POST http://localhost:8080/api/v1/bookings/100/payments \
  -H "Authorization: Bearer <token>"
```

**Response:**
```json
{
  "timeStamp": "2024-02-10T10:30:45...",
  "data": {
    "sessionUrl": "https://checkout.stripe.com/pay/cs_..."
  },
  "error": null
}
```

**4. Check Status**
```bash
curl -X GET http://localhost:8080/api/v1/bookings/100/status \
  -H "Authorization: Bearer <token>"
```

---

## Performance Optimizations

### Query Optimizations

**1. Lazy Loading with Select Clause**
```java
@Query("SELECT new com.pavanKakade.projects.airBnbApp.dto.RoomPriceResponseDto"
        + "(r.id, r.type, r.photos, r.amenities, i.price) "
        + "FROM Inventory i "
        + "JOIN i.room r "
        + "WHERE i.hotel.id = ? AND i.date BETWEEN ? AND ? "
        + "GROUP BY r.id")
List<RoomPriceResponseDto> findAvailableRoomsWithPricing(
    Long hotelId, 
    LocalDate startDate, 
    LocalDate endDate);
```

**Benefits:**
- Returns DTO directly (no entity hydration)
- Only selected columns fetched from database
- Avoids N+1 queries for related entities

**2. Batch Fetching**
```java
@Transactional
public Page<HotelPriceDto> searchAvailableHotels(
    HotelSearchRequest request,
    Pageable pageable) {
    
    return hotelMinPriceRepository.findAvailableHotels(
        request.getCity(),
        request.getStartDate(),
        request.getEndDate(),
        request.getRoomsCount(),
        pageable  // Limits result set
    )
}
```

**Benefits:**
- Pagination prevents large dataset loads
- Database does pagination (more efficient)

### Database Connection Pooling

**HikariCP (Spring Data Default)**
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

**What it does:**
- Maintains pool of reusable database connections
- Prevents creating new connection per request
- Reduces latency and resource contention

### Caching Recommendations (Not Implemented)

```java
@Cacheable("hotels")
public HotelDto getHotel(Long hotelId) {
    // Results cached for 1 hour
    // Second call returns from cache
}

@CacheEvict("hotels", allEntries = true)
public HotelDto updateHotel(Long hotelId, HotelDto dto) {
    // Clears cache on update
}
```

### Async Processing (Future Enhancement)

```java
@Async
public void sendBookingConfirmationEmail(Booking booking) {
    // Doesn't block booking creation
    // Runs on separate thread pool
}
```

---

## Security Best Practices

### Password Storage
- ✅ BCrypt hashing with configurable rounds
- ❌ Never store plaintext passwords
- ❌ Never use MD5 or SHA1

### JWT Security
- ✅ HMAC-SHA256 signature
- ✅ Token expiration enforced
- ✅ Secret key >256 bits
- ❌ Token never stored on server
- ❌ No sensitive data in claims

### Database Security
- ✅ Prepared statements (prevents SQL injection)
- ✅ Unique constraints on sensitive fields
- ✅ Foreign key constraints
- ❌ Never execute raw SQL from user input
- ❌ Principle of least privilege for DB user

### Transport Security
- ✅ HTTPS in production
- ✅ CORS properly configured
- ✅ CSRF protection enabled
- ❌ Credentials never in URL
- ❌ Sensitive data not in query params

---

## Testing Strategy

### Unit Testing Example

```java
@SpringBootTest
class BookingServiceImplTest {
    
    @MockBean
    BookingRepository bookingRepository;
    
    @MockBean
    HotelRepository hotelRepository;
    
    @InjectMocks
    BookingServiceImpl bookingService;
    
    @Test
    void testInitialiseBooking_HotelNotFound() {
        when(hotelRepository.findById(1L))
            .thenReturn(Optional.empty())
        
        assertThrows(ResourceNotFoundException.class, 
            () -> bookingService.initialiseBooking(request))
    }
}
```

### Integration Testing Example

```java
@SpringBootTest
@Transactional
class BookingIntegrationTest {
    
    @Autowired
    BookingService bookingService
    
    @Autowired
    HotelRepository hotelRepository
    
    @Test
    void testCompleteBookingFlow() {
        // Setup: Create test data
        Hotel hotel = hotelRepository.save(createTestHotel())
        
        // Execute: Complete booking flow
        BookingDto booking = bookingService.initialiseBooking(request)
        
        // Verify: Assertions
        assertThat(booking.getBookingStatus())
            .isEqualTo(BookingStatus.RESERVED)
    }
}
```

---

## Deployment Considerations

### Environment Variables Required
```bash
DB_URL=jdbc:postgresql://db-host:5432/airbnb_db
DB_USERNAME=db_user
DB_PASSWORD=secure_password_here
JWT_SECRET=min_32_characters_very_long_secret_key_here
FRONTEND_URL=https://frontend.example.com
STRIPE_SECRET_KEY=sk_live_your_live_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
```

### Production Checklist
- [ ] HTTPS enabled
- [ ] Database backups configured
- [ ] JWT_SECRET rotated regularly
- [ ] Stripe keys from production account
- [ ] Logging aggregation setup
- [ ] Monitoring and alerting configured
- [ ] Database connection pooling optimized
- [ ] Rate limiting implemented
- [ ] CORS whitelist specific origins
- [ ] Security headers set (HSTS, CSP, etc.)

---

## Maintenance & Monitoring

### Key Metrics to Track
- Booking success rate
- Average API response time
- Payment failure/retry rate
- Database query performance
- JWT token expiration issues
- Stripe API error rates

### Common Issues & Solutions

**Issue**: "Room is not available anymore"
```
Cause: Race condition in concurrent bookings
Solution: Pessimistic locking already implemented
Verify: Check InventoryRepository.findAndLockAvailableInventory()
```

**Issue**: Booking expires before payment
```
Problem: 10-minute window too short
Solution: Check hasBookingExpired() in BookingServiceImpl
Adjust: Update expiry logic based on user feedback
```

**Issue**: Stripe webhook not received
```
Verify: STRIPE_WEBHOOK_SECRET matches dashboard
Check: Logs in WebhookController
Test: Use Stripe CLI for local testing
```

---

**Document Updated**: February 10, 2026  
**Maintained By**: Pavan Kakade  
**Status**: Production Ready
