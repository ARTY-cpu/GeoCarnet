package com.example.geocarnet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geocarnet.R;
import com.example.geocarnet.db.LocalDb;
import com.example.geocarnet.model.Fiche;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Écran "Fiches" : la liste de toutes les fiches enregistrées localement,
 * avec un champ de recherche et un bouton "+" pour en créer une nouvelle.
 */
public class ListeFragment extends Fragment {

    private LocalDb db;
    private FicheAdapter adapter;
    private EditText recherche;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View vue = inflater.inflate(R.layout.fragment_liste, container, false);

        db = new LocalDb(getContext());

        // La liste déroulante (RecyclerView) + son organisation verticale.
        RecyclerView liste = vue.findViewById(R.id.recycler);
        liste.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FicheAdapter(getContext(), db);
        liste.setAdapter(adapter);

        // Le champ de recherche : à chaque lettre tapée, on refiltre la liste.
        recherche = vue.findViewById(R.id.recherche);
        recherche.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                rafraichir();
            }
        });

        // Le bouton "+" ouvre l'écran de création d'une fiche.
        FloatingActionButton ajout = vue.findViewById(R.id.bouton_ajout);
        ajout.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), EditFicheActivity.class)));

        return vue;
    }

    @Override
    public void onResume() {
        super.onResume();
        rafraichir(); // on recharge la liste à chaque retour sur l'écran
    }

    // Relit la base (avec le filtre courant) et met à jour l'affichage.
    private void rafraichir() {
        List<Fiche> fiches = db.lire(recherche.getText().toString());
        adapter.setFiches(fiches);
    }
}
