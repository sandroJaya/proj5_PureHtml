package it.polimi.tiw.projects.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.projects.beans.Meeting;
import it.polimi.tiw.projects.beans.User;

public class UserDAO {
    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    public User checkCredentials(String usrn, String pwd) throws SQLException { // dice se esiste un determinato account nel db. se c'� ritorna l'account, se no null
        String query = "SELECT  id, username, name, surname FROM userlist  WHERE username = ? AND password =?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setString(1, usrn);
            pstatement.setString(2, pwd);
            try (ResultSet result = pstatement.executeQuery();) {
                if (!result.isBeforeFirst()) // no results, credential check failed
                    return null;
                else {
                    result.next();
                    User user = new User();
                    user.setId(result.getInt("id"));
                    user.setUsername(result.getString("username"));
                    user.setName(result.getString("name"));
                    user.setSurname(result.getString("surname"));
                    return user;
                }
            }
        }
    }

    public boolean registration(String username, String password, String name, String surname) throws SQLException {
        String query2 = "INSERT INTO userlist (username, password, name, surname) VALUES (?,?,?,?)";
        String query="SELECT username FROM userlist where username = ?";

        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1,username);

            try (ResultSet result = pstatement.executeQuery();) {
                if (!result.isBeforeFirst()) { // no results, l'utente non esiste gia.

                    try (PreparedStatement pstatement2 = connection.prepareStatement(query2);) {
                        pstatement2.setString(1, username);
                        pstatement2.setString(2, password);
                        pstatement2.setString(3, name);
                        pstatement2.setString(4, surname);
                        pstatement2.executeUpdate();
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public List<User> findUser(int userId) throws SQLException {
        List<User> users = new ArrayList<>();

        String query = "SELECT * FROM userlist WHERE userlist.id != ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, userId);
            try (ResultSet result = pstatement.executeQuery();) {
                while (result.next()) {
                    User user = new User();
                    user.setId(result.getInt("id"));
                    user.setName(result.getString("name"));
                    user.setSurname(result.getString("surname"));
                    users.add(user);
                }
            }
        }
        return users;
    }
}