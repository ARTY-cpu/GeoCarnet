package com.example.geocarnet.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geocarnet.R;
import com.example.geocarnet.db.LocalDb;
import com.example.geocarnet.model.Fiche;
import com.example.geocarnet.net.RemoteApi;

import java.util.List;

/**
 * Écran "Synchro" (consigne n°4 : base externe MySQL).
 *   - "Envoyer"   : pousse vers le serveur les fiches pas encore synchronisées.
 *   - "Récupérer" : télécharge les fiches du serveur et les ajoute en local.
 *
 * Le réseau est fait dans un Thread séparé pour ne pas bloquer l'interface
 * (Android interdit le réseau sur le thread principal).
 */
public class SyncFragment extends Fragment {

    private TextView etat;
    private LocalDb db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View vue = inflater.inflate(R.layout.fragment_sync, container, false);
        db = new LocalDb(getContext());
        etat = vue.findViewById(R.id.texte_etat);

        Button envoyer = vue.findViewById(R.id.bouton_envoyer);
        envoyer.setOnClickListener(v -> envoyer());

        Button recevoir = vue.findViewById(R.id.bouton_recevoir);
        recevoir.setOnClickListener(v -> recevoir());

        return vue;
    }

    // Envoie vers MySQL toutes les fiches non synchronisées.
    private void envoyer() {
        etat.setText(R.string.sync_en_cours);
        new Thread(() -> {
            int envoyees = 0;
            try {
                List<Fiche> aEnvoyer = db.nonSynchronisees();
                for (Fiche f : aEnvoyer) {
                    if (RemoteApi.envoyer(f)) {
                        db.marquerSynchronisee(f.id);
                        envoyees++;
                    }
                }
            } catch (Exception e) {
                afficher(getString(R.string.sync_erreur));
                return;
            }
            afficher(getString(R.string.sync_envoyees, envoyees));
        }).start();
    }

    // Récupère les fiches depuis MySQL et les enregistre en local.
    private void recevoir() {
        etat.setText(R.string.sync_en_cours);
        new Thread(() -> {
            try {
                List<Fiche> recues = RemoteApi.recevoir();
                for (Fiche f : recues) {
                    db.ajouter(f);
                }
                afficher(getString(R.string.sync_recues, recues.size()));
            } catch (Exception e) {
                afficher(getString(R.string.sync_erreur));
            }
        }).start();
    }

    // Met à jour le texte sur le thread principal (obligatoire pour toucher l'UI).
    private void afficher(String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> etat.setText(message));
    }
}
