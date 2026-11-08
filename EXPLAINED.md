# How this Spring Boot store works

You already know Java classes and objects. Spring adds a framework that creates
and connects certain objects for you. Spring Boot configures that framework
from your dependencies and settings, and starts an embedded web server.

## 1. Start with the Java main method

Open src/main/java/com/example/sapir/webstore/WebstoreApplication.java.

```java
SpringApplication.run(WebstoreApplication.class, args);
```

This starts Spring, discovers the application's components, configures database
access, and starts the web server. The @SpringBootApplication annotation enables
that setup and component discovery under com.example.sapir.webstore.

The application keeps running so it can answer requests. Maven is the build
tool that downloads libraries and compiles/packages the app; Spring Boot is the
framework setup used inside the running Java app.

## 2. A URL reaches a controller

Open controller/ProductController.java.

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    // ...
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }
}
```

When a browser requests GET /api/products, Spring calls this method.
@RestController means the return value goes into the HTTP response body. With
this application's web dependencies, a List<Product> is converted into JSON.

HTTP is the browser/server request-response protocol. An API is the set of
requests a program accepts. JSON is the text format carrying product data:

```json
[
  {"id": 1, "name": "Mallow Bunny", "description": "A floppy-eared cuddle buddy for slow, cozy mornings.", "price": 89.90, "stockQuantity": 25}
]
```

GET reads, POST creates, PUT updates, and DELETE deletes. The annotations on
the other controller methods connect those HTTP requests to Java methods.
@PathVariable reads an id from the URL; @RequestBody converts JSON into a Java
object; @Valid checks that object against its validation annotations.

## 3. Spring supplies the required objects

The controller has this constructor:

```java
public ProductController(ProductService productService) {
    this.productService = productService;
}
```

You do not write new ProductService(...) in the controller. Spring discovers
ProductService because it has @Service, creates it, and passes it into this
constructor. It also provides that service's ProductRepository dependency.

This is constructor dependency injection: the class declares what it needs,
and Spring supplies those objects. An object managed by Spring is called a bean.
Each class still contains ordinary Java methods.

## 4. The service handles the operation

Open service/ProductService.java. Its getAllProducts() method calls
repository.findAll(). Its other methods define create, update, delete, and
missing-product behavior. For example, getProductById throws a
ResponseStatusException with NOT_FOUND when the id does not exist, producing
an HTTP 404 response.

The controller is responsible for the HTTP interface. The service is where
business rules and operations belong. This separation becomes more useful in
CustomerOrderService, where creating an order has several steps.

## 5. The repository accesses the database

Open repository/ProductRepository.java.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

Product is the entity type. Long is its id type. Spring Data JPA provides the
implementation of methods such as findAll, findById, save, and delete. That is
why this interface can be empty and still work.

JPA defines how Java objects map to database records. Hibernate implements JPA
here and executes the SQL needed to read/write MySQL. A method name such as
ShoppingCartRepository.findByUserId also lets Spring Data derive a query from
the entity's Java property names.

## 6. An entity describes a table

Open entity/Product.java.

- @Entity marks the class as a JPA entity.
- @Table(name = "products") sets its table name.
- @Id marks the primary key.
- @GeneratedValue lets the database generate an id for a new row.
- @Column describes a database column.
- @NotBlank, @NotNull, @DecimalMin, and @Min describe input constraints.
- Lombok's @Getter, @Setter, and @NoArgsConstructor generate Java boilerplate
  during compilation.

Each persisted Product is a row in products. BigDecimal is used for prices.
Saving a new product inserts a row; loading one constructs a Java object from
the stored values.

The connection settings are in src/main/resources/application.properties.
DB_PASSWORD is read from the environment, so your password is not in the code.
ddl-auto=update tells Hibernate to create/update tables from the mappings for
this educational project.

## 7. Follow one request all the way through

```text
Browser: GET /api/products
    -> ProductController.getAllProducts()
    -> ProductService.getAllProducts()
    -> ProductRepository.findAll()
    -> Hibernate executes SQL against MySQL
    <- List<Product>
    <- JSON response
