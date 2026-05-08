package com.esprit.services;

import com.esprit.utils.MyDataBase;
import java.time.LocalDate;
import java.util.Map;

public class DashboardService {
    private final MyDataBase dataBase = MyDataBase.getInstance();

    public int getTotalUsers() {
        return dataBase.getTotalUsersCount();
    }

    public int getTodayLogins() {
        return dataBase.getTodayLoginsCount();
    }

    public int getEmailCount() {
        return dataBase.getEmailsSentTodayCount();
    }

    public int getActiveUsers() {
        return dataBase.getActiveUsersCount();
    }

    public Map<String, Integer> getRoleDistribution() {
        return dataBase.getRoleDistributionData();
    }

    public Map<LocalDate, Integer> getLoginActivityEvolution() {
        return dataBase.getLoginActivityData();
    }

    public Map<LocalDate, Integer> getDailyActivity() {
        return dataBase.getDailyActivityData();
    }

    public int getTotalEvents() {
        return dataBase.getTotalEventsCount();
    }

    public int getTotalRegistrations() {
        return dataBase.getTotalRegistrationsCount();
    }
}

