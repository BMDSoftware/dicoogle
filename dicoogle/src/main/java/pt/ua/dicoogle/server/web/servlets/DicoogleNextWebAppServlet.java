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

package pt.ua.dicoogle.server.web.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * Servlet to serve the experimental dicoogle-next webapp
 */
public class DicoogleNextWebAppServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getPathInfo();

        // Default to index.html for root and client-side routing
        if (path == null || path.equals("/")) {
            path = "/index.html";
        }

        // Try to serve from /dicoogle-next/webapp/dist/ first
        String resourcePath = "/dicoogle-next/webapp/dist" + path;
        InputStream is = getClass().getResourceAsStream(resourcePath);

        // If it's a file request (has extension) and not found, it's a 404
        // If it's a route (no extension), fallback to index.html for SPA routing
        if (is == null) {
            if (path.contains(".")) {
                // File not found
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else {
                // Fallback to index.html for client-side routing
                resourcePath = "/dicoogle-next/webapp/dist/index.html";
                is = getClass().getResourceAsStream(resourcePath);
            }
        }

        if (is == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Set content type
        String contentType = getServletContext().getMimeType(path);
        if (contentType == null) {
            if (path.endsWith(".js")) {
                contentType = "text/javascript";
            } else if (path.endsWith(".css")) {
                contentType = "text/css";
            } else if (path.endsWith(".html")) {
                contentType = "text/html";
            } else if (path.endsWith(".json")) {
                contentType = "application/json";
            } else if (path.endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (path.endsWith(".ico")) {
                contentType = "image/x-icon";
            } else if (path.endsWith(".woff") || path.endsWith(".woff2")) {
                contentType = "font/woff2";
            }
        }

        if (contentType != null) {
            resp.setContentType(contentType);
        }

        // Cache assets
        if (path.contains("/assets/") || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".svg")
                || path.endsWith(".ico")) {
            resp.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        }

        // Stream response
        try (InputStream in = is; OutputStream out = resp.getOutputStream()) {
            IOUtils.copy(in, out);
        }
    }
}
