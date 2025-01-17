package edu.ship.engr.shipsim.datasource;

import edu.ship.engr.shipsim.dataDTO.ObjectiveStateRecordDTO;
import edu.ship.engr.shipsim.datatypes.ObjectiveStateEnum;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

/**
 * The RDS Implementation of this gateway
 *
 * @author merlin
 */
public class ObjectiveStateTableDataGateway
{

    private static ObjectiveStateTableDataGateway singleton;

    /**
     * A private constructor only called by the getSingleton method
     */
    private ObjectiveStateTableDataGateway()
    {
        //do nothing this just explicitly makes it private
    }

    public static ObjectiveStateTableDataGateway getSingleton()
    {
        if (singleton == null)
        {
            singleton = new ObjectiveStateTableDataGateway();
        }
        return singleton;
    }

    /**
     * Create a new row in the table
     *
     * @param playerID            the player ID
     * @param questID             the quest that contains the objective
     * @param objectiveID         the unique ID of the objective
     * @param objectiveState      the state of this objective for this player
     * @param needingNotification true if the player should be notified about
     *                            the state of this objective
     * @throws DatabaseException if we can't talk to the RDS
     */
    public void createRow(int playerID, int questID, int objectiveID,
                          ObjectiveStateEnum objectiveState,
                          boolean needingNotification) throws DatabaseException
    {
        Connection connection = DatabaseManager.getSingleton().getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(
                "Insert INTO ObjectiveStates SET questID = ?, objectiveID = " +
                        "?, playerID = ?, objectiveState = ?, " +
                        "needingNotification = ?"))
        {
            stmt.setInt(1, questID);
            stmt.setInt(2, objectiveID);
            stmt.setInt(3, playerID);
            stmt.setInt(4, objectiveState.getID());
            stmt.setBoolean(5, needingNotification);
            stmt.executeUpdate();

        }
        catch (SQLException e)
        {
            throw new DatabaseException(
                    "Couldn't create a objective state record for objective " +
                            "with ID " +
                            objectiveID,
                    e);
        }
    }


    public void createDateRow(int playerID, int questID, int objectiveID,
                              ObjectiveStateEnum objectiveState,
                              boolean needingNotification,
                              LocalDate dateCompleted) throws DatabaseException
    {
        Connection connection = DatabaseManager.getSingleton().getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(
                "Insert INTO ObjectiveStates SET questID = ?, objectiveID = " +
                        "?, playerID = ?, objectiveState = ?, " +
                        "needingNotification = ?, dateCompleted = ?"))
        {
            stmt.setInt(1, questID);
            stmt.setInt(2, objectiveID);
            stmt.setInt(3, playerID);
            stmt.setInt(4, objectiveState.getID());
            stmt.setBoolean(5, needingNotification);
            stmt.setDate(6, Date.valueOf(dateCompleted));
            stmt.executeUpdate();

        }
        catch (SQLException e)
        {
            throw new DatabaseException(
                    "Couldn't create a objective state record for objective " +
                            "with ID " +
                            objectiveID,
                    e);
        }
    }

    /**
     * Drop the table if it exists and re-create it empty
     *
     * @throws DatabaseException shouldn't
     */
    public void createTable() throws DatabaseException
    {
        Connection connection = DatabaseManager.getSingleton().getConnection();

        String dropSql = "DROP TABLE IF EXISTS ObjectiveStates";
        String createSql =
                "Create TABLE ObjectiveStates (objectiveID INT NOT NULL, " +
                        "questID INT NOT NULL, playerID INT NOT NULL, "
                        +
                        "objectiveState INT, needingNotification BOOLEAN, " +
                        "dateCompleted DATE)";

        try (PreparedStatement stmt = connection.prepareStatement(dropSql))
        {
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Unable to drop ObjectiveStates table",
                    e);
        }

        try (PreparedStatement stmt = connection.prepareStatement(createSql))
        {
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new DatabaseException(
                    "Unable to create ObjectiveStates table", e);
        }
    }

    public ArrayList<ObjectiveStateRecordDTO> getObjectiveStates(int playerID,
                                                                 int questID)
            throws DatabaseException
    {
        Connection connection = DatabaseManager.getSingleton().getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM ObjectiveStates WHERE questID = ? and playerID" +
                        " = ?"))
        {
            stmt.setInt(1, questID);
            stmt.setInt(2, playerID);

            try (ResultSet result = stmt.executeQuery())
            {
                ArrayList<ObjectiveStateRecordDTO> results = new ArrayList<>();
                while (result.next())
                {
                    if (result.getDate("dateCompleted") == null)
                    {
                        ObjectiveStateRecordDTO rec =
                                new ObjectiveStateRecordDTO(
                                        result.getInt("playerID"),
                                        result.getInt("questID"),
                                        result.getInt("objectiveID"),
                                        convertToState(
                                                result.getInt(
                                                        "objectiveState")),
                                        result.getBoolean(
                                                "needingNotification"),
                                        null);
                        results.add(rec);
                    }
                    else
                    {
                        ObjectiveStateRecordDTO rec =
                                new ObjectiveStateRecordDTO(
                                        result.getInt("playerID"),
                                        result.getInt("questID"),
                                        result.getInt("objectiveID"),
                                        convertToState(result.getInt(
                                                "objectiveState")),
                                        result.getBoolean(
                                                "needingNotification"),
                                        result.getDate("dateCompleted")
                                                .toLocalDate());
                        results.add(rec);
                    }
                }
                return results;
            }

        }
        catch (SQLException e)
        {
            throw new DatabaseException(
                    "Couldn't find objective for quest ID " + questID,
                    e);
        }
    }

    public ArrayList<ObjectiveStateRecordDTO> getPendingObjectivesForPlayer(
            int playerID)
            throws DatabaseException
    {
        Connection connection = DatabaseManager.getSingleton().getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM ObjectiveStates WHERE objectiveState = ? and " +
                        "playerID = ?"))
        {
            stmt.setInt(1, ObjectiveStateEnum.TRIGGERED.getID());
            stmt.setInt(2, playerID);

            try (ResultSet result = stmt.executeQuery())
            {
                ArrayList<ObjectiveStateRecordDTO> results = new ArrayList<>();
                while (result.next())
                {
                    if (result.getDate("dateCompleted") == null)
                    {
                        ObjectiveStateRecordDTO rec =
                                new ObjectiveStateRecordDTO(
                                        result.getInt("playerID"),
                                        result.getInt("questID"),
                                        result.getInt("objectiveID"),
                                        convertToState(
                                                result.getInt(
                                                        "objectiveState")),
                                        result.getBoolean(
                                                "needingNotification"),
                                        null);
                        results.add(rec);
                    }
                    else
                    {
                        ObjectiveStateRecordDTO rec =
                                new ObjectiveStateRecordDTO(
                                        result.getInt("playerID"),
                                        result.getInt("questID"),
                                        result.getInt("objectiveID"),
                                        convertToState(
                                                result.getInt(
                                                        "objectiveState")),
                                        result.getBoolean(
                                                "needingNotification"),
                                        result.getDate("dateCompleted")
                                                .toLocalDate());
                        results.add(rec);
                    }
                }
                return results;
            }
        }
        catch (SQLException e)
        {
            throw new DatabaseException(
                    "Couldn't find pending objectives for player ID " +
                            playerID, e);
        }
    }

    /**
     * Get a list of all of the uncompleted (e.g. HIDDEN or TRIGGERED)
     * objectives that a player current has
     *
     * @param playerID the player
     * @return the list
     * @throws DatabaseException if we can't talk to the data source
     */
    public ArrayList<ObjectiveStateRecordDTO> getUncompletedObjectivesForPlayer(
            int playerID) throws DatabaseException
    {
        Connection connection = DatabaseManager.getSingleton().getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM ObjectiveStates WHERE (objectiveState = ? OR " +
                        "objectiveState = ?) and playerID = ?"))
        {

            stmt.setInt(1, ObjectiveStateEnum.TRIGGERED.getID());
            stmt.setInt(2, ObjectiveStateEnum.HIDDEN.getID());
            stmt.setInt(3, playerID);

            try (ResultSet result = stmt.executeQuery())
            {
                ArrayList<ObjectiveStateRecordDTO> results = new ArrayList<>();
                while (result.next())
                {
                    if (result.getDate("dateCompleted") == null)
                    {
                        ObjectiveStateRecordDTO rec =
                                new ObjectiveStateRecordDTO(
                                        result.getInt("playerID"),
                                        result.getInt("questID"),
                                        result.getInt("objectiveID"),
                                        convertToState(
                                                result.getInt(
                                                        "objectiveState")),
                                        result.getBoolean(
                                                "needingNotification"),
                                        null);
                        results.add(rec);
                    }
                    else
                    {
                        ObjectiveStateRecordDTO rec =
                                new ObjectiveStateRecordDTO(
                                        result.getInt("playerID"),
                                        result.getInt("questID"),
                                        result.getInt("objectiveID"),
                                        convertToState(
                                                result.getInt(
                                                        "objectiveState")),
                                        result.getBoolean(
                                                "needingNotification"),
                                        result.getDate(
                                                "dateCompleted").toLocalDate());
                        results.add(rec);
                    }
                }
                return results;
            }
        }
        catch (SQLException e)
        {
            throw new DatabaseException(
                    "Couldn't find pending objectives for player ID " +
                            playerID, e);
        }
    }

    public void updateState(int playerID, int questID, int objectiveID,
                            ObjectiveStateEnum newState,
                            boolean needingNotification)
            throws DatabaseException
    {

        Connection connection = DatabaseManager.getSingleton().getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE ObjectiveStates SET dateCompleted = ?, objectiveState" +
                        " = ?, needingNotification = ? WHERE playerID = ? " +
                        "and questID = ? and objectiveID = ?"))
        {
            if (newState == ObjectiveStateEnum.COMPLETED)
            {

                LocalDate date = LocalDate.now();
                stmt.setDate(1, Date.valueOf(date));
            }
            else
            {
                stmt.setDate(1, null);
            }
            stmt.setInt(2, newState.getID());
            stmt.setBoolean(3, needingNotification);
            stmt.setInt(4, playerID);
            stmt.setInt(5, questID);
            stmt.setInt(6, objectiveID);


            int count = stmt.executeUpdate();
            if (count == 0)
            {
                this.createRow(playerID, questID, objectiveID, newState,
                        needingNotification);
                this.updateState(playerID, questID, objectiveID, newState,
                        needingNotification);
            }
        }
        catch (SQLException e)
        {
            throw new DatabaseException(
                    "Couldn't update an objective state record for player " +
                            "with ID " +
                            playerID
                            + " and quest with ID " + questID, e);
        }

    }

    private ObjectiveStateEnum convertToState(int int1)
    {
        return ObjectiveStateEnum.values()[int1];
    }
}
