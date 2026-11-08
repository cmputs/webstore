CREATE DATABASE IF NOT EXISTS webstore
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE webstore;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    category_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed 5 sample plushie categories when the categories table is empty
INSERT INTO categories (name, description)
SELECT * FROM (
    SELECT 'Woodland Buddies' AS name, 'Floppy-eared bunnies, forest bears, and cozy woodland pals.' AS description
    UNION ALL
    SELECT 'Sweet Treats', 'Pastel dessert-inspired plushies as sweet as pudding and mochi.'
    UNION ALL
    SELECT 'Cloud & Naptime', 'Extra-soft, dreamy companions made for naps and bedtime stories.'
    UNION ALL
    SELECT 'Ocean & Starlight', 'Magical sea creatures and starry night cuddle friends.'
    UNION ALL
    SELECT 'Pocket Pals', 'Mini travel-sized plushies to carry in your bag everywhere.'
) AS category_seed
WHERE NOT EXISTS (SELECT 1 FROM categories LIMIT 1);

-- Replace legacy computer tutorial items with Mallow & Moon plushies if present
DELETE FROM products WHERE name IN (
    'Wireless Mouse',
    'Mechanical Keyboard',
    'USB-C Hub',
    'Laptop Stand',
    'Noise-Cancelling Headphones'
);

-- Seed plushie products when the table is empty
INSERT INTO products (name, description, price, stock_quantity, category_id)
SELECT
    seed.name,
    seed.description,
    seed.price,
    seed.stock_quantity,
    (SELECT id FROM categories WHERE name = seed.category_name LIMIT 1)
FROM (
    SELECT 'Mallow Bunny' AS name, 'A floppy-eared cuddle buddy for slow, cozy mornings.' AS description, 89.90 AS price, 25 AS stock_quantity, 'Woodland Buddies' AS category_name
    UNION ALL
    SELECT 'Peachy Pudding Bear', 'A squishy little bear with the sweetest, sunny smile.', 129.00, 12, 'Sweet Treats'
    UNION ALL
    SELECT 'Cloudlet Kitten', 'A soft cloud-colored kitty made for naps and daydreams.', 99.90, 18, 'Cloud & Naptime'
    UNION ALL
    SELECT 'Strawberry Mochi Pup', 'A rosy-cheeked puppy with an extra-squishy hug.', 119.00, 8, 'Sweet Treats'
    UNION ALL
    SELECT 'Sleepy Star Axolotl', 'A dreamy water friend who loves bedtime stories.', 149.00, 0, 'Ocean & Starlight'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM products LIMIT 1);
