package com.example.geocarnet.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.geocarnet.R;
import com.example.geocarnet.db.LocalDb;
import com.example.geocarnet.model.Fiche;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Écran d'une fiche. Il sert à la FOIS à :
 *   - CRÉER une nouvelle fiche (on arrive sans identifiant),
 *   - CONSULTER / MODIFIER une fiche existante (on arrive avec son identifiant).
 *
 * Il réunit plusieurs consignes :
 *   - consigne n°3 : prendre une photo et l'enregistrer localement,
 *   - consigne n°2 : lire la position GPS en temps réel,
 *   - consigne n°2 : géocodage (transformer lat/lon en nom de rue),
 *   - consigne n°5 : enregistrer / modifier / supprimer dans SQLite.
 */
public class EditFicheActivity extends AppCompatActivity implements LocationListener {

    // Nom de l'information transmise depuis la liste pour ouvrir une fiche existante.
    public static final String EXTRA_ID = "id_fiche";

    private static final int CODE_PHOTO = 1;
    private static final int CODE_PERMISSIONS = 2;

    private EditText champTitre, champNote;
    private TextView texteGps;
    private ImageView apercu;
    private Button boutonSupprimer;

    private LocationManager gps;
    private LocalDb db;

    private long ficheId = -1;          // -1 = nouvelle fiche ; sinon = fiche existante
    private String cheminPhoto;         // chemin de la photo sur le téléphone
    private double latitude, longitude;
    private String rue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_fiche);

        db = new LocalDb(this);
        champTitre = findViewById(R.id.champ_titre);
        champNote = findViewById(R.id.champ_note);
        texteGps = findViewById(R.id.texte_gps);
        apercu = findViewById(R.id.apercu_photo);
        boutonSupprimer = findViewById(R.id.bouton_supprimer);

        Button boutonPhoto = findViewById(R.id.bouton_photo);
        boutonPhoto.setOnClickListener(v -> prendrePhoto());

        Button boutonSave = findViewById(R.id.bouton_enregistrer);
        boutonSave.setOnClickListener(v -> enregistrer());

        boutonSupprimer.setOnClickListener(v -> supprimer());

        gps = (LocationManager) getSystemService(LOCATION_SERVICE);

        // A-t-on reçu l'identifiant d'une fiche existante ?
        ficheId = getIntent().getLongExtra(EXTRA_ID, -1);
        if (ficheId != -1) {
            // MODE CONSULTATION / MODIFICATION : on charge la fiche et on l'affiche.
            chargerFiche();
            setTitle(R.string.titre_fiche_existante);
            boutonSupprimer.setVisibility(View.VISIBLE);
        } else {
            // MODE CRÉATION : on demande les permissions et on lance le GPS.
            setTitle(R.string.titre_nouvelle_fiche);
            boutonSupprimer.setVisibility(View.GONE);
            demanderPermissions();
        }
    }

    // Charge une fiche existante depuis SQLite et remplit l'écran avec ses infos.
    private void chargerFiche() {
        Fiche f = db.lireUne(ficheId);
        if (f == null) return;
        champTitre.setText(f.titre);
        champNote.setText(f.note);
        latitude = f.latitude;
        longitude = f.longitude;
        rue = f.rue;
        cheminPhoto = f.cheminPhoto;
        texteGps.setText(getString(R.string.gps_format, latitude, longitude, rue));
        if (cheminPhoto != null && new File(cheminPhoto).exists()) {
            apercu.setImageURI(Uri.fromFile(new File(cheminPhoto)));
        }
    }

    // ----- PERMISSIONS (obligatoires depuis Android 6) ---------------------

    private void demanderPermissions() {
        String[] perms = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        };
        ActivityCompat.requestPermissions(this, perms, CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (ficheId == -1) demarrerGps(); // GPS seulement en création
    }

    // ----- GÉOLOCALISATION EN TEMPS RÉEL -----------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        if (ficheId == -1) demarrerGps(); // on ne relance pas le GPS pour une fiche existante
    }

    private void demarrerGps() {
        // Sans permission accordée, on ne fait rien.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            // 1) On affiche TOUT DE SUITE la dernière position connue (s'il y en a une),
            //    sans attendre un nouveau point GPS.
            Location derniere = gps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (derniere == null) {
                derniere = gps.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (derniere != null) {
                appliquerPosition(derniere);
            }

            // 2) On demande des mises à jour régulières : c'est le "temps réel".
            //    On écoute le GPS ET le réseau (le réseau répond souvent plus vite,
            //    notamment sur l'émulateur).
            if (gps.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, this);
            }
            if (gps.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                gps.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, this);
            }
        } catch (SecurityException e) {
            // permission retirée entre-temps : on ignore
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gps.removeUpdates(this); // on coupe le GPS pour économiser la batterie
    }

    // Appelée automatiquement à CHAQUE nouvelle position reçue.
    @Override
    public void onLocationChanged(@NonNull Location location) {
        appliquerPosition(location);
    }

    // Met à jour lat/lon + le nom de rue + le texte affiché à l'écran.
    private void appliquerPosition(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        rue = geocoder(latitude, longitude);
        texteGps.setText(getString(R.string.gps_format, latitude, longitude, rue));
    }

    // GÉOCODAGE : transforme une position (lat/lon) en nom de rue.
    // On utilise la classe Geocoder du SDK Android (autorisée : ce n'est pas
    // une bibliothèque tierce, elle fait partie d'Android).
    private String geocoder(double lat, double lon) {
        try {
            Geocoder g = new Geocoder(this, Locale.getDefault());
            List<Address> adresses = g.getFromLocation(lat, lon, 1);
            if (adresses != null && !adresses.isEmpty()) {
                Address a = adresses.get(0);
                String rueTrouvee = a.getThoroughfare();   // nom de la rue
                if (rueTrouvee == null) rueTrouvee = a.getFeatureName();
                String ville = a.getLocality();
                return (rueTrouvee != null ? rueTrouvee : "")
                        + (ville != null ? ", " + ville : "");
            }
        } catch (Exception e) {
            // pas de réseau / pas de résultat : on renvoie une valeur par défaut
        }
        return getString(R.string.rue_inconnue);
    }

    // Méthodes imposées par l'interface LocationListener (non utilisées ici).
    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    // ----- PHOTO -----------------------------------------------------------

    private void prendrePhoto() {
        // 1) On prépare un fichier vide dans le dossier privé de l'application.
        File dossier = getExternalFilesDir(null);
        File fichier = new File(dossier, "photo_" + System.currentTimeMillis() + ".jpg");
        cheminPhoto = fichier.getAbsolutePath();

        // 2) On obtient une URI sécurisée via FileProvider (obligatoire Android 7+).
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fichiers", fichier);

        // 3) On lance l'appli "appareil photo" en lui indiquant où enregistrer.
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, CODE_PHOTO);
    }

    @Override
    protected void onActivityResult(int code, int resultat, @Nullable Intent data) {
        super.onActivityResult(code, resultat, data);
        if (code == CODE_PHOTO && resultat == RESULT_OK) {
            // La photo a été enregistrée dans cheminPhoto : on l'affiche en aperçu.
            apercu.setImageURI(Uri.fromFile(new File(cheminPhoto)));
        }
    }

    // ----- ENREGISTREMENT (création OU modification) -----------------------

    private void enregistrer() {
        if (champTitre.getText().toString().isEmpty()) {
            Toast.makeText(this, R.string.titre_obligatoire, Toast.LENGTH_SHORT).show();
            return;
        }

        Fiche f = new Fiche();
        f.id = ficheId;
        f.titre = champTitre.getText().toString();
        f.note = champNote.getText().toString();
        f.latitude = latitude;
        f.longitude = longitude;
        f.rue = rue;
        f.cheminPhoto = cheminPhoto;
        f.date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());
        f.synchro = 0; // pas encore (re)envoyée vers MySQL

        if (ficheId == -1) {
            db.ajouter(f);                  // nouvelle fiche
            Toast.makeText(this, R.string.fiche_enregistree, Toast.LENGTH_SHORT).show();
        } else {
            db.modifier(f);                 // fiche existante
            Toast.makeText(this, R.string.fiche_modifiee, Toast.LENGTH_SHORT).show();
        }
        finish(); // on revient à la liste
    }

    // ----- SUPPRESSION -----------------------------------------------------

    private void supprimer() {
        if (ficheId != -1) {
            db.supprimer(ficheId);
            Toast.makeText(this, R.string.fiche_supprimee, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
