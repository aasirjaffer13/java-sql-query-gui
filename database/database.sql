-- =====================================================================
-- CSE2006 - Programming in Java | Group Activity
-- Sample library database:  cse2006_library
--
-- How to run this script:
--   * MySQL Workbench :  open the file and click the lightning bolt
--   * command line    :  mysql -u root -p < database/database.sql
--
-- The script is idempotent (IF NOT EXISTS + INSERT IGNORE), so it can be
-- executed as often as needed without duplicating or losing data.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS cse2006_library
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cse2006_library;

-- ---------------------------------------------------------------------
-- Table structure
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS authors (
    authorID  INT         NOT NULL AUTO_INCREMENT,
    firstName VARCHAR(50) NOT NULL,
    lastName  VARCHAR(50) NOT NULL,
    PRIMARY KEY (authorID)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS titles (
    isbn          VARCHAR(20)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    editionNumber INT          NOT NULL,
    copyrightYear INT          NOT NULL,
    PRIMARY KEY (isbn)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS authorISBN (
    authorID INT         NOT NULL,
    isbn     VARCHAR(20) NOT NULL,
    PRIMARY KEY (authorID, isbn),
    CONSTRAINT fk_authorisbn_authors
        FOREIGN KEY (authorID) REFERENCES authors (authorID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_authorisbn_titles
        FOREIGN KEY (isbn) REFERENCES titles (isbn)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Sample data: authors
-- ---------------------------------------------------------------------

INSERT IGNORE INTO authors (authorID, firstName, lastName) VALUES
    (1,  'Paul',        'Deitel'),
    (2,  'Harvey',      'Deitel'),
    (3,  'Abbey',       'Deitel'),
    (4,  'Dan',         'Quirk'),
    (5,  'Michael',     'Morgano'),
    (6,  'Cay S.',      'Horstmann'),
    (7,  'Herbert',     'Schildt'),
    (8,  'Kathy',       'Sierra'),
    (9,  'Bert',        'Bates'),
    (10, 'Dennis M.',   'Ritchie'),
    (11, 'Brian W.',    'Kernighan'),
    (12, 'Robert C.',   'Martin');

-- ---------------------------------------------------------------------
-- Sample data: titles  (ISBN values are sample data for this project)
-- ---------------------------------------------------------------------

INSERT IGNORE INTO titles (isbn, title, editionNumber, copyrightYear) VALUES
    ('9780134707846', 'Java How to Program',                            11, 2017),
    ('9780134670959', 'Java How to Program, Late Objects',              10, 2015),
    ('9780134580999', 'Java for Programmers',                            2, 2019),
    ('9780134685991', 'Android How to Program',                          9, 2016),
    ('9780132576277', 'Internet & World Wide Web: How to Program',       5, 2007),
    ('9780134590462', 'C How to Program',                                8, 2016),
    ('9780134601540', 'C++ How to Program',                             10, 2017),
    ('9780134681726', 'Python for Programmers',                          1, 2019),
    ('9780134712710', 'Visual Basic How to Program',                     6, 2014),
    ('9780134586458', 'JavaScript How to Program',                       2, 2012),
    ('9780134665635', 'C# How to Program',                               7, 2016),
    ('9780134742939', 'Big Java: Early Objects',                         6, 2020),
    ('9780134670973', 'Java: The Complete Reference',                   11, 2021),
    ('9780596009205', 'Head First Java',                                 2, 2005),
    ('9780131103627', 'The C Programming Language',                      2, 1988),
    ('9780132350882', 'Clean Code',                                      1, 2008);

-- ---------------------------------------------------------------------
-- Sample data: author / ISBN relations
-- ---------------------------------------------------------------------

INSERT IGNORE INTO authorISBN (authorID, isbn) VALUES
    (1,  '9780134707846'),
    (2,  '9780134707846'),
    (1,  '9780134670959'),
    (2,  '9780134670959'),
    (1,  '9780134580999'),
    (2,  '9780134580999'),
    (1,  '9780134685991'),
    (2,  '9780134685991'),
    (5,  '9780134685991'),
    (1,  '9780132576277'),
    (2,  '9780132576277'),
    (3,  '9780132576277'),
    (1,  '9780134590462'),
    (2,  '9780134590462'),
    (4,  '9780134590462'),
    (1,  '9780134601540'),
    (2,  '9780134601540'),
    (1,  '9780134681726'),
    (2,  '9780134681726'),
    (3,  '9780134681726'),
    (1,  '9780134712710'),
    (2,  '9780134712710'),
    (1,  '9780134586458'),
    (2,  '9780134586458'),
    (4,  '9780134586458'),
    (1,  '9780134665635'),
    (2,  '9780134665635'),
    (6,  '9780134742939'),
    (7,  '9780134670973'),
    (8,  '9780596009205'),
    (9,  '9780596009205'),
    (10, '9780131103627'),
    (11, '9780131103627'),
    (12, '9780132350882');
