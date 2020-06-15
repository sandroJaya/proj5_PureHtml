package it.polimi.tiw.projects.controllers;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/Register")
public class Register extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public Register() {
        super();
    }

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
        ServletContext servletContext = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        String path;
        // obtain and escape params
        String usrn = null;
        String pwd = null;
        String repwd = null;
        String name = null;
        String surname = null;

        try {
            usrn = StringEscapeUtils.escapeJava(request.getParameter("username"));
            pwd = StringEscapeUtils.escapeJava(request.getParameter("pwd"));
            repwd = StringEscapeUtils.escapeJava(request.getParameter("repwd"));
            name = StringEscapeUtils.escapeJava(request.getParameter("name"));
            surname = StringEscapeUtils.escapeJava(request.getParameter("surname"));
        } catch (NullPointerException e) {
            // for debugging only e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing param values");
            return;
        }


        if (!pwd.equals(repwd)) {
            System.out.println(pwd);
            System.out.println(repwd);
            ctx.setVariable("errorMsg", "Passwords do not match");
            path = "/register.html";
            templateEngine.process(path, ctx, response.getWriter());
            System.out.println("BBBBBBBBBBBBB");
            return;
        }

        // query db to authenticate for user
        UserDAO userDao = new UserDAO(connection);
        Boolean user = null;
        try {
            user = userDao.registration(usrn, pwd, name, surname);
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not Possible to check credentials");
            return;
        }

        // If the user exists, add info to the session and go to home page, otherwise
        // show login page with error message

        if (!user) {
            ctx.setVariable("errorMsg", "Account already exists");
            path = "/register.html";
            templateEngine.process(path, ctx, response.getWriter());
            return;
        }
        else {
            ctx.setVariable("errorMsg", "");
            path = getServletContext().getContextPath() + "/index.html";
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