Browser: JavaScript builds the product cards
```

Read these four files in that order: ProductController, ProductService,
ProductRepository, Product. They describe the first complete feature.

## 8. Understand the store page and account controls

Open src/main/resources/static/index.html. Spring Boot serves this file when
you open http://localhost:8080/.

The JavaScript function loadProducts() calls fetch("/api/products"). It reads
the JSON response and renderProducts() creates the cards. On page load,
refreshAccountStatus() calls GET /api/auth/me to show whether the browser's
current session is signed in. The account dialog posts registration details to
/api/auth/register and submits login credentials to Spring Security's /login.

The browser first requests /api/auth/csrf and sends the returned token with
registration and logout. Spring Security creates a server-side session after
successful login; the browser keeps its session identifier in a cookie and
sends it on later requests. Signing out ends the session but does not delete
the StoreUser row. ShoppingCartService and CustomerOrderService still have no
HTTP controllers, so the store page has no cart or checkout feature.

## 9. Understand product categories

Category is a separate entity with its own repository, service, and controller.
The service checks for duplicate names and clears product assignments before
deleting a category, so deleting a category never deletes its products.

Product has an optional `@ManyToOne` reference to Category. Many products can
share one category, and the product row stores that relationship in the nullable
`category_id` foreign-key column. The mapping is one-way: Category has no
collection of products. That keeps product JSON from looping from Category back
to Product and avoids a second mapping of the same relationship.

Use `GET /api/categories` to list categories and
`GET /api/products?categoryId=1` or `GET /api/products?uncategorized=true` to
filter products. The home page and `products-by-category.html` display those
filters; `manage-products.html` changes a product's category.

If a future design required Category to expose its products, an
`@OneToMany(mappedBy = "category")` collection could provide that reverse Java
view without adding another foreign-key column. Returning both sides directly
as JSON would create a circular reference, so a DTO (or a serialization rule)
would be needed. Many-to-many is a different relationship and is unnecessary
while each product may belong to at most one category.

## 10. Understand the other five entities

| Entity/table | Purpose |
| --- | --- |
| StoreUser / store_users | Username and email |
| ShoppingCart / shopping_carts | One cart belonging to one user |
| CartItem / cart_items | A product and quantity within a cart |
| CustomerOrder / customer_orders | A user's order, creation time, and status |
| OrderItem / order_items | A purchased product, quantity, and purchase price |

Product is shared by cart items and order items. A CartItem is needed because
the relationship also has a quantity. OrderItem additionally keeps
priceAtPurchase, so an old order retains its historical price even when the
product price changes.

@ManyToOne means many rows can reference one related object. @JoinColumn names
the foreign-key column. @OneToMany(mappedBy = "cart") points back to the Java
field cart on CartItem. @OneToOne connects a user to their cart.

LAZY relationships are loaded when accessed within an active persistence
context. Removing a CartItem from its cart's collection deletes that row because
the mapping uses orphanRemoval. It does not delete the shared Product.

## 11. Why the order service has @Transactional

createOrderFromCart checks the cart, checks stock, reduces stock, creates order
items with purchase prices, and clears cart items. @Transactional makes these
database changes commit together or roll back together if a runtime exception
occurs. One integration test deliberately causes insufficient stock and verifies
that no partial order or stock reduction remains.

The other tests show the services being used directly from Java. The category
pages extend the product API; shopping-cart and order services remain separate
from the browser because the project does not define cart or order endpoints.

## 10. Understand authentication and sessions

StoreUser adds a nullable passwordHash mapped to password_hash. It is nullable
so older user rows remain valid, but StoreUserDetailsService refuses to
authenticate an account without a hash. StoreUserService validates password
length for BCrypt, encodes the password, and saves the user. The raw password
is never stored.

SecurityConfig allows public catalog reads, registration, and the session
status/token endpoints. Other requests require authentication, and CSRF
protection remains enabled for state-changing requests. AuthController exposes
registration, the current session's identity, and a CSRF token. Spring Security
checks login credentials against StoreUserDetailsService and keeps the
authenticated identity in a server-side session.

Two browser profiles have separate session cookies, even though they connect
to the same database. Logging out one profile ends only that session; the
registered account remains in MySQL.

## Recommended reading order

1. WebstoreApplication.java - where it starts.
2. SecurityConfig.java - which requests are public and how sessions are protected.
3. AuthController.java - which account endpoints exist.
4. StoreUserService.java and StoreUserDetailsService.java - registration and login lookup.
5. ProductController.java - which catalog requests exist.
6. ProductService.java - what those requests do.
7. ProductRepository.java - how data is accessed.
8. Product.java - what is stored.
9. static/index.html - how the page calls the API.
10. The remaining entities and services - relationships and transactions.

Account registration and session authentication, sample-data initialization,
category browsing and assignment, and automated checks extend the store's
catalog and data services.
