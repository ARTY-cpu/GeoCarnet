package com.example.geocarnet.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geocarnet.db.LocalDb;
import com.example.geocarnet.model.Fiche;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Carte Leaflet/OpenStreetMap dans une WebView (map.html).
 * Java envoie les fiches en JSON au JS ; le JS rappelle Java quand on clique un marqueur.
 */
public class CarteFragment extends Fragment {

    private WebView web;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        web = new WebView(getContext());
        web.getSettings().setJavaScriptEnabled(true); // nécessaire pour Leaflet

        // On expose un objet Java au JavaScript, sous le nom "Android".
        // Le JS pourra donc appeler : Android.ouvrirFiche("3")
        web.addJavascriptInterface(new PontJs(), "Android");

        // Quand la page HTML a fini de charger, on envoie les marqueurs.
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                envoyerMarqueurs();
            }
        });

        web.loadUrl("file:///android_asset/map.html");
        return web;
    }

    // Construit un tableau JSON des fiches et appelle la fonction JavaScript.
    private void envoyerMarqueurs() {
        LocalDb db = new LocalDb(getContext());
        List<Fiche> fiches = db.lire(null);

        JSONArray tableau = new JSONArray();
        try {
            for (Fiche f : fiches) {
                JSONObject o = new JSONObject();
                o.put("id", f.id);          // <-- on envoie l'id pour pouvoir rouvrir la fiche
                o.put("lat", f.latitude);
                o.put("lon", f.longitude);
                o.put("titre", f.titre);
                o.put("rue", f.rue);
                tableau.put(o);
            }
        } catch (Exception e) {
            // ignoré
        }

        // On appelle la fonction "ajouterMarqueurs" définie dans map.html.
        web.evaluateJavascript("ajouterMarqueurs(" + tableau.toString() + ");", null);
    }

    /**
     * Pont JavaScript -> Java.
     * Les méthodes annotées @JavascriptInterface peuvent être appelées depuis le JS.
     * Attention : elles s'exécutent sur un autre thread, donc on revient sur le
     * thread principal (runOnUiThread) pour ouvrir l'écran.
     */
    private class PontJs {
        @JavascriptInterface
        public void ouvrirFiche(String id) {
            if (getActivity() == null) return;
            final long ficheId = Long.parseLong(id);
            getActivity().runOnUiThread(() -> {
                Intent i = new Intent(getActivity(), EditFicheActivity.class);
                i.putExtra(EditFicheActivity.EXTRA_ID, ficheId);
                startActivity(i);
            });
        }
    }
}
