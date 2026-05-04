package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    final String USERNAME ="root";
    final String URL ="jdbc:mysql://localhost:3306/bledna";
    final String PASSWORD ="";
    Connection connection ;
    static MyDataBase instance;
    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to the database successfully!");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to connect to MySQL. Verify server is running and DB 'bledna' exists at " + URL,
                    e
            );
        }

    }

    public static MyDataBase getInstance() {
        if(instance==null){
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is not initialized.");
        }
        return connection;
    }
}
