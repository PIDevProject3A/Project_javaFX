package org.example.services;

import java.sql.SQLException;
import java.util.List;

public interface Icrud<T> {

    // Change le type de retour de void à int
    int ajouter(T t) throws SQLException;  // ← Retourne l'ID généré

    List<T> afficher() throws SQLException;

    void modifier(T t) throws SQLException;

    void supprimer(int id) throws SQLException;
}