# How Zelora is built – logic and code explained

## 1. Big picture
Zelora is a **single-module Android app** written in Java. There is no server: the "backend" is a **local SQLite database**
managed by `SQLiteOpenHelper`. The code is split in four layers, so each class has one job (easy to explain in your defense):

| Layer | Package | Job |
|---|---|---|
| UI | `ui/` + `res/layout` | Screens, fragments, adapters. Reads input, calls DAOs, shows results. |
| Backend / data | `db/` | `DatabaseHelper` creates tables; each `*Dao` does the SQL (CRUD) for one table. |
| Model | `model/` | Plain Java objects (`Product`, `CartItem`, `Order`, `User`, `Address`). |
| Utilities | `util/` | `Validator` (all input rules), `PasswordUtil` (hashing), `SessionManager` (who is logged in), `Money` (₱ format + shipping rule), `ColorUtil` (color dots). |

Your process-flow diagram maps to the code like this:

| Diagram box | Where it lives |
|---|---|
| User Authentication | `LoginActivity`, `SignUpActivity`, `UserDao`, `PasswordUtil`, `SessionManager` |
| Search / Browse, View Product | `HomeFragment`, `ProductDao.query()`, `ProductDetailActivity` |
| Add to Cart, Store Cart in Database | `CartDao` (`cart_items` table), `CartFragment` |
| Apply vouchers / shipping | `Money.shippingFor()` (free shipping from ₱800, else ₱50) |
| Initiate / Submit Payment | `CheckoutBottomSheet` (card is validated, GCash/Maya number validated, COD) → `payments` table |
| Order Processing, Inventory Control | `OrderDao.placeOrder()` (one transaction, decrements stock) |
| Monitor Order Status | `OrdersActivity` + `orders.status` |
| Users / Products / Orders / Payment tables | `DatabaseHelper.onCreate()` |

Seller, Rider and Admin roles in the diagram are outside this app (it is the **buyer** app). The `orders.status`
column already supports Processing → Shipped → Delivered for a future seller/admin app.

## 2. Database (ER diagram)
```
users 1───* cart_items *───1 products
users 1───* wishlist   *───1 products
users 1───1 addresses
users 1───* orders 1───* order_items *───1 products
                   1───1 payments
```
Tables: `users, products, cart_items, wishlist, addresses, orders, order_items, payments`.
Important design decisions (good defense answers):
- **Foreign keys are switched on** (`onConfigure → setForeignKeyConstraintsEnabled(true)`), so you cannot create a cart line for a user that does not exist.
- `cart_items` has `UNIQUE(user_id, product_id, color)`: the same product+color is one line; adding again increases the quantity (`CartDao.add`).
- `order_items` copies `product_name` and `unit_price` at purchase time, so old orders never change if prices change later.
- Money is stored as **integer pesos** (no floating-point rounding bugs).
- Products are **seeded once** in `DatabaseHelper.seedProducts()` (the 4 products from your design).

## 3. Key logic, step by step

### 3.1 Sign up and log in
1. `SignUpActivity.attemptSignUp()` runs `Validator.name/email/password`, checks the two passwords match and the Terms box is ticked.
2. `UserDao.register()` creates a random **salt**, hashes the password with **PBKDF2** (`PasswordUtil.hash`) and inserts the row.
   The plain password is never stored. A duplicate e-mail hits the `UNIQUE` constraint → `SQLiteConstraintException` → returns -1 → "This email is already registered".
3. `LoginActivity.attemptLogin()` → `UserDao.login()` re-hashes the typed password with the stored salt and compares.
4. `SessionManager.login(userId)` saves the id in `SharedPreferences`, so the app opens straight to Home next time. Log Out removes it.

### 3.2 Home: search, chips, sort
`HomeFragment` keeps three variables: `category`, the search text and `sort`. Any change calls `loadProducts()`, which calls ONE query:
```java
productDao.query(category, etSearch.getText().toString(), sort);
```
`ProductDao.query()` builds `SELECT … WHERE category = ? AND (name LIKE ? OR …) ORDER BY …`. User text is passed as `?` arguments
(never concatenated) so SQL injection is impossible. The sort `switch` uses fixed strings only.

### 3.3 Product card grid
`ProductAdapter` (RecyclerView) inflates `item_product.xml` for each product. The heart calls `WishlistDao.toggle()`:
delete the row if it exists, otherwise insert it. `Product.drawableRes()` turns the stored photo name (`img_mug`) into the drawable id.

