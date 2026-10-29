-- Database schema for OnskeSkyen
DROP TABLE IF EXISTS Bruger;
DROP TABLE IF EXISTS BrugerØnsker;
DROP TABLE IF EXISTS Ønske;
DROP TABLE IF EXISTS TagsØnsker;
DROP TABLE IF EXISTS Tags;
DROP TABLE IF EXISTS Reservering;

CREATE TABLE Bruger (
                        bruger_id INT AUTO_INCREMENT PRIMARY KEY,
                        userName varchar(100) NOT NULL,
                        email varchar(100),
                        password varchar(100)
);

CREATE TABLE Ønske (
                       ønske_id INT AUTO_INCREMENT PRIMARY KEY,
                       navn varchar(100) NOT NULL,
                       pris INT

);

CREATE TABLE BrugerØnsker (
                              brugerØnske INT AUTO_INCREMENT PRIMARY KEY,
                              FOREIGN KEY (bruger_id) REFERENCES Bruger(bruger_id) ON DELETE CASCADE,
                              FOREIGN KEY (ønske_id) REFERENCES Ønske(ønske_id) ON DELETE CASCADE
);

CREATE TABLE Reservering (
                             reserve_id INT AUTO_INCREMENT PRIMARY KEY,
                             FOREIGN KEY (bruger_id) REFERENCES Bruger(bruger_id) ON DELETE CASCADE,
                             FOREIGN KEY (ønske_id) REFERENCES Ønske(ønske_id) ON DELETE CASCADE
);

CREATE TABLE Tags(
                     tag_id INT AUTO_INCREMENT PRIMARY KEY,
                     tag_navn varchar(100)
);

CREATE TABLE TagsØnsker (
                            tagØnske_id INT AUTO_INCREMENT PRIMARY KEY,
                            FOREIGN KEY (tag_id) REFERENCES Tags(tag_id) ON DELETE CASCADE,
                            FOREIGN KEY (ønske_id) REFERENCES Ønske(ønske_id) ON DELETE CASCADE
);