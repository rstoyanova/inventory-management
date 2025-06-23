CREATE TABLE IF NOT EXISTS transactions (
    id serial PRIMARY KEY NOT NULL,
    item_name varchar(255) NOT NULL,
    quantity numeric NOT NULL,
    unit varchar(255) NOT NULL,
    price_per_unit numeric(10,2),
    warehouse_name varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS stocks (
    id serial PRIMARY KEY NOT NULL,
    item_name varchar(255) NOT NULL,
    quantity numeric NOT NULL,
    unit varchar(255) NOT NULL,
    price_per_unit numeric(10,2),
    warehouse_name varchar(255) NOT NULL,
    CONSTRAINT unique_item_warehouse_price UNIQUE (item_name, warehouse_name, price_per_unit)
);
