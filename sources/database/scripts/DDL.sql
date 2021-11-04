CREATE DATABASE IF NOT EXISTS demobd CHARACTER SET latin1 COLLATE latin1_swedish_ci;

USE demobd;

SET foreign_key_checks = 0;

DROP TABLE IF EXISTS personas;

SET foreign_key_checks = 1;

CREATE TABLE personas(
   	id INT(2) NOT NULL AUTO_INCREMENT,
	nombre VARCHAR(50) NOT NULL,
    edad INT(2) NULL,
    sexo CHAR NOT NULL,
    
    INDEX (id),
    PRIMARY KEY (`id`)
) ENGINE=INNODB;

CREATE TABLE preferencias(
    id INT(2) NOT NULL AUTO_INCREMENT,
    id_persona INT(2) NOT NULL,
    lenguaje_programacion VARCHAR(50) NOT NULL,

    INDEX (id),
    FOREIGN KEY (id_persona) REFERENCES personas(id),
    PRIMARY KEY (`id`)
) ENGINE=INNODB;