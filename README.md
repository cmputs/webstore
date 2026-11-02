# Mallow & Moon - Plushie Boutique

A Java 21 / Spring Boot 4.1.2 project using the code from
WebStore_SpringBoot_Student_Tutorial_HE (1).html, with a pastel pink plushie
boutique storefront.
Used to be a computer store, i made some changes for it to look pretty and changed the names, I'm sure it's fine.

## Open the store

On the computer where this project was built, MySQL is running and the webstore
database has been created. No database password is stored in these files.

Open this folder in VS Code. In its PowerShell terminal run:

```powershell
.\Start-WebStore.ps1
```

The launcher uses your DB_PASSWORD environment variable if one is available.
Otherwise, it asks for your local MySQL password with hidden input. It uses the
included webstore.jar. Keep the terminal open, wait for Started
WebstoreApplication, and open:

http://localhost:8080/

Stop the server with Ctrl+C. If the store is already running, open the URL
instead of starting another copy.

## Edit and run the source

Open the webstore folder containing pom.xml and mvnw.cmd in VS Code. With your
database credentials set in that terminal, run:

```powershell
.\mvnw.cmd spring-boot:run
```

After Java edits, stop and start this command again. Changes to the source do
not update the bundled webstore.jar. Rebuild it when you want to use the launcher
with your changed code:

```powershell
.\mvnw.cmd package
Copy-Item .\target\webstore-0.0.1-SNAPSHOT.jar .\webstore.jar
```

Maven is downloaded by the included wrapper; a separate Maven installation is
not needed. The first source build requires an internet connection.

## What is implemented

- Six entities: Product, StoreUser, ShoppingCart, CartItem, CustomerOrder,
  and OrderItem, plus OrderStatus.
- Six Spring Data JPA repositories.
- Product, shopping-cart, and order services.
- Product REST API: list, get, create, update, and delete.
- Validation for product names, prices, and stock.
- The Mallow & Moon HTML/CSS/JavaScript store page, including browser cart,
  quantity controls, totals, stock limits, and localStorage persistence.
- Five sample plushies, inserted only when the products table is empty.
- Seven integration tests and a live API verification script.

The product API uses MySQL. As in the teacher's guide, the page's shopping cart
uses the browser's localStorage. Cart and order services exist, but the tutorial
has no cart/order controllers, checkout button, or user login. Testing the order
service does not add a checkout feature to the website.

## API

| Method | URL | Result |
| --- | --- | --- |
| GET | /api/products | All products |
| GET | /api/products/{id} | One product; 404 when absent |
| POST | /api/products | Save a product; 201 Created |
| PUT | /api/products/{id} | Update a product |
| DELETE | /api/products/{id} | Delete a product; 204 No Content |

### Test with Thunder Client

The tutorial's Thunder Client exercise covers creating and reading products.
Import [Mallow-Moon-API.postman_collection.json](./thunder-tests/Mallow-Moon-API.postman_collection.json)
using Thunder Client's **Collection → Import**; Thunder Client supports Postman
2.1 collection files. Start the app first, then run **Create plushie (POST)** and
copy its returned `id` into the collection's `productId` variable. Run **List
products (GET)** and **Fetch one product by id (GET)** to verify the result.
The POST creates a product that remains in MySQL until deleted. The tutorial
does not provide cart or order REST endpoints, so those services cannot be
tested through Thunder Client yet.

Example POST body:

```json
{
  "name": "Mallow Bunny",
  "description": "A floppy-eared cuddle buddy",
  "price": 89.90,
  "stockQuantity": 25
}
```

Use Content-Type: application/json. Open the page through Spring Boot on port
8080, as shown above, so its /api/products requests reach this server.

## Verify

```powershell
.\mvnw.cmd test
```

Tests use a separate, temporary H2 database. They cover product persistence,
missing products, duplicate carts, invalid quantity, empty-cart rejection,
successful order creation, purchase-price history, and rollback on insufficient
stock. They do not erase your MySQL data.

With the real application running, verify all five product API methods:

```powershell
.\scripts\Verify-Api.ps1
```

The script creates, updates, and deletes its own temporary verification product.

## Database setup on another computer

1. Install Java 21 and MySQL 8.0 or later.
2. Run database/create-database.sql in MySQL Workbench.
3. Start the launcher and enter your own MySQL password when asked.

application.properties uses these environment settings:

| Setting | Default |
| --- | --- |
| DB_URL | jdbc:mysql://localhost:3306/webstore |
| DB_USERNAME | root |
| DB_PASSWORD | empty; the launcher prompts when missing |
| PORT | 8080 |
| SEED_DEMO_DATA | true |

Hibernate creates the six tables from the entity mappings on startup. Setting
SEED_DEMO_DATA to false disables sample-product creation. Existing products are
kept when the application restarts. Clearing the browser cart does not delete
products from MySQL.

## Learn how it works

Read EXPLAINED.md for a walkthrough using the actual classes in this project.

Additions to the teacher's source: sample-product initialization, a launcher,
environment overrides for the connection/port, local-only server binding,
automated checks, and these explanations. H2 is a test-only dependency; the
running store uses MySQL.
