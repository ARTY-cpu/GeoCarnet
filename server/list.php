<?php
// Renvoie toutes les fiches au format JSON.
require "db.php";

$resultat = $cnx->query(
    "SELECT titre, note, latitude, longitude, rue, date FROM fiches");

$fiches = [];
while ($ligne = $resultat->fetch_assoc()) {
    // On force les types numériques pour produire un JSON propre.
    $ligne["latitude"]  = (double) $ligne["latitude"];
    $ligne["longitude"] = (double) $ligne["longitude"];
    $fiches[] = $ligne;
}

header("Content-Type: application/json");
echo json_encode($fiches);
?>
