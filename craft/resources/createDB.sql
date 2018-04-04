DELETE FROM Orders_Resources;
DELETE FROM Orders_Materials;
DELETE FROM Orders;
DELETE FROM Machines;
DELETE FROM Employees;
DELETE FROM Resources;
DELETE FROM Customers;
DELETE FROM Materials;

DROP TABLE Orders_Resources;
DROP TABLE Orders_Materials;
DROP TABLE Orders;
DROP TABLE Machines;
DROP TABLE Employees;
DROP TABLE Resources;
DROP TABLE Customers;
DROP TABLE Materials;

CREATE TABLE IF NOT EXISTS Materials (
  name         VARCHAR(300)   NOT NULL,
  price        INT UNSIGNED  NOT NULL DEFAULT 0,
  note         VARCHAR(200)   NOT NULL DEFAULT '',
  PRIMARY KEY  (name)
);
CREATE TABLE IF NOT EXISTS Customers (
  customerID   BIGINT UNSIGNED  NOT NULL,
  name         VARCHAR(150)   NOT NULL,
  adress       VARCHAR(300)   NOT NULL DEFAULT '',
  zip          VARCHAR(200)   NOT NULL DEFAULT '',
  section      VARCHAR(200),       
  PRIMARY KEY  (customerID)
);
CREATE TABLE IF NOT EXISTS Resources (
  resourceID   BIGINT UNSIGNED  NOT NULL,
  priceperhour INT UNSIGNED  NOT NULL,
  PRIMARY KEY  (resourceID)
);
CREATE TABLE IF NOT EXISTS Employees (
  resourceID   BIGINT UNSIGNED  NOT NULL,
  name         VARCHAR(200)   NOT NULL,
  adress       VARCHAR(300)   NOT NULL DEFAULT '',
  zip          VARCHAR(200)   NOT NULL DEFAULT '',
  telefon      VARCHAR(300)   NOT NULL DEFAULT '',
  PRIMARY KEY  (resourceID),
  FOREIGN KEY  (resourceID)  REFERENCES Resources(resourceID)
);
CREATE TABLE IF NOT EXISTS Machines (
  resourceID   BIGINT UNSIGNED  NOT NULL,
  label        VARCHAR(200)   NOT NULL,
  acquisition  int UNSIGNED  NOT NULL,
  PRIMARY KEY  (resourceID),
  FOREIGN KEY  (resourceID)  REFERENCES Resources(resourceID)
);
CREATE TABLE IF NOT EXISTS Orders (
  orderID      BIGINT UNSIGNED  NOT NULL,
  day          VARCHAR(200)     NOT NULL,
  customerID   BIGINT UNSIGNED  NOT NULL,
  PRIMARY KEY  (orderID),
  FOREIGN KEY  (customerID)  REFERENCES Customers(customerID)
);
CREATE TABLE IF NOT EXISTS Orders_Materials (
  name         VARCHAR(200)   NOT NULL,
  orderID      BIGINT UNSIGNED  NOT NULL,
  amount       INT UNSIGNED  NOT NULL,
  PRIMARY KEY  (name, orderID),
  FOREIGN KEY  (name)  REFERENCES Materials(name),
  FOREIGN KEY  (orderID)  REFERENCES Orders(orderID)
);
CREATE TABLE IF NOT EXISTS Orders_Resources (
  resourceID   BIGINT UNSIGNED  NOT NULL,
  orderID      BIGINT UNSIGNED  NOT NULL,
  hours        INT UNSIGNED  NOT NULL,
  PRIMARY KEY  (resourceID, orderID),
  FOREIGN KEY  (resourceID)  REFERENCES Resources(resourceID),
  FOREIGN KEY  (orderID)  REFERENCES Orders(orderID)
);