### 3.4 Product detail
`ProductDetailActivity` receives only the product id, reloads the product from SQLite and wires every control:
- `ViewPager2` + `GalleryAdapter` = swipeable photos; the arrows call `setCurrentItem`; dots are small views recolored in `updateDots()`.
- Color dots: `ColorUtil.circle()` draws the circle; the selected one gets a ring. Quantity is limited by `min(10, stock)`.
- **Add to Cart** → `CartDao.add(userId, productId, color, qty)`.
- **Buy Now** → creates a `CartItem` with `cartId = 0` (meaning "not in the cart table") and opens the checkout sheet with just that item.

### 3.5 Cart
`CartFragment` + `CartAdapter`: plus/minus call `CartDao.updateQuantity()` (UPDATE), the bin calls `CartDao.remove()` (DELETE),
and `updateTotals()` recomputes subtotal, shipping (`Money.shippingFor`) and total every time.

### 3.6 Checkout and placing an order (the most important code)
`CheckoutBottomSheet` is a `BottomSheetDialogFragment`. It shows the items, the saved address (`AddressDao`) and three payment rows.
Tapping **Place Order**:
1. no address → asks for one (validated);
2. Card → dialog; `Validator.cardNumber` uses the **Luhn algorithm**, plus expiry (MM/YY, not in the past) and CVV;
   GCash/Maya → validates a PH mobile number; Cash on Delivery → no extra step;
3. `OrderDao.placeOrder(...)` runs inside **one SQLite transaction**:
```java
db.beginTransaction();
try {
    // insert order  →  for each item: UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?
    //                  (0 rows changed = out of stock → throw)  →  insert order line  →  delete from cart
    // insert payment (PAID for card/wallet, PENDING for cash on delivery)
    db.setTransactionSuccessful();
} finally {
    db.endTransaction();   // rolls back everything if we did not reach setTransactionSuccessful()
}
```
Either the whole order is saved or nothing is. The `stock >= ?` condition makes overselling impossible even if two orders race.
Card numbers are validated and then discarded – they are never saved (payments here are a simulation).

### 3.7 Navigation
`MainActivity` hosts a `DrawerLayout` (the side menu) and a `BottomNavigationView`. Tabs replace a fragment inside `fragmentContainer`
(`showTab()`). The back button closes the drawer first, then returns to the Home tab, then exits. The orange dot on Cart is a
`BadgeDrawable` refreshed by `refreshBadges()` using `CartDao.count()`.

### 3.8 Error handling and validation (rubric items)
- Every field is checked in `Validator` and shown with `setError()`; nothing invalid reaches the database.
- DAO calls are wrapped in `try/catch` in the UI and show a friendly toast; a missing product closes the screen instead of crashing.
- Stock, quantity (max 10), duplicate e-mail, wrong password and expired cards all have explicit messages.

## 4. How the UI matches your design
- All sizes were measured from your mockup frame (390 × 844 → 1 px = 1 dp). Comments at the top of each layout say so.
- Colors live in **one file**, `res/values/colors.xml` (names describe roles, e.g. `bg #F8F3EB`, `btn_primary #4B2215`, `brown #67331B`).
- Fonts: **Inter** (all text) and **Bricolage Grotesque** (checkout headings), bundled in `res/font` (no internet needed).
- Corners, borders, shadows are in `res/drawable/bg_*.xml`; icons are vector drawables (`ic_*.xml`) so they stay sharp on any screen.
- Small deliberate differences: the selected color dot gets a thin ring (so the choice is visible), "Filters" opens a sort dialog,
  and the Wishlist, Cart, Account, Orders, Settings and Help screens (not in your mockups) reuse the same colors, fonts and cards.
- The mockup shows the tote at ₱400 on Home but ₱300 (25% OFF) on its detail page. The app copies this; change
  `SHOW_ORIGINAL_PRICE_ON_CARD` in `ProductAdapter` to `false` to show ₱300 with the old price struck through on the card.
- The mug shows 98 reviews on Home and 92 on its detail page in the mockups; the app uses one value (98) everywhere.

