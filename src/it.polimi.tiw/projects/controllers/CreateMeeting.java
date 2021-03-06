package it.polimi.tiw.projects.controllers;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.MeetingsDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

@WebServlet("/CreateMeeting")
public class CreateMeeting extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private Connection connection = null;
    private TemplateEngine templateEngine;

    private Date getMeYesterday() {
        return new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    }

    public CreateMeeting() {
        super();
    }

    public void init() throws ServletException {

        ServletContext servletContext = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
        connection = ConnectionHandler.getConnection(getServletContext());
    }


    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        // If the user is not logged in (not present in session) redirect to the login
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") == null) {
            String loginpath = getServletContext().getContextPath() + "/index.html";
            response.sendRedirect(loginpath);
            return;

        }

        // Get and parse all parameters from request
        boolean isBadRequest = false;
        String title = null;
        Date dateStart = null;
        String timeStart = null;
        Float duration = null;
        Integer maxParticipants = null;
        ArrayList<Integer> participantsID = new ArrayList<Integer>();
        User user = (User) session.getAttribute("user");
        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());


        try {
            Cookie cookies[] = request.getCookies();
            for (Cookie c : cookies) {
                if (c.getName().equals("title")) {
                    title = c.getValue();
                } else if (c.getName().equals("date")) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    dateStart = (Date) sdf.parse(c.getValue());
                } else if (c.getName().equals("time")) {
                    timeStart = c.getValue();
                } else if (c.getName().equals("duration")) {
                    duration = Float.parseFloat(c.getValue());
                } else if (c.getName().equals("maxparticipants")) {
                    maxParticipants = Integer.parseInt(c.getValue());
                }
            }
            isBadRequest = duration <= 0 || title.isEmpty() || timeStart.isEmpty()
                    || getMeYesterday().after(dateStart) || maxParticipants <= 0;


        } catch (NumberFormatException | NullPointerException | ParseException e) {
            isBadRequest = true;
            e.printStackTrace();
        }


        if (isBadRequest) {
            //String path = "/WEB-INF/CreationFailed.html";
            //templateEngine.process(path, ctx, response.getWriter());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect or missing param values");
            return;
        }


        try {
            if (request.getParameterValues("invited") != null) {
                for (String s : request.getParameterValues("invited")) {
                    participantsID.add(Integer.parseInt(s));

                }
            }
            participantsID.add(user.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String ctxpath = getServletContext().getContextPath();

        int tries = (Integer) session.getAttribute("tries");


        if (participantsID.size() > maxParticipants) {

            if (tries == 2) {
                String path = "/WEB-INF/CreationFailed.html";
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }

            tries++;
            session.setAttribute("toDeselect", participantsID.size() - maxParticipants);
            String deselect = ctxpath + "/GetContacts";
            session.setAttribute("tries", tries);
            session.setAttribute("participantsIDs", participantsID);
            response.sendRedirect(deselect);
            return;


        } else {
            // Create mission in DB

            MeetingsDAO meetingsDAO = new MeetingsDAO(connection);
            int meetingKey;
            try {
                meetingKey = meetingsDAO.createMeeting(user.getId(), title, dateStart, timeStart, duration, maxParticipants);
                meetingsDAO.addParticipants(meetingKey, participantsID);
            } catch (SQLException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to create meeting");
                return;
            }


            // return the user to the right view
            String path = ctxpath + "/Home";
            response.sendRedirect(path);
        }
    }

    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
