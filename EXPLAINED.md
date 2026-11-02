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

## 8. Understand the store page

Open src/main/resources/static/index.html. Spring Boot serves this file when
you open http://localhost:8080/.

The JavaScript function loadProducts() calls fetch("/api/products"). It reads
the JSON response and renderProducts() creates the cards. Clicking Add to cart
updates a JavaScript array. saveCart() serializes that array into localStorage,
so the same browser can restore its cart after refreshing the page.

This is a second storage location: products are in MySQL, while this page's cart
is in localStorage. The page does not call ShoppingCartService or
CustomerOrderService. Those services need HTTP controllers before a browser
can use them through the server.

## 9. Understand the other five entities

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

## 10. Why the order service has @Transactional

createOrderFromCart checks the cart, checks stock, reduces stock, creates order
items with purchase prices, and clears cart items. @Transactional makes these
database changes commit together or roll back together if a runtime exception
occurs. One integration test deliberately causes insufficient stock and verifies
that no partial order or stock reduction remains.

The other tests show the services being used directly from Java. The website
still matches the teacher's current tutorial: a product API plus a browser cart.

## Recommended reading order

1. WebstoreApplication.java - where it starts.
2. ProductController.java - which requests exist.
3. ProductService.java - what those requests do.
4. ProductRepository.java - how data is accessed.
5. Product.java - what is stored.
6. static/index.html - how the page calls the API.
7. The remaining entities and services - relationships and transactions.

Sample-product initialization and the automated checks were added to make this
copy easy to open and verify; the 19 source files from your teacher's guide are
otherwise preserved.
