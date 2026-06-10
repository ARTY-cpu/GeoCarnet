package com.example.geocarnet.model;

/**
 * Modèle de données : représente UNE fiche du carnet.
 * Une fiche = une photo + une position GPS + un nom de rue + une note.
 * C'est juste un "sac" de données, sans logique (pour rester simple).
 */
public class Fiche {
    public long id;            // identifiant local (clé primaire SQLite)
    public String titre;       // titre court saisi par l'utilisateur
    public String note;        // texte libre
    public double latitude;    // position GPS (géolocalisation)
    public double longitude;
    public String rue;         // résultat du géocodage (nom de rue / adresse)
    public String cheminPhoto; // chemin du fichier photo sur le téléphone
    public String date;        // date de création (texte simple)
    public int synchro;        // 0 = pas encore envoyée vers MySQL, 1 = envoyée

    public Fiche() {
    }
}
