/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle.
 *
 * Dicoogle/dicoogle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */

package pt.ua.dicoogle.server.web.auth;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.ua.dicoogle.server.users.Role;
import pt.ua.dicoogle.server.users.User;

/**
 * Servlet filter to ensure that the user is logged in
 * (has a valid session token).
 *
 * <p>
 * Authentication tokens are created upon login.
 * A Dicoogle user can provide an authentication token
 * through one of these mechanisms:
 * </p>
 *
 * <ul>
 * <li><code>Authorization</code> header with the token as the value,
 * optionally prefixed with "Dicoogle " or "Bearer ";
 * <li>Or a cookie named <code>DICOOGLE_SESSION</code>
 * with the token as the value.
 * </ul>
 *
 * <p>
 * Before Dicoogle 3.6.0, only the <code>Authorization</code> header
 * with the token as the value was supported.
 * </p>
 */
public class AuthenticatedFilter implements Filter {

    /**
     * Name of the filter configuration parameter for
     * whether the user needs to be admin
     */
    public static final String NEEDS_ADMIN_PARAM = "needsAdmin";
    /**
     * Name of the filter configuration parameter for
     * whether the user needs to have a specific role
     */
    public static final String NEEDS_ROLE_PARAM = "needsRole";

    /** Name of the servlet request parameter
     * containing the authenticated user object,
     * if the request is successfully authenticated.
     */
    public static final String USER_ATTRIBUTE = "dicoogleUser";

    /**
     * The name of the Dicoogle session cookie,
     * where the authentication token can be stored.
     */
    public static final String DICOOGLE_SESSION_COOKIE_NAME = "DICOOGLE_SESSION";

    /** Whether the user needs to be an admin */
    private boolean needsAdmin = false;
    /** Whether the user needs to have this specific role (or be admin) */
    private Role needsRole = null;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        try {
            String needsAdminStr = fc.getInitParameter(NEEDS_ADMIN_PARAM);
            if (needsAdminStr != null && !needsAdminStr.isEmpty()) {
                needsAdmin = Boolean.parseBoolean(needsAdminStr);
            }
        } catch (IllegalArgumentException ex) {
            throw new ServletException("Invalid needsAdmin value for AuthenticatedFilter", ex);
        }

        try {
            String needsRoleStr = fc.getInitParameter(NEEDS_ROLE_PARAM);
            if (needsRoleStr != null && !needsRoleStr.isEmpty()) {
                needsRole = new Role(needsRoleStr);
            }
        } catch (IllegalArgumentException ex) {
            throw new ServletException("Invalid role for AuthenticatedFilter", ex);
        }
    }

    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain fc)
            throws IOException, ServletException {

        if (sreq instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) sreq;
            String token = getTokenFromRequest(req);

            // A Dicoogle session token must be found in order to continue
            if (token == null) {
                unauthorized(sresp);
                return;
            }

            // user has to exist
            User user = Authentication.getInstance().getUsername(token);
            if (user == null) {
                forbidden(sresp);
                return;
            }

            // user must be admin if required by this endpoint
            if (needsAdmin && !user.isAdmin()) {
                forbidden(sresp);
                return;
            }

            // if a role is required by this endpoint,
            // the user must either have that role or be admin
            if (!user.isAdmin() && needsRole != null && !user.hasRole(needsRole)) {
                forbidden(sresp);
                return;
            }

            // OK, inject user attribute and continue
            sreq.setAttribute(USER_ATTRIBUTE, user);
        }
        fc.doFilter(sreq, sresp);
    }

    /**
     * Helper function to retrieve the Dicoogle user authentication token.
     *
     * Note that servlets can call {@link Authentication#getAuthenticatedUser}
     * to retrieve the authenticated user.
     * This filter will ensure that the user object
     * is injected as an attribute in the servlet request in advance,
     * so that subsequent calls will not repeat the process.
     *
     * This method is only necessary if retrieving the token itself is important.
     * In such cases, prefer using this method over inspecting the request directly,
     * as it supports more ways in which
     * the token is sent by the client (see also #223).
     *
     * @param req the HTTP request to retrieve the token from
     * @return the token string, or null if no token is present
     */
    public static String getTokenFromRequest(HttpServletRequest req) {
        // prefer the Authorization header
        String token = req.getHeader("Authorization");
        if (token != null) {
            if (token.startsWith("Dicoogle ") || token.startsWith("dicoogle ")) {
                token = token.substring("dicoogle ".length());
            } else if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
                token = token.substring("bearer ".length());
            }
            if (!token.isEmpty()) {
                return token;
            }
        }

        // if not found, look for the DICOOGLE_SESSION cookie
        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if (DICOOGLE_SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    String cookieValue = cookie.getValue();
                    if (!cookieValue.isEmpty()) {
                        return cookieValue;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Helper function to retrieve the Dicoogle user authentication token.
     *
     * Prefer using this over inspecting the request directly,
     * as it supports more ways in which
     * the token is sent by the client (see also #223).
     *
     * @param req the HTTP request to retrieve the token from
     * @return the token string, or null if no token is present
     */
    public static String getTokenFromRequest(ServletRequest req) {
        if (req instanceof HttpServletRequest) {
            return getTokenFromRequest((HttpServletRequest) req);
        }
        return null;
    }

    private static void unauthorized(ServletResponse resp) throws IOException {
        if (resp instanceof HttpServletResponse) {
            HttpServletResponse httpResp = (HttpServletResponse) resp;
            httpResp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private static void forbidden(ServletResponse resp) throws IOException {
        if (resp instanceof HttpServletResponse) {
            HttpServletResponse httpResp = (HttpServletResponse) resp;
            httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    public void destroy() {
    }

}
