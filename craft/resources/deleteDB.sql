#DROP DATABASE IF EXISTS craft
#CREATE DATABASE craft
CREATE TABLE IF NOT EXISTS Materials (
  name         VARCHAR(30)   NOT NULL,
  price        INT UNSIGNED  NOT NULL DEFAULT 0,
  note         VARCHAR(30)   NOT NULL DEFAULT '',
  PRIMARY KEY  (name)
);
CREATE TABLE IF NOT EXISTS Customers (
  customerID   INT UNSIGNED  NOT NULL,
  name         VARCHAR(30)   NOT NULL,
  adress       VARCHAR(40)   NOT NULL DEFAULT '',
  zip          VARCHAR(40)   NOT NULL DEFAULT '',
  section      VARCHAR(30),       
  PRIMARY KEY  (customerID)
);
CREATE TABLE IF NOT EXISTS Resources (
  resourceID   INT UNSIGNED  NOT NULL,
  priceperhour INT UNSIGNED  NOT NULL,
  PRIMARY KEY  (resourceID)
);
CREATE TABLE IF NOT EXISTS Staff (
  resourceID   INT UNSIGNED  NOT NULL,
  name         VARCHAR(30)   NOT NULL,
  adress       VARCHAR(40)   NOT NULL DEFAULT '',
  zip          VARCHAR(40)   NOT NULL DEFAULT '',
  telefon      VARCHAR(40)   NOT NULL DEFAULT '',
  PRIMARY KEY  (resourceID),
  FOREIGN KEY  (resourceID)  REFERENCES Resources(resourceID)
);
CREATE TABLE IF NOT EXISTS Machines (
  resourceID   INT UNSIGNED  NOT NULL,
  label        VARCHAR(30)   NOT NULL,
  acquisition  int UNSIGNED  NOT NULL,
  PRIMARY KEY  (resourceID),
  FOREIGN KEY  (resourceID)  REFERENCES Resources(resourceID)
);
CREATE TABLE IF NOT EXISTS Orders (
  orderID      INT UNSIGNED  NOT NULL,
  day          VARCHAR(10)     NOT NULL,
  customerID   INT UNSIGNED  NOT NULL,
  PRIMARY KEY  (orderID),
  FOREIGN KEY  (customerID)  REFERENCES Customers(customerID)
);
CREATE TABLE Orders_Materials (
  name         VARCHAR(30)   NOT NULL,
  orderID      INT UNSIGNED  NOT NULL,
  amount       INT UNSIGNED  NOT NULL,
  PRIMARY KEY  (name, orderID),
  FOREIGN KEY  (name)  REFERENCES Materials(name),
  FOREIGN KEY  (orderID)  REFERENCES Orders(orderID)
);
CREATE TABLE Orders_Resources (
  resourceID   INT UNSIGNED  NOT NULL,
  orderID      INT UNSIGNED  NOT NULL,
  hours        INT UNSIGNED  NOT NULL,
  PRIMARY KEY  (resourceID, orderID),
  FOREIGN KEY  (resourceID)  REFERENCES Resources(resourceID),
  FOREIGN KEY  (orderID)  REFERENCES Orders(orderID)
);