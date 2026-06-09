# DeliveryApp — Android

An on-demand delivery application built for Android, featuring a full dual-role system for customers and couriers.

## Features

### Customer Side
- Register and log in securely
- Create delivery requests (pickup address, destination, item description, fee)
- Checkout and simulate payment
- Track active shipments with real-time status timeline
- View delivery history

### Courier Side
- Browse available delivery jobs
- Accept and manage active jobs
- Update shipment status: `PICKED_UP` → `ON_THE_WAY` → `DELIVERED`
- Balance updated automatically upon delivery

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| UI | Android Studio, ConstraintLayout, LinearLayout |
| Local Database | SQLite (AppDbHelper + DAOs) |
| Cloud | Firebase Authentication + Firestore |

## Database Schema (SQLite)

- `addresses` — pickup and destination addresses
- `customers` — user accounts (customer + carrier roles)
- `carriers` — courier-specific data (car number, balance)
- `shipments` — full shipment lifecycle with tracking number and status
- `tracking_events` — timestamped event log per shipment

## Architecture

```
UI Layer       → Activities (Auth / Customer / Carrier)
Data Layer     → DAOs (CustomersDao, CarriersDao, ShipmentsDao, AddressesDao, TrackingEventsDao)
Persistence    → SQLite (offline-first)
Cloud Bonus    → Firebase Auth + Firestore (real-time sync)
```

> Firebase config (`google-services.json`) is required for cloud features. Add your own from the Firebase Console.

## Course

Android Development Workshop — Tel-Hai University, 2024
