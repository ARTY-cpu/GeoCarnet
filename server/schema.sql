-- Base de données DISTANTE (MySQL).
-- À exécuter une fois (par exemple dans phpMyAdmin) pour créer la table.
CREATE DATABASE IF NOT EXISTS geocarnet CHARACTER SET utf8;
USE geocarnet;

CREATE TABLE IF NOT EXISTS fiches (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    titre     VARCHAR(255),
    note      TEXT,
    latitude  DOUBLE,
    longitude DOUBLE,
    rue       VARCHAR(255),
    date      VARCHAR(50)
);
