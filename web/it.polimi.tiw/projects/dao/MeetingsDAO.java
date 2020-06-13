package it.polimi.tiw.projects.dao;

import java.sql.*;

import java.util.ArrayList;
import java.util.List;


import it.polimi.project5.beans.Meeting;


public class MeetingsDAO {

    private Connection connection;

    public MeetingsDAO(Connection connection) {
        this.connection = connection;
    }

    public int createMeeting(Integer creator, String title, String dateStart, String timeStart, Float duration,
                                 Integer maxParticipants) throws SQLException {

        String query1 = "";

        query1 = "INSERT INTO meeting (title, dateStart, creator, timeStart, duration, maxparticipants) VALUES (?, ?, ?, ?,?,?)";
        try (PreparedStatement pstatement = connection.prepareStatement(query1, Statement.RETURN_GENERATED_KEYS);) {
            pstatement.setString(1, title);
            pstatement.setString(2, dateStart);
            pstatement.setInt(3, creator);
            pstatement.setString(4, timeStart);
            pstatement.setFloat(5, duration);
            pstatement.setInt(6, maxParticipants);
            pstatement.executeUpdate();
            ResultSet generatedKeys = pstatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return(generatedKeys.getInt(1));

            } else throw new SQLException("Creation meeting reference failed");
        }


    }

    public void addParticipants(int meetingKey,  List<Integer> participantsID) throws SQLException{
         String query = "";
        query = "INSERT INTO participant (meeting, userParticipant) VALUES (?,?)";
        for(Integer participant : participantsID) {
            try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {
                pstatement.setInt(1, meetingKey);
                pstatement.setInt(2, participant);
                pstatement.executeUpdate();
                ResultSet generatedKeys = pstatement.getGeneratedKeys();
                if(!generatedKeys.next()) throw new SQLException("Creation participant reference failed");
            }

        }

    }

    public List<Meeting> findMeetingsByUser(int userId) throws SQLException {
        List<Meeting> meetings = new ArrayList<Meeting>();

        String query = "SELECT * FROM meeting where creator = ? ORDER BY dateStart DESC";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, userId);
            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    Meeting meeting = new Meeting();
                    meeting.setId(result.getInt("id_meeting"));
                    meeting.setTitle(result.getString("title"));
                    meeting.setStartDate(result.getDate("dateStart"));
                    meeting.setCreator(result.getInt("creator"));
                    meeting.setTime(result.getTime("timeStart"));
                    meeting.setDuration(result.getInt("duration"));
                    meeting.setMaxParticipants(result.getInt("maxParticipants"));
                    meetings.add(meeting);
                }
            }
        }
        return meetings;
    }

    public List<Meeting> findUserInvitedMeetingsByUser(int userId) throws SQLException {
        List<Meeting> meetings = new ArrayList<Meeting>();
        String query = "select id_meeting, title, dateStart, creator, timeStart, duration  from (meeting join participant join userlist) where userlist.id = ? and meeting.id_meeting=participant.meeting and userlist.id = participant.userParticipant";
        try (PreparedStatement pstatement = connection.prepareStatement(query);) {
            pstatement.setInt(1, userId);
            try (ResultSet result = pstatement.executeQuery();) {
                while (result.next()) {
                    Meeting meeting = new Meeting();
                    meeting.setId(result.getInt("id_meeting"));
                    meeting.setTitle(result.getString("title"));
                    meeting.setStartDate(result.getDate("dateStart"));
                    meeting.setCreator(result.getInt("creator"));
                    meeting.setTime(result.getTime("timeStart"));
                    meeting.setDuration(result.getInt("duration"));
                    meetings.add(meeting);
                }
            }
        }
        return meetings;
    }
}
