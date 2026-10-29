-- Database schema for OnskeSkyen --

CREATE DATABASE onskeStjerne;
USE onskeStjerne;

DROP TABLE IF EXISTS tagsWish;
DROP TABLE IF EXISTS reserve;
DROP TABLE IF EXISTS wishCounter;
DROP TABLE IF EXISTS userWishes;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS wish;
DROP TABLE IF EXISTS users;

CREATE TABLE users (

                       user_id INT PRIMARY KEY AUTO_INCREMENT,
                       userName VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL
);

CREATE TABLE wish (

                      wish_id INT PRIMARY KEY AUTO_INCREMENT,
                      name    VARCHAR(100) NOT NULL,
                      price   INT
);

CREATE TABLE userWishes (

                            userWish_id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id     INT NOT NULL,
                            FOREIGN KEY (user_id) REFERENCES users (user_id),
                            wish_id     INT NOT NULL,
                            FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
);

CREATE TABLE wishCounter (

                             wishCount_id INT PRIMARY KEY AUTO_INCREMENT,
                             wish_count INT DEFAULT 0,
                             wish_id INT NOT NULL,
                             FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
);

CREATE TABLE reserve (

                         reserve_id INT PRIMARY KEY AUTO_INCREMENT,
                         user_id INT NOT NULL,
                         FOREIGN KEY (user_id) REFERENCES users (user_id),
                         wish_id INT NOT NULL,
                         FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
);

CREATE TABLE tags (

                      tag_id INT PRIMARY KEY AUTO_INCREMENT,
                      tag_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tagsWish (

                          tagsWish_id INT PRIMARY KEY AUTO_INCREMENT,
                          tag_id INT NOT NULL,
                          FOREIGN KEY (tag_id) REFERENCES tags (tag_id),
                          wish_id INT NOT NULL,
                          FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
);