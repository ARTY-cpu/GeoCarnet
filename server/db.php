<?php
// Connexion à la base MySQL distante.
// Adaptez ces valeurs à votre serveur (avec XAMPP/WAMP/MAMP : souvent root + mot de passe vide).
$hote        = "localhost";
$utilisateur = "root";
$motdepasse  = "";
$base        = "geocarnet";

$cnx = new mysqli($hote, $utilisateur, $motdepasse, $base);
if ($cnx->connect_error) {
    http_response_code(500);
    echo json_encode(["ok" => false, "erreur" => "connexion"]);
    exit;
}
$cnx->set_charset("utf8");
?>
