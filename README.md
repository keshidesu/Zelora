# Zelora
An eccommerce app
# Zelora – E-Commerce Mobile App (Java, Android Studio)

Category: **Business Applications → E-Commerce Application** · CT-MAD 213 Final Project

Zelora is a small online store for lifestyle products (bags, apparel, accessories) priced in Philippine pesos.
Customers register, browse and search products, save favourites, fill a cart, check out with a shipping
address and a payment method, and review their order history. All data is stored on the phone in a SQLite database.

## Features
- Sign up / Log in with validation, salted + hashed passwords, "Forgot Password?" (offline reset), persistent session
- Home: search, category chips (All / Bags / Apparel / Accessories), free-shipping banner, sort (Filters), 2-column product grid
- Product detail: photo slider, price + sale badge, description, color picker, quantity, wishlist heart, share, Add to Cart, Buy Now
- Wishlist and Cart tabs (add, change quantity, remove)
- Checkout sheet: shipping address, payment (Card / GCash-Maya / Cash on Delivery), totals, free shipping from ₱800
- Orders saved in one database transaction with stock control; My Orders (with Cancel Order while an order is still Processing) history
- Account Settings: profile, address, change password · Help Center · Terms & Conditions · Log Out

## Roles (one app, the role is chosen at sign up)

| Role | What they can do | Demo login |
|---|---|---|
| **Customer** | Shop, cart, checkout (with **voucher codes**), My Orders: cancel, **rate & review**, **report a problem** | sign up |
| **Seller** | Add / edit / list / unlist own products, see orders for own products, **accept** and **mark them ready**, dashboard with **sales chart** | `seller@zelora.com` / `Seller1234` (or Sign Up > "Join as a Seller or Rider") |
| **Delivery Rider** | See packed orders, **accept / reject** a job, **confirm pickup**, **mark delivered**, earnings wallet (cash on delivery becomes Paid) | `rider@zelora.com` / `Rider1234` |
| **Admin** | Dashboard numbers, enable / disable accounts, see all orders, **review disputes** (refund / resolve / reject), see vouchers | `admin@zelora.com` / `Admin1234` |

Order status path: Processing -> Packed (seller) -> Out for Delivery (rider) -> Delivered (rider). Only a Processing order can be cancelled.
Vouchers to try: `WELCOME10` (10% off), `SAVE100` (P100 off P1,000+), `FREESHIP`.
Database version 3: an older install is upgraded in place (nothing is lost).

## Technologies
Java 17 · Android SDK 26–35 · SQLite (`SQLiteOpenHelper`) · AndroidX AppCompat, RecyclerView, ViewPager2, Fragment
· Material Components · Inter + Bricolage Grotesque fonts (SIL OFL)

## How to run
1. Open **Android Studio** (Ladybug 2024.2.1 or newer) → *Open* → choose this `Zelora` folder.
2. Wait for *Gradle Sync* to finish (needs internet the first time).
3. Create/choose an emulator (API 26+) or a phone with USB debugging → press **Run ▶**.
4. First launch shows Login → tap **Sign Up**, create an account, and start shopping.
5. APK: *Build → Build Bundle(s)/APK(s) → Build APK(s)* → `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout
```
Zelora/
├── app/src/main/java/com/zelora/app/
│   ├── db/      DatabaseHelper, UserDao, ProductDao, CartDao, WishlistDao, AddressDao, OrderDao   (backend)
│   ├── model/   User, Product, CartItem, Address, Order
│   ├── util/    Validator, PasswordUtil, SessionManager, Money, ColorUtil
│   └── ui/      Login, SignUp, Main (drawer + bottom nav), Home/Wishlist/Cart/Account fragments,
│                ProductDetail, CheckoutBottomSheet, Orders, AccountSettings, Info + adapters
├── app/src/main/res/   layouts, drawables, colors, fonts, product photos
├── docs/               HOW_IT_WORKS.md, design/ (mockups)
└── README.md
```

## Screenshots
Emulator screenshots are in the [`screenshots/`](screenshots) folder. Design mockups are in `docs/design/`.

## Download and documentation
- APK: see the **Releases** page of this repository (or the `apk/` folder).
- Project documentation (PDF/DOCX): [`documentation/`](Group 9)
- System diagrams: `docs/diagrams/` and `docs/drawio/`

## Developers
[Isidro, Kristina Cassandra P.] - developer
[Eva, Angel T.] - designer
[ Pascual, Leira C. and Espino, Jerome M.] - documentation
[Cabuz, Jewel Ace V.] - tester 
(CT-MAD 213, [BSIS 2C]).

## Version
1.3.1 (versionCode 6) - Customer, Seller, Rider and Admin roles; order notifications; new Seller and Rider screens

## Credits
Product photos and mockups: the project author. Fonts: Inter (Rasmus Andersson) and Bricolage Grotesque (Mathieu Triay), SIL Open Font License.


## Device feature: order status notifications (v1.2.0)
When a seller packs an order, a rider picks it up or delivers it, the customer gets an Android notification the next time the app is opened (`util/NotificationHelper.java`, uses a notification channel and the Android 13 POST_NOTIFICATIONS permission). It is a local notification; server push (Firebase) is future work.

## v1.3.1: logo and new Seller / Rider screens

- Zelora logo: launcher icon (mipmap, all densities) and the Login / Sign Up headers.
- Seller app (bottom nav: Overview, Orders, Products, Shop): greeting, New orders / Sales today / Shop rating tiles,
  7-day sales line chart with % change vs the previous 7 days, "Needs attention" card, and order tabs
  New / Preparing / Ready. Order flow is now Processing -> Preparing ("Accept order") -> Packed ("Mark ready").
- Rider app (bottom nav: Home, Tasks, Wallet, Profile): Online toggle, today's deliveries, incoming request cards,
  and a Delivery Details screen with progress stepper (Accepted, Picked up, Delivered), "Confirm pickup" and "Contact customer".
- Database version 3 (adds `orders.picked_up` and `orders.delivered_at`; existing data is migrated).
- The map on Delivery Details is a placeholder drawing, not a real map. Rider earnings use a flat fee per delivery defined in the app, not real payroll data.

Known limitation: an order that contains products from two different sellers moves through Preparing / Ready as one order,
so the first seller to press "Mark ready" makes it available to riders. Keep one seller per order when demonstrating.

## Diagrams (docs/diagrams, docs/drawio)

Architecture, Use Case, ER, Flowchart, Process Flow and Order Lifecycle, updated for v1.3.1 (Seller and Rider screens,
the Preparing status, `picked_up` / `delivered_at`, notifications). Editable draw.io file: `docs/drawio/Zelora-Diagrams.drawio`.
