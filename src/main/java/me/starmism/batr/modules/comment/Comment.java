package me.starmism.batr.modules.comment;

import com.google.common.collect.Lists;
import me.mattstudios.config.SettingsManager;
import me.starmism.batr.BATR;
import me.starmism.batr.database.DataSourceHandler;
import me.starmism.batr.database.SQLQueries;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.BATCommand;
import me.starmism.batr.modules.IModule;
import me.starmism.batr.modules.comment.CommentEntry.Type;
import me.starmism.batr.modules.core.Core;
import me.starmism.batr.utils.UtilsKt;

import java.nio.file.Path;
import java.sql.*;
import java.util.List;

public class Comment implements IModule {
    private final SettingsManager config;
    private CommentCommand commandHandler;
    private final I18n i18n;

    public Comment() {
        config = SettingsManager.from(Path.of(BATR.getInstance().getDataFolder().getPath(), "comment.yml"))
                .configurationData(CommentConfig.class).create();
        i18n = BATR.getInstance().getI18n();
    }

    @Override
    public List<BATCommand> getCommands() {
        return commandHandler.getCmds();
    }

    @Override
    public String getName() {
        return "comment";
    }

    @Override
    public String getMainCommand() {
        return "comment";
    }

    @Override
    public SettingsManager getConfig() {
        return config;
    }

    @Override
    public boolean isEnabled() {
        return config.get(CommentConfig.ENABLED);
    }

    @Override
    public boolean load() {
        // Init table
        Statement statement = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.createStatement();
            if (DataSourceHandler.isSQLite()) {
                for (final String commentsQuery : SQLQueries.Comments.SQLite.createTable) {
                    statement.executeUpdate(commentsQuery);
                }
            } else {
                statement.executeUpdate(SQLQueries.Comments.createTable);
            }
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

        // Register commands
        commandHandler = new CommentCommand(this);
        commandHandler.loadCmds();

        return true;
    }

    @Override
    public boolean unload() {
        return true;
    }

    /**
     * Get the notes relative to an entity
     *
     * @param entity | can be an ip or a player name
     * @return The notes
     */
    public List<CommentEntry> getComments(final String entity) {
        List<CommentEntry> notes = Lists.newArrayList();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(DataSourceHandler.isSQLite()
                    ? SQLQueries.Comments.SQLite.getEntries
                    : SQLQueries.Comments.getEntries);
            if (UtilsKt.validIP(entity)) {
                statement.setString(1, entity);
            } else {
                statement.setString(1, Core.getUUID(entity));
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                final long date;
                if (DataSourceHandler.isSQLite()) {
                    date = resultSet.getLong("strftime('%s',date)") * 1000;
                } else {
                    date = resultSet.getTimestamp("date").getTime();
                }
                notes.add(new CommentEntry(resultSet.getInt("id"), entity, resultSet.getString("note"),
                        resultSet.getString("staff"), CommentEntry.Type.valueOf(resultSet.getString("type")),
                        date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return notes;
    }

    public List<CommentEntry> getManagedComments(final String staff) {
        List<CommentEntry> notes = Lists.newArrayList();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(DataSourceHandler.isSQLite()
                    ? SQLQueries.Comments.SQLite.getManagedEntries
                    : SQLQueries.Comments.getManagedEntries);
            statement.setString(1, staff);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final long date;
                if (DataSourceHandler.isSQLite()) {
                    date = resultSet.getLong("strftime('%s',date)") * 1000;
                } else {
                    date = resultSet.getTimestamp("date").getTime();
                }
                String entity = Core.getPlayerName(resultSet.getString("entity"));
                if (entity == null) {
                    entity = "UUID:" + resultSet.getString("entity");
                }
                notes.add(new CommentEntry(resultSet.getInt("id"), entity, resultSet.getString("note"),
                        staff, CommentEntry.Type.valueOf(resultSet.getString("type")),
                        date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return notes;
    }

    public void insertComment(final String entity, final String comment, final Type type, final String author) {
        PreparedStatement statement = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Comments.insertEntry);
            statement.setString(1, (UtilsKt.validIP(entity)) ? entity : Core.getUUID(entity));
            statement.setString(2, comment);
            statement.setString(3, type.name());
            statement.setString(4, author);
            statement.executeUpdate();
            statement.close();

            // Handle the trigger system
            for (final Trigger trigger : config.get(CommentConfig.TRIGGERS).values()) {
                for (final String pattern : trigger.getPattern()) {
                    if (pattern.isEmpty() || comment.contains(pattern)) {
                        statement = conn.prepareStatement((pattern.isEmpty())
                                ? SQLQueries.Comments.simpleTriggerCheck
                                : SQLQueries.Comments.patternTriggerCheck);
                        statement.setString(1, Core.getUUID(entity));
                        if (!pattern.isEmpty()) {
                            statement.setString(2, '%' + pattern + '%');
                        }

                        final ResultSet rs = statement.executeQuery();
                        try {
                            if (rs.next()) {
                                int count = rs.getInt("COUNT(*)");
                                if (trigger.getTriggerNumber() == count) {
                                    trigger.onTrigger(entity, comment);
                                    break;
                                }
                            }
                        } finally {
                            rs.close();
                            statement.close();
                        }

                    }
                }
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }

    /**
     * Clear all the comments and warning of an entity or the specified one
     *
     * @param entity The entity to clear
     * @param commentID | use -1 to remove all the comments
     * @return The cleared message
     */
    public String clearComments(final String entity, final int commentID) {
        PreparedStatement statement = null;
        try (Connection conn = BATR.getConnection()) {
            if (commentID == -1) {
                statement = conn.prepareStatement(SQLQueries.Comments.clearEntries);
                statement.setString(1, (UtilsKt.validIP(entity)) ? entity : Core.getUUID(entity));
            } else {
                statement = conn.prepareStatement(SQLQueries.Comments.clearByID);
                statement.setString(1, (UtilsKt.validIP(entity)) ? entity : Core.getUUID(entity));
                statement.setInt(2, commentID);
            }
            // Check if it was successfully deleted, will be used if tried to delete an specific id comment
            boolean deleted = statement.executeUpdate() > 0;

            if (commentID != -1) {
                if (!deleted) {
                    throw new IllegalArgumentException(i18n.format("noCommentIDFound", new String[]{entity}));
                }
                return i18n.format("commentIDCleared", new String[]{String.valueOf(commentID), entity});
            }

            return i18n.format("commentsCleared", new String[]{entity});
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }
}
