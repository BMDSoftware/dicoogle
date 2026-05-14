/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle-sdk.
 *
 * Dicoogle/dicoogle-sdk is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle-sdk is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.sdk.datastructs;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Simple descriptor for a Dicoogle user,
 * agnostic from authentication method and source.
 */
public final class DicoogleUser {
    private final String username;
    private final boolean admin;
    private final Set<String> roles;

    /** Constructs a Dicoogle user record from its parts. */
    public DicoogleUser(String username, boolean admin, Set<String> roles) {
        this.username = username;
        this.admin = admin;
        this.roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, admin, roles);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DicoogleUser other = (DicoogleUser) obj;
        return Objects.equals(username, other.username) && admin == other.admin && Objects.equals(roles, other.roles);
    }

    @Override
    public String toString() {
        return "DicoogleUser [username=" + username + ", admin=" + admin + ", roles=" + roles + "]";
    }
}
