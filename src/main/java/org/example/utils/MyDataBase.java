package org.example.utils;

import java.sql.Connection;

/**
 * Proxy class to maintain compatibility with legacy code while using the 
 * modernized MyDataBase implementation in com.esprit.utils.
 */
public class MyDataBase {
    private static MyDataBase instance;

    private MyDataBase() {
        // Initialization is handled by com.esprit.utils.MyDataBase
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        return com.esprit.utils.MyDataBase.getInstance().getSharedConnection();
    }
}
