package com.example.geocarnet.net;

import com.example.geocarnet.model.Fiche;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Communication avec la base MySQL DISTANTE (consigne n°4).
 *
 * On ne se connecte PAS directement à MySQL depuis le téléphone (déconseillé et
 * peu sûr). On passe par une petite API PHP (add.php / list.php) qui, elle,
 * parle à MySQL. C'est l'approche standard pour une appli mobile.
 *
 * Tout le réseau est écrit à la main avec HttpURLConnection, et le JSON est
 * analysé à la main -> on respecte la consigne n°0 (pas de Retrofit/Volley).
 *
 * IMPORTANT : ces méthodes font du réseau, donc elles DOIVENT être appelées
 * depuis un thread secondaire (jamais sur le thread principal de l'interface).
 */
public class RemoteApi {

    // À adapter à votre serveur PHP.
    // 10.0.2.2 = le "localhost" de votre PC, vu depuis l'émulateur Android.
    private static final String BASE = "http://10.0.2.2/geocarnet/";

    /** Envoie une fiche vers le serveur (requête POST). Renvoie true si OK. */
    public static boolean envoyer(Fiche f) throws Exception {
        URL url = new URL(BASE + "add.php");
        HttpURLConnection co = (HttpURLConnection) url.openConnection();
        co.setRequestMethod("POST");
        co.setDoOutput(true);
        co.setConnectTimeout(8000);

        // On construit le corps de la requête au format "cle=valeur&cle=valeur".
        String corps = "titre=" + enc(f.titre)
                + "&note=" + enc(f.note)
                + "&latitude=" + f.latitude
                + "&longitude=" + f.longitude
                + "&rue=" + enc(f.rue)
                + "&date=" + enc(f.date);

        OutputStream os = co.getOutputStream();
        os.write(corps.getBytes("UTF-8"));
        os.close();

        int code = co.getResponseCode();
        String reponse = lire(co);
        co.disconnect();

        // Le serveur renvoie {"ok":true} si l'insertion a réussi.
        return code == 200 && reponse.contains("\"ok\":true");
    }

    /** Récupère toutes les fiches stockées sur le serveur (requête GET). */
    public static List<Fiche> recevoir() throws Exception {
        URL url = new URL(BASE + "list.php");
        HttpURLConnection co = (HttpURLConnection) url.openConnection();
        co.setRequestMethod("GET");
        co.setConnectTimeout(8000);

        String reponse = lire(co);
        co.disconnect();

        List<Fiche> liste = new ArrayList<>();
        // La réponse est un tableau JSON : [ {...}, {...} ]. On l'analyse à la main.
        JSONArray tableau = new JSONArray(reponse);
        for (int i = 0; i < tableau.length(); i++) {
            JSONObject o = tableau.getJSONObject(i);
            Fiche f = new Fiche();
            f.titre = o.optString("titre");
            f.note = o.optString("note");
            f.latitude = o.optDouble("latitude");
            f.longitude = o.optDouble("longitude");
            f.rue = o.optString("rue");
            f.date = o.optString("date");
            f.synchro = 1; // elle vient du serveur, donc déjà synchronisée
            liste.add(f);
        }
        return liste;
    }

    // Encode une valeur pour l'URL (gère les espaces, accents, etc.).
    private static String enc(String s) throws Exception {
        if (s == null) s = "";
        return URLEncoder.encode(s, "UTF-8");
    }

    // Lit toute la réponse texte renvoyée par le serveur.
    private static String lire(HttpURLConnection co) throws Exception {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(co.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String ligne;
        while ((ligne = br.readLine()) != null) {
            sb.append(ligne);
        }
        br.close();
        return sb.toString();
    }
}
