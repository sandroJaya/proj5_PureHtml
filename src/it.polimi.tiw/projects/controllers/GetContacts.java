package it.polimi.tiw.projects.controllers;


import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/GetContacts")
public class GetContacts extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public GetContacts() {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        HttpSession session = request.getSession();
        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        if (session.isNew() || session.getAttribute("user") == null) {
            String loginpath = getServletContext().getContextPath() + "/index.html";
            response.sendRedirect(loginpath);
            return;
        }

        if (session.getAttribute("toDeselect") != null) {
            ctx.setVariable("errorField", "Too many users selected, delete at least " +
                    session.getAttribute("toDeselect"));

        } else {

            if ((Integer) session.getAttribute("tries") == 0) {
                ctx.setVariable("errorField", "");
            }
            Cookie cookies[] = new Cookie[5];
            String title = (request.getParameter("title")).replaceAll("\\s+","_");
            cookies[0] = new Cookie("title", title);
            cookies[1] = new Cookie("date", request.getParameter("date"));
            cookies[2] = new Cookie("time", request.getParameter("time"));
            cookies[3] = new Cookie("duration", request.getParameter("duration"));
            cookies[4] = new Cookie("maxparticipants", request.getParameter("maxparticipants"));

            for (int i = 0; i < cookies.length; i++) {
                cookies[i].setMaxAge(3600);
                response.addCookie(cookies[i]);
            }
        }


        User user = (User) session.getAttribute("user");
        UserDAO userDAO = new UserDAO(connection);
        List<User> users;

        try {
            users = userDAO.findUser(user.getId());
            if (users == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
                return;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "It was not possible to recover contacts");
            return;
        }


        ArrayList<Integer> IDs = (ArrayList<Integer>) session.getAttribute("participantsIDs");
        if (IDs != null) {
            for (int i : IDs) {
                for (User u : users) {
                    if (u.getId() == i)
                        u.setInvited(true);
                }
            }
        }

        // Redirect to the Home page and add missions to the parameters
        String path = "/WEB-INF/Contacts.html";

        ctx.setVariable("contacts", users);
        templateEngine.process(path, ctx, response.getWriter());
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
