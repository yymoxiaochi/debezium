/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.testing.system.tools.databases;

import static io.debezium.testing.system.tools.WaitConditions.scaled;
import static org.awaitility.Awaitility.await;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jakub Cechacek
 */
public class SqlDatabaseClient implements DatabaseClient<Connection, SQLException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDatabaseClient.class);

    private final String url;
    private final String username;
    private final String password;

    public SqlDatabaseClient(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    private boolean doExecute(Commands<Connection, SQLException> commands) throws SQLException {
        try (Connection con = connect()) {
            commands.execute(con);
        }
        return true;
    }

    public void execute(Commands<Connection, SQLException> commands) throws SQLException {
        await()
                .atMost(scaled(2), TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> doExecute(commands));
    }

    public void execute(String database, Commands<Connection, SQLException> commands) throws SQLException {
        Commands<Connection, SQLException> withDatabase = con -> con.setCatalog(database);
        execute(con -> withDatabase.andThen(commands).execute(con));
    }

    public void execute(String database, String command) throws SQLException {
        LOGGER.info("Running SQL Command [" + database + "]: " + command);
        execute(database, con -> {
            try (Statement stmt = con.createStatement()) {
                stmt.execute(command);
            }
        });
    }

    public void execute(String command) throws SQLException {
        LOGGER.info("Running SQL Command: " + command);
        execute(con -> {
            try (Statement stmt = con.createStatement()) {
                stmt.execute(command);
            }
        });
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
