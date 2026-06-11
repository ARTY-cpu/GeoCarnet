package com.example.geocarnet;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.geocarnet.ui.AProposFragment;
import com.example.geocarnet.ui.CarteFragment;
import com.example.geocarnet.ui.ListeFragment;
import com.example.geocarnet.ui.SyncFragment;
import com.google.android.material.navigation.NavigationView;

/**
 * Activité principale ( interface ergonomique).
 * Elle contient :
 *   - une barre d'outils en haut (Toolbar),
 *   - un menu latéral coulissant (Navigation Drawer).
 * Selon l'entrée choisie dans le menu, on affiche un Fragment différent
 * dans la zone centrale.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) La barre d'outils, qui sert aussi de barre d'action.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 2) Le menu latéral + le bouton "hamburger" qui l'ouvre/ferme.
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigation = findViewById(R.id.nav_view);
        navigation.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.ouvrir_menu, R.string.fermer_menu);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // 3) Au démarrage, on affiche la liste des fiches.
        if (savedInstanceState == null) {
            afficher(new ListeFragment(), getString(R.string.menu_liste));
        }
    }

    // Appelée quand on clique sur une entrée du menu latéral.
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_liste) {
            afficher(new ListeFragment(), getString(R.string.menu_liste));
        } else if (id == R.id.nav_carte) {
            afficher(new CarteFragment(), getString(R.string.menu_carte));
        } else if (id == R.id.nav_sync) {
            afficher(new SyncFragment(), getString(R.string.menu_sync));
        } else if (id == R.id.nav_apropos) {
            afficher(new AProposFragment(), getString(R.string.menu_apropos));
        }
        drawer.closeDrawer(GravityCompat.START); // on referme le menu après le clic
        return true;
    }

    // Remplace le fragment affiché et met à jour le titre de la barre.
    private void afficher(Fragment fragment, String titre) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.conteneur, fragment)
                .commit();
        setTitle(titre);
    }

    // Si le menu est ouvert, le bouton "retour" le referme d'abord.
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
