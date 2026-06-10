<?php
// Reçoit une fiche en POST et l'insère dans MySQL.
require "db.php";

$titre     = $_POST["titre"]     ?? "";
$note      = $_POST["note"]      ?? "";
$latitude  = $_POST["latitude"]  ?? 0;
$longitude = $_POST["longitude"] ?? 0;
$rue       = $_POST["rue"]       ?? "";
$date      = $_POST["date"]      ?? "";

// Requête préparée : protège contre les injections SQL.
$req = $cnx->prepare(
    "INSERT INTO fiches (titre, note, latitude, longitude, rue, date)
     VALUES (?, ?, ?, ?, ?, ?)");
$req->bind_param("ssddss", $titre, $note, $latitude, $longitude, $rue, $date);
$ok = $req->execute();

header("Content-Type: application/json");
echo json_encode(["ok" => $ok]);
?>
