package com.example.geocarnet.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geocarnet.R;
import com.example.geocarnet.db.LocalDb;
import com.example.geocarnet.model.Fiche;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Fait le lien entre la liste de fiches (les données) et l'affichage de chaque
 * ligne à l'écran. C'est le rôle d'un "Adapter" pour un RecyclerView.
 *
 * - Un CLIC sur une ligne ouvre la fiche (consultation / modification).
 * - Un APPUI LONG sur une ligne supprime la fiche.
 */
public class FicheAdapter extends RecyclerView.Adapter<FicheAdapter.Vue> {

    private final Context contexte;
    private final LocalDb db;
    private List<Fiche> fiches = new ArrayList<>();

    public FicheAdapter(Context contexte, LocalDb db) {
        this.contexte = contexte;
        this.db = db;
    }

    // Remplace les données affichées et redessine la liste.
    public void setFiches(List<Fiche> f) {
        this.fiches = f;
        notifyDataSetChanged();
    }

    // Crée la "boîte" (le layout) d'une ligne.
    @NonNull
    @Override
    public Vue onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View v = LayoutInflater.from(contexte).inflate(R.layout.item_fiche, parent, false);
        return new Vue(v);
    }

    // Remplit une ligne avec les données d'une fiche.
    @Override
    public void onBindViewHolder(@NonNull Vue h, int position) {
        Fiche f = fiches.get(position);
        h.titre.setText(f.titre);
        h.rue.setText(f.rue);
        // Petit repère : la fiche est-elle déjà envoyée vers MySQL ?
        if (f.synchro == 1) {
            h.etat.setText("OK");   // déjà envoyée sur le serveur
        } else {
            h.etat.setText("");     // encore seulement en local
        }

        // On charge la photo depuis le fichier local, si elle existe.
        if (f.cheminPhoto != null && new File(f.cheminPhoto).exists()) {
            h.photo.setImageBitmap(BitmapFactory.decodeFile(f.cheminPhoto));
        } else {
            h.photo.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // CLIC = ouvrir la fiche pour la consulter / la modifier.
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(contexte, EditFicheActivity.class);
            i.putExtra(EditFicheActivity.EXTRA_ID, f.id);
            contexte.startActivity(i);
        });

        // APPUI LONG = supprimer la fiche.
        h.itemView.setOnLongClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return true;
            Fiche aSupprimer = fiches.get(pos);
            db.supprimer(aSupprimer.id);
            fiches.remove(pos);
            notifyItemRemoved(pos);
            Toast.makeText(contexte, R.string.fiche_supprimee, Toast.LENGTH_SHORT).show();
            return true; // true = l'appui long est "consommé"
        });
    }

    @Override
    public int getItemCount() {
        return fiches.size();
    }

    // Garde une référence vers les vues d'une ligne (pour de meilleures perfs).
    static class Vue extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView titre, rue, etat;

        Vue(View v) {
            super(v);
            photo = v.findViewById(R.id.item_photo);
            titre = v.findViewById(R.id.item_titre);
            rue = v.findViewById(R.id.item_rue);
            etat = v.findViewById(R.id.item_etat);
        }
    }
}
