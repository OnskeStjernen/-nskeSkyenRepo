-- Database schema for OnskeSkyen --

CREATE TABLE IF NOT EXISTS users (

                       user_id INT PRIMARY KEY AUTO_INCREMENT,
                       userName VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS wish (

                      wish_id INT PRIMARY KEY AUTO_INCREMENT,
                      name    VARCHAR(100) NOT NULL,
                      price   INT
);

CREATE TABLE IF NOT EXISTS userWishes (

                            userWish_id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id     INT NOT NULL,
                            FOREIGN KEY (user_id) REFERENCES users (user_id)
                            ON DELETE CASCADE,

                            wish_id     INT NOT NULL,
                            FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
                            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS wishCounter (

                             wishCount_id INT PRIMARY KEY AUTO_INCREMENT,
                             wish_count INT DEFAULT 0,
                             wish_id INT NOT NULL,
                             FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
);

CREATE TABLE IF NOT EXISTS reserve (

                         reserve_id INT PRIMARY KEY AUTO_INCREMENT,
                         user_id INT NOT NULL,
                         FOREIGN KEY (user_id) REFERENCES users (user_id),
                         wish_id INT NOT NULL,
                         FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
);

CREATE TABLE IF NOT EXISTS tags (

                      tag_id INT PRIMARY KEY AUTO_INCREMENT,
                      tag_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tagsWish (

                          tagsWish_id INT PRIMARY KEY AUTO_INCREMENT,
                          tag_id INT NOT NULL,
                          FOREIGN KEY (tag_id) REFERENCES tags (tag_id)
                          ON DELETE CASCADE,

                          wish_id INT NOT NULL,
                          FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
                          ON DELETE CASCADE
);
