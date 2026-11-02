CREATE DATABASE IF NOT EXISTS webstore
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE webstore;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Replace legacy computer tutorial items with Mallow & Moon plushies if present
DELETE FROM products WHERE name IN (
    'Wireless Mouse',
    'Mechanical Keyboard',
    'USB-C Hub',
    'Laptop Stand',
    'Noise-Cancelling Headphones'
);

-- Seed plushie products when the table is empty
INSERT INTO products (name, description, price, stock_quantity)
SELECT * FROM (
    SELECT 'Mallow Bunny' AS name, 'A floppy-eared cuddle buddy for slow, cozy mornings.' AS description, 89.90 AS price, 25 AS stock_quantity
    UNION ALL
    SELECT 'Peachy Pudding Bear', 'A squishy little bear with the sweetest, sunny smile.', 129.00, 12
    UNION ALL
    SELECT 'Cloudlet Kitten', 'A soft cloud-colored kitty made for naps and daydreams.', 99.90, 18
    UNION ALL
    SELECT 'Strawberry Mochi Pup', 'A rosy-cheeked puppy with an extra-squishy hug.', 119.00, 8
    UNION ALL
    SELECT 'Sleepy Star Axolotl', 'A dreamy water friend who loves bedtime stories.', 149.00, 0
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM products LIMIT 1);
