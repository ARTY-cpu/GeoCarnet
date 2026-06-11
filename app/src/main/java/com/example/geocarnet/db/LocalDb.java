package com.example.geocarnet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.geocarnet.model.Fiche;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de données LOCALE.
 * On l'écrit "à la main" avec SQLiteOpenHelper (PAS de bibliothèque Room)
 */
public class LocalDb extends SQLiteOpenHelper {

    private static final String NOM_BASE = "geocarnet.db";
    private static final int VERSION = 1;

    // On regroupe les noms de table/colonnes ici pour éviter les fautes de frappe.
    public static final String TABLE = "fiches";
    public static final String COL_ID = "id";
    public static final String COL_TITRE = "titre";
    public static final String COL_NOTE = "note";
    public static final String COL_LAT = "latitude";
    public static final String COL_LON = "longitude";
    public static final String COL_RUE = "rue";
    public static final String COL_PHOTO = "photo";
    public static final String COL_DATE = "date";
    public static final String COL_SYNC = "synchro";

    public LocalDb(Context context) {
        super(context, NOM_BASE, null, VERSION);
    }

    // Appelée UNE seule fois, à la première utilisation : on crée la table.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITRE + " TEXT, " +
                COL_NOTE + " TEXT, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_RUE + " TEXT, " +
                COL_PHOTO + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_SYNC + " INTEGER DEFAULT 0)";
        db.execSQL(sql);
    }

    // Appelée si on change le numéro de VERSION : ici on recrée simplement la table.
    @Override
    public void onUpgrade(SQLiteDatabase db, int ancienne, int nouvelle) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    /** Insère une nouvelle fiche et renvoie son identifiant. */
    public long ajouter(Fiche f) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_TITRE, f.titre);
        v.put(COL_NOTE, f.note);
        v.put(COL_LAT, f.latitude);
        v.put(COL_LON, f.longitude);
        v.put(COL_RUE, f.rue);
        v.put(COL_PHOTO, f.cheminPhoto);
        v.put(COL_DATE, f.date);
        v.put(COL_SYNC, f.synchro);
        long id = db.insert(TABLE, null, v);
        db.close();
        return id;
    }

    /**
     * Renvoie les fiches.
     * Si "filtre" est vide -> toutes les fiches.
     * Sinon -> seulement celles dont le titre contient "filtre" (recherche).
     */
    public List<Fiche> lire(String filtre) {
        List<Fiche> liste = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c;
        if (filtre == null || filtre.isEmpty()) {
            c = db.query(TABLE, null, null, null, null, null, COL_ID + " DESC");
        } else {
            c = db.query(TABLE, null, COL_TITRE + " LIKE ?",
                    new String[]{"%" + filtre + "%"}, null, null, COL_ID + " DESC");
        }
        while (c.moveToNext()) {
            liste.add(curseurVersFiche(c));
        }
        c.close();
        db.close();
        return liste;
    }

    /** Renvoie uniquement les fiches PAS encore envoyées vers MySQL. */
    public List<Fiche> nonSynchronisees() {
        List<Fiche> liste = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, COL_SYNC + " = 0", null, null, null, null);
        while (c.moveToNext()) {
            liste.add(curseurVersFiche(c));
        }
        c.close();
        db.close();
        return liste;
    }

    /** Marque une fiche comme synchronisée (synchro = 1). */
    public void marquerSynchronisee(long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_SYNC, 1);
        db.update(TABLE, v, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    /** Supprime une fiche. */
    public void supprimer(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    /** Renvoie UNE fiche à partir de son identifiant (ou null si introuvable). */
    public Fiche lireUne(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        Fiche f = null;
        if (c.moveToFirst()) {
            f = curseurVersFiche(c);
        }
        c.close();
        db.close();
        return f;
    }

    /** Met à jour une fiche existante (identifiée par f.id). */
    public void modifier(Fiche f) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_TITRE, f.titre);
        v.put(COL_NOTE, f.note);
        v.put(COL_LAT, f.latitude);
        v.put(COL_LON, f.longitude);
        v.put(COL_RUE, f.rue);
        v.put(COL_PHOTO, f.cheminPhoto);
        v.put(COL_DATE, f.date);
        v.put(COL_SYNC, 0); // modifiée -> à renvoyer vers MySQL
        db.update(TABLE, v, COL_ID + " = ?", new String[]{String.valueOf(f.id)});
        db.close();
    }

    // Petit utilitaire : transforme la ligne courante du curseur en objet Fiche.
    private Fiche curseurVersFiche(Cursor c) {
        Fiche f = new Fiche();
        f.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
        f.titre = c.getString(c.getColumnIndexOrThrow(COL_TITRE));
        f.note = c.getString(c.getColumnIndexOrThrow(COL_NOTE));
        f.latitude = c.getDouble(c.getColumnIndexOrThrow(COL_LAT));
        f.longitude = c.getDouble(c.getColumnIndexOrThrow(COL_LON));
        f.rue = c.getString(c.getColumnIndexOrThrow(COL_RUE));
        f.cheminPhoto = c.getString(c.getColumnIndexOrThrow(COL_PHOTO));
        f.date = c.getString(c.getColumnIndexOrThrow(COL_DATE));
        f.synchro = c.getInt(c.getColumnIndexOrThrow(COL_SYNC));
        return f;
    }
}