## 5. Test cases (for your Testing section)
| # | Test | Expected |
|---|---|---|
| 1 | Sign up with empty name | "Please enter your full name" |
| 2 | Sign up with `abc` as e-mail | "Enter a valid email address" |
| 3 | Password `abcdefgh` | "Use both letters and numbers" |
| 4 | Passwords differ | "Passwords do not match" |
| 5 | Register the same e-mail twice | "This email is already registered" |
| 6 | Log in with wrong password | "Incorrect email or password." |
| 7 | Search "mug" | Only Ceramic Mug is listed |
| 8 | Chip "Bags" | Only Canvas Tote Bag |
| 9 | Heart a product, open Wishlist | Product appears; un-heart removes it |
| 10 | Add the same product + color twice | One cart line, quantity 2 |
| 11 | Quantity above stock/10 | "Maximum quantity reached" |
| 12 | Cart ₱200 total | Shipping ₱50, total ₱250 |
| 13 | Cart ₱800 or more | Shipping "Free" |
| 14 | Place order with no address | Asks for an address first |
| 15 | Card `4111 1111 1111 1112` | "Card number is invalid" (Luhn) |
| 16 | Card `4111111111111111`, valid expiry, CVV 123 | Order placed, code `ZEL-0000xx` |
| 17 | Cash on Delivery | Order saved, payment shows "Pay on delivery" |
| 18 | After an order | Stock decreases, cart line removed, order in My Orders |
| 19 | Close and reopen the app | Still logged in; Log Out returns to Login |

## 6. Ideas to extend (Intermediate → Advanced)
Seller/Admin role and product CRUD screen · REST API or Firebase instead of local SQLite · barcode scanning of products ·
order notifications · GPS address picker · biometric login.


## Cancel Order (added in v1.0.2)

`OrderDao.cancelOrder(userId, orderId)` runs in one transaction:
1. `UPDATE orders SET status='Cancelled' WHERE id=? AND user_id=? AND status='Processing'` - only the owner, and only while the order is still Processing. If no row changes, the order can no longer be cancelled.
2. Every product in `order_items` gets its quantity added back to `products.stock`.
3. The payment becomes `REFUNDED` (it was PAID) or `CANCELLED` (cash on delivery).

My Orders shows a **Cancel Order** button only on Processing orders and asks for confirmation first.

Extra test cases: T20 cancel a Processing order -> status Cancelled, stock restored, no button any more. T21 cancel a card-paid order -> payment shows Refunded.


## Multi-role version (v1.1.0)

**One app, one database.** Every row in `users` has a `role` (CUSTOMER, SELLER, RIDER, ADMIN) and an `active` flag. After login `Nav.openHome()` opens `MainActivity` (customer), `SellerActivity`, `RiderActivity` or `AdminActivity`. Each of those checks the role again in `onCreate`, so a customer can never open a staff screen. `SignUpActivity` only allows CUSTOMER, SELLER or RIDER; the admin and the demo seller / rider are created by `DatabaseHelper`.

**Shared screen.** `StaffActivity` draws the header + tab chips + a list of `CardRow` cards (`CardAdapter`). A role only says its tabs and what each tab loads, so Seller, Rider and Admin are small classes.

**New tables:** `vouchers`, `voucher_redemptions` (one use per customer), `reviews` (one per customer per product, only after Delivered), `disputes` (Open / Resolved / Rejected), `job_rejections`. New columns: `users.role/active`, `products.seller_id/active`, `orders.voucher_code/discount/rider_id`, `order_items.seller_id`.

**Order lifecycle = guarded UPDATEs** in `OrderDao`, each one changes the row only if the status is right and the actor is allowed, for example `UPDATE orders SET status='Out for Delivery', rider_id=? WHERE id=? AND status='Packed' AND rider_id IS NULL` - when two riders tap at the same time only the first one wins. Cancel restores stock, returns the voucher and refunds the payment. `markDelivered` turns a cash-on-delivery payment from PENDING to PAID in the same transaction.

**Voucher at checkout.** `VoucherDao.check()` validates the code, the minimum spend and one-use-per-customer; `OrderDao.placeOrder()` checks it again inside the transaction, so the screen is never trusted.

**Upgrade from v1.** `DatabaseHelper.onUpgrade()` runs `ALTER TABLE ... ADD COLUMN` for the new columns, creates the new tables, assigns the old products to the demo seller and seeds the vouchers.

Extra test cases: T22 log in as seller, add a product -> it appears on Home. T23 customer orders it, seller taps Pack order, rider accepts, rider marks delivered -> customer sees Delivered and can Rate & Review. T24 customer reports a problem -> admin sees it under Disputes and can Refund. T25 admin disables a customer -> that customer gets "account has been disabled" on login. T26 apply WELCOME10 at checkout -> total drops by 10% and the code cannot be used twice by the same customer.

Not included (honest limits): no live GPS tracking of the rider (status only), seller product photos are chosen from the four built-in pictures, and an order that contains products from several sellers has one shared status.
