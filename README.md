# National Nines Golf API

Backend API for National Nines Golf website - handles competition entries, shop orders, and Stripe payments.

## Tech Stack

- **Java 21**
- **Spring Boot 3.2**
- **Spring Data JPA**
- **H2 Database** (dev) / **PostgreSQL** (production)
- **Stripe SDK** for payments

## Features

- 🏌️ Competition entry management (Kent Nines, Essex Nines)
- 🛒 Shop order processing
- 💳 Stripe Checkout integration
- 📊 Admin dashboard endpoints
- 🔔 Webhook handling for payment confirmations

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+

### Development

```bash
# Run with H2 database
./mvnw spring-boot:run

# Access H2 console at http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:./data/national-nines
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | 8080 |
| `STRIPE_API_KEY` | Stripe secret key | - |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | - |
| `FRONTEND_URL` | Frontend origin for CORS | https://nationalninesgolf.co.uk |
| `DATABASE_URL` | PostgreSQL URL (prod) | - |
| `MAIL_HOST` | SMTP host | smtp.gmail.com |
| `MAIL_USERNAME` | SMTP username | - |
| `MAIL_PASSWORD` | SMTP password | - |
| `ADMIN_EMAIL` | Admin notification email | info@nationalninesgolf.co.uk |
| `ADMIN_API_KEY` | API key for admin endpoints | - (open in dev) |

## API Endpoints

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/entries` | Submit competition entry |
| GET | `/api/entries/event/{event}/count` | Get entry count |
| POST | `/api/orders` | Create shop order |
| GET | `/api/orders/{orderNumber}/status` | Check order status |

### Admin Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/dashboard` | Dashboard stats |
| GET | `/api/admin/entries` | All entries |
| GET | `/api/admin/orders` | All orders |
| GET | `/api/admin/orders/to-fulfill` | Orders to fulfill |
| POST | `/api/admin/entries/{id}/mark-paid` | Manual payment |
| POST | `/api/admin/orders/{id}/status` | Update order status |

### Webhooks

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/webhooks/stripe` | Stripe webhook handler |

## Stripe Integration

### Setup

1. Create Stripe account at [stripe.com](https://stripe.com)
2. Get API keys from Dashboard > Developers > API keys
3. Set up webhook endpoint pointing to `/api/webhooks/stripe`
4. Configure webhook events:
   - `checkout.session.completed`
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`

### Flow

1. Frontend calls `/api/entries` or `/api/orders`
2. API creates record and Stripe Checkout session
3. Returns checkout URL to frontend
4. User redirected to Stripe for payment
5. On success, Stripe sends webhook
6. API marks entry/order as paid

## Deployment

### Railway / Render

```bash
# Build
./mvnw clean package -DskipTests

# Dockerfile is included
```

### Docker

```bash
# Build image
docker build -t national-nines-api .

# Run
docker run -p 8080:8080 \
  -e STRIPE_API_KEY=sk_xxx \
  -e STRIPE_WEBHOOK_SECRET=whsec_xxx \
  -e DATABASE_URL=postgres://... \
  national-nines-api
```

### Environment

Set `SPRING_PROFILES_ACTIVE=prod` for production settings.

## Database Schema

### Entries Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| event | VARCHAR | Event identifier |
| club_name | VARCHAR | Golf club name |
| player1_name | VARCHAR | First player |
| player1_email | VARCHAR | First player email |
| player1_handicap | DECIMAL | First player handicap |
| player2_name | VARCHAR | Second player |
| player2_email | VARCHAR | Second player email |
| player2_handicap | DECIMAL | Second player handicap |
| contact_phone | VARCHAR | Contact number |
| payment_status | ENUM | PENDING/PAID/FAILED/REFUNDED |
| entry_fee | DECIMAL | Fee amount |
| created_at | TIMESTAMP | Created timestamp |
| paid_at | TIMESTAMP | Payment timestamp |

### Orders Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| order_number | VARCHAR | Unique order reference |
| customer_name | VARCHAR | Customer name |
| customer_email | VARCHAR | Customer email |
| delivery_method | ENUM | COLLECTION/SHIPPING |
| status | ENUM | Order status |
| subtotal | DECIMAL | Items total |
| shipping_cost | DECIMAL | Shipping cost |
| total | DECIMAL | Grand total |

## Security

Admin endpoints (`/api/admin/*`) are protected by API key authentication.

**To access admin endpoints in production:**
```bash
curl -H "X-API-Key: your-secret-key" https://api.nationalninesgolf.co.uk/api/admin/dashboard
```

**Development mode:** If `ADMIN_API_KEY` is not set, admin endpoints are open.

**Recommendations for production:**
1. Set a strong `ADMIN_API_KEY` (generate with `openssl rand -hex 32`)
2. Enable HTTPS (handled by Railway/Render/etc.)
3. Consider adding rate limiting
4. Monitor with actuator endpoints

## License

Proprietary - National Nines Golf Ltd
