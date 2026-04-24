package com.esprit;

import com.esprit.entities.Event;
import com.esprit.Services.EventService;
import com.esprit.utils.MyDataBase;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        // 🔌 Connexion
        MyDataBase db = MyDataBase.getInstance();

        if (db.getConnection() != null) {
            System.out.println("✅ Connexion OK");
        } else {
            System.out.println("❌ Connexion échouée");
            return;
        }

        EventService es = new EventService();

        try {
            // 🎯 Création event
            Event e = new Event();
            e.setName("Beach Cleanup");
            e.setDescription("Collecte des déchets à la plage");
            e.setEventDate(LocalDateTime.now());
            e.setLocation("La Marsa");
            e.setPrice(0);
            e.setPaymentType("CASH");
            e.setEventType("FREE");
            e.setMaxPlaces(100);

            // ➕ Ajouter
            es.ajouter(e);

            System.out.println("🎉 Event ajouté !");

        } catch (SQLException ex) {
            System.out.println("❌ Erreur: " + ex.getMessage());
        }
    }
}