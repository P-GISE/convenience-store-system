-- Reviewed fresh-database bootstrap only. Never mount into MySQL's auto-init directory.
CREATE TABLE order_types (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255));
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('STORE_OWNER','HEADQUARTERS') NOT NULL
);
CREATE TABLE inventory (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_type_id BIGINT NOT NULL UNIQUE,
    quantity INT NOT NULL,
    status ENUM('COMPLETED','ACTIVE','DISPOSED') NOT NULL,
    completion_date DATETIME(6) NOT NULL,
    FOREIGN KEY (order_type_id) REFERENCES order_types(id)
);
CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_type_id BIGINT,
    requester_id BIGINT,
    quantity INT,
    order_date DATETIME(6),
    status ENUM('RECEIVED','PREPARATION','DEPARTED','ARRIVED'),
    status_date DATETIME(6),
    FOREIGN KEY (order_type_id) REFERENCES order_types(id),
    FOREIGN KEY (requester_id) REFERENCES users(id)
);
CREATE TABLE stock_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_type_id BIGINT NOT NULL,
    inventory_id BIGINT NOT NULL,
    quantity INT,
    transaction_date DATETIME(6),
    dispose_time DATETIME(6),
    status ENUM('PENDING','DISPOSED','COMPLETED') NOT NULL,
    CONSTRAINT uk_stock_transactions_order UNIQUE (order_id),
    INDEX idx_dispose_time (dispose_time),
    INDEX idx_status (status),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (order_type_id) REFERENCES order_types(id),
    FOREIGN KEY (inventory_id) REFERENCES inventory(id)
);
CREATE TABLE customer_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_type_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    customer_name VARCHAR(80) NOT NULL,
    customer_phone VARCHAR(40) NOT NULL,
    fulfillment_type ENUM('PICKUP','DELIVERY') NOT NULL,
    delivery_address VARCHAR(240),
    request_memo VARCHAR(240),
    status ENUM('RECEIVED','PREPARING','READY_FOR_PICKUP','OUT_FOR_DELIVERY','COMPLETED','CANCELLED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    status_date DATETIME(6) NOT NULL,
    INDEX idx_customer_order_status (status),
    INDEX idx_customer_order_phone (customer_phone),
    FOREIGN KEY (order_type_id) REFERENCES order_types(id)
);

-- Spring Session JDBC MySQL layout (Spring Session 3.4).
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL PRIMARY KEY,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    UNIQUE INDEX SPRING_SESSION_IX1 (SESSION_ID),
    INDEX SPRING_SESSION_IX2 (EXPIRY_TIME),
    INDEX SPRING_SESSION_IX3 (PRINCIPAL_NAME)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;
CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BLOB NOT NULL,
    PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

-- The application never needs schema privileges; only the separate operator does.
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'convenience'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON convenience.* TO 'convenience'@'%';
