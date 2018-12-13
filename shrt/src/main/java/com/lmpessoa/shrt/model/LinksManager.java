/*
 * Copyright (c) 2018 Leonardo Pessoa
 * https://github.com/lmpessoa/java-services
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lmpessoa.shrt.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;

import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.security.IdentityBuilder;
import com.lmpessoa.services.services.HealthStatus;
import com.lmpessoa.shrt.model.LinkInfo.Visitor;

public final class LinksManager implements ILinksManager {

   private static final DateTimeFormatter sqliteFormat = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

   private final ILogger log;

   private Connection connection;

   public LinksManager(ILogger log) {
      this.log = log;
   }

   @Override
   public HealthStatus getHealth() {
      Connection conn = getConnection();
      try {
         return conn == null || conn.isValid(0) ? HealthStatus.OK : HealthStatus.FAILED;
      } catch (SQLException e) {
         return HealthStatus.FAILED;
      }
   }

   @Override
   public String expand(String slug) {
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("SELECT * FROM links WHERE slug = ?")) {
         stmt.setString(1, slug);
         try (ResultSet result = stmt.executeQuery()) {
            if (result.next()) {
               return result.getString("url");
            }
         }
      } catch (SQLException e) {
         throw new DataManagerException(e);
      }
      return null;
   }

   @Override
   public String shorten(String url) {
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("SELECT * FROM links WHERE url = ?")) {
         stmt.setString(1, Objects.requireNonNull(url).toString());
         try (ResultSet result = stmt.executeQuery()) {
            if (result.next()) {
               return result.getString("slug");
            }
         }
      } catch (SQLException e) {
         throw new DataManagerException(e);
      }
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("INSERT INTO links VALUES(?, ?, CURRENT_TIMESTAMP)")) {
         stmt.setString(2, url.toString());
         int slugSize = computeSlugSize();
         while (true) {
            String slug = createRandomSlug(slugSize);
            stmt.setString(1, slug);
            if (stmt.executeUpdate() != 0) {
               return slug;
            }
         }
      } catch (SQLException e) {
         throw new DataManagerException(e);
      }
   }

   @Override
   public String shorten(String url, String slug) {
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("SELECT * FROM links WHERE url = ?")) {
         stmt.setString(1, Objects.requireNonNull(url).toString());
         try (ResultSet result = stmt.executeQuery()) {
            if (result.next()) {
               return result.getString("slug");
            }
         }
      } catch (SQLException e) {
         throw new DataManagerException(e);
      }
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("INSERT INTO links VALUES(?, ?, CURRENT_TIMESTAMP)")) {
         stmt.setString(1, slug);
         stmt.setString(2, url.toString());
         if (stmt.executeUpdate() != 0) {
            return slug;
         }
      } catch (SQLException e) {
         throw new DataManagerException(e);
      }
      return null;
   }

   @Override
   public boolean rename(String oldSlug, String newSlug) {
      if (expand(oldSlug) == null || expand(newSlug) != null) {
         return false;
      }
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("UPDATE links SET slug = ? WHERE slug = ?")) {
         stmt.setString(1, newSlug);
         stmt.setString(2, oldSlug);
         return stmt.executeUpdate() == 1;
      } catch (SQLException e) {
         throw new DataManagerException(e);
      }
   }

   @Override
   public boolean delete(String slug) {
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("DELETE FROM links WHERE slug = ?")) {
         stmt.setString(1, slug);
         return stmt.executeUpdate() == 1;
      } catch (SQLException e) {
         throw new DataManagerException(e);
      }
   }

   @Override
   public void log(String slug, String ipAddress) {
      new Thread(() -> {
         String countryCode = "XX";
         try (InputStream is = new URL(
                  String.format("http://ip-api.com/json/%s?fields=1", ipAddress)).openStream()) {
            if (is != null) {
               BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
               String result = in.readLine();
               if (result.startsWith("{\"country\":\"")) {
                  countryCode = result.substring(12, result.length() - 2);
               }
            }
         } catch (IOException e) {
            log.error(e);
         }

         try (PreparedStatement stmt = getConnection()
                  .prepareStatement("INSERT INTO log VALUES(?, ?, ?, CURRENT_TIMESTAMP)")) {
            stmt.setString(1, slug);
            stmt.setString(2, ipAddress);
            stmt.setString(3, countryCode);
            stmt.executeUpdate();
         } catch (SQLException e) {
            log.debug(e);
         }
      }).start();
   }

   @Override
   public LinkInfo info(String link) throws SQLException {
      LocalDateTime created = null;
      String original = null;
      String creator = null;
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("SELECT L.created, C.name, U.original\n"
                        + "FROM links L, url U, users C WHERE L.link = ? AND L.url = U.id AND L.creator = C.id")) {
         stmt.setString(1, link);
         try (ResultSet result = stmt.executeQuery()) {
            if (result.next()) {
               created = sqliteFormat.parse(result.getString("created"), LocalDateTime::from);
               original = result.getString("original");
               creator = result.getString("name");
            } else {
               return null;
            }
         }
      }
      Collection<Visitor> visitors = new ArrayList<>();
      try (PreparedStatement stmt = getConnection().prepareStatement("SELECT V.country, V.created "
               + "FROM visitors V, links L WHERE L.link = ? AND V.link = L.id")) {
         stmt.setString(1, link);
         try (ResultSet result = stmt.executeQuery()) {
            while (result.next()) {
               LocalDateTime date = sqliteFormat.parse(result.getString("created"),
                        LocalDateTime::from);
               visitors.add(new Visitor(date, result.getString("country")));
            }
         }
      }
      return new LinkInfo(link, original, creator, created, visitors);
   }

   @Override
   public boolean isValidUser(String user, String password) {
      try (PreparedStatement stmt = getConnection().prepareStatement(
               "SELECT count(*) AS count FROM users WHERE login = ? AND password = ?")) {
         stmt.setString(1, user);
         stmt.setString(2, password);
         try (ResultSet result = stmt.executeQuery()) {
            return result.next() && result.getInt("count") == 1;
         }
      } catch (SQLException e) {
         log.error(e);
      }
      return false;
   }

   @Override
   public IIdentity getUserOfToken(String token) {
      try (PreparedStatement stmt = getConnection().prepareStatement(
               "SELECT u.* FROM users u, tokens t WHERE t.id = ? AND t.user = u.id")) {
         stmt.setString(1, token);
         try (ResultSet result = stmt.executeQuery()) {
            if (result.next()) {
               IdentityBuilder builder = new IdentityBuilder();
               builder.addAccountName(result.getString("login"));
               builder.addDisplayName(result.getString("name"));
               return builder.build();
            }
         }
      } catch (SQLException e) {
         log.error(e);
      }
      return null;
   }

   @Override
   public void setTokenForUser(String user, String token, Duration expires) {
      int userId = -1;
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("SELECT id FROM users WHERE login = ?")) {
         stmt.setString(1, user);
         try (ResultSet result = stmt.executeQuery()) {
            if (!result.next()) {
               return;
            }
            userId = result.getInt("id");
         }
      } catch (SQLException e) {
         log.error(e);
         return;
      }
      Timestamp expiresAt = null;
      if (expires != null) {
         expiresAt = Timestamp.from(Instant.now().plus(expires));
      }
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("INSERT INTO tokens VALUES(?, ?, ?)")) {
         stmt.setString(1, token);
         stmt.setInt(2, userId);
         if (expiresAt == null) {
            stmt.setNull(3, JDBCType.TIMESTAMP.ordinal());
         } else {
            stmt.setTimestamp(3, expiresAt);
         }
         stmt.executeUpdate();
      } catch (SQLException e) {
         log.error(e);
      }
   }

   @Override
   public void removeToken(String token) {
      try (PreparedStatement stmt = getConnection()
               .prepareStatement("DELETE FROM tokens WHERE id = ?")) {
         stmt.setString(1, token);
         stmt.executeUpdate();
      } catch (SQLException e) {
         log.error(e);
      }
   }

   private int computeSlugSize() throws SQLException {
      ResultSet result = getConnection().createStatement()
               .executeQuery("SELECT LENGTH(slug) AS length, COUNT(slug) AS count FROM links "
                        + "GROUP BY LENGTH(slug) ORDER BY length ASC");
      while (result.next()) {
         int length = result.getInt("length");
         long count = result.getLong("count");
         long max = (long) Math.pow(36, length);
         if (max > count) {
            return length;
         }
      }
      return 3;
   }

   private String createRandomSlug(int length) {
      String result = "";
      Random rand = new Random();
      while (result.length() < length) {
         result += "zaqxswcdevfrbgtnhymjukilop".charAt(rand.nextInt(26));
      }
      return result;
   }

   private Connection getConnection() {
      try {
         if (connection == null || !connection.isValid(0)) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (SQLException e) {
                  log.debug(e);
               }
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:shrt.sqlite3");
            createTables(connection);
         }
         return connection;
      } catch (SQLException | ClassNotFoundException e) {
         throw new DataManagerException(e);
      }
   }

   private void createTables(Connection connection) throws SQLException {
      Statement stmt = connection.createStatement();
      try (ResultSet result = stmt.executeQuery("PRAGMA table_info(\"links\")")) {
         if (!result.next()) {
            stmt.executeUpdate("PRAGMA application_id = 1");
            stmt.executeUpdate("PRAGMA encoding = 'UTF-8';");
            stmt.executeUpdate("PRAGMA foreign_keys = 1");
            stmt.executeUpdate("PRAGMA synchronous = 'normal'");

            stmt.executeUpdate("CREATE TABLE \"users\" (" //
                     + "\"id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE," //
                     + "\"login\" VARCHAR(20) UNIQUE NOT NULL," //
                     + "\"name\" VARCHAR(100) NOT NULL," //
                     + "\"password\" VARCHAR(60) NOT NULL" //
                     + ")");

            stmt.executeUpdate("CREATE TABLE \"tokens\" (" //
                     + "\"id\" VARCHAR(60) PRIMARY KEY NOT NULL UNIQUE," //
                     + "\"user\" INTEGER UNIQUE NOT NULL REFERENCES \"users\"(\"id\") ON UPDATE CASCADE ON DELETE CASCADE," //
                     + "\"expires\" TIMESTAMP" //
                     + ")");

            stmt.executeUpdate("CREATE TABLE \"url\" (" //
                     + "\"id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,"
                     + "\"original\" VARCHAR(500) NOT NULL" //
                     + ")");

            stmt.executeUpdate("CREATE TABLE \"links\" (" //
                     + "\"id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE," //
                     + "\"link\" CHAR(9) NOT NULL UNIQUE," //
                     + "\"url\" INTEGER REFERENCES \"url\"(\"id\") ON UPDATE CASCADE ON DELETE CASCADE," //
                     + "\"created\" TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP," //
                     + "\"creator\" INTEGER NOT NULL REFERENCES \"users\"(\"id\") ON UPDATE CASCADE ON DELETE CASCADE" //
                     + ")");

            stmt.executeUpdate("CREATE TABLE \"visitors\" (" //
                     + "\"link\" INTEGER NOT NULL REFERENCES \"links\"(\"id\") ON UPDATE CASCADE ON DELETE CASCADE," //
                     + "\"created\" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," //
                     + "\"ipaddr\" VARCHAR(40) NOT NULL," //
                     + "\"country\" VARCHAR(30) NOT NULL," //
                     + ")");
         }
      }
   }
}
