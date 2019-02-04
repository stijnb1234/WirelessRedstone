package net.licks92.WirelessRedstone.Storage;

import com.tylersuehr.sql.ContentValues;
import com.tylersuehr.sql.SQLiteDatabase;
import com.tylersuehr.sql.SQLiteOpenHelper;
import net.licks92.WirelessRedstone.Signs.SignType;
import net.licks92.WirelessRedstone.Signs.WirelessChannel;
import net.licks92.WirelessRedstone.Signs.WirelessPoint;
import net.licks92.WirelessRedstone.Signs.WirelessReceiver;
import net.licks92.WirelessRedstone.Signs.WirelessReceiverClock;
import net.licks92.WirelessRedstone.Signs.WirelessReceiverDelayer;
import net.licks92.WirelessRedstone.Signs.WirelessReceiverInverter;
import net.licks92.WirelessRedstone.Signs.WirelessReceiverSwitch;
import net.licks92.WirelessRedstone.Signs.WirelessScreen;
import net.licks92.WirelessRedstone.Signs.WirelessTransmitter;
import net.licks92.WirelessRedstone.WirelessRedstone;
import org.apache.commons.io.IOUtils;
import org.bukkit.block.BlockFace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class DatabaseClient extends SQLiteOpenHelper {
    private static final String DB_NAME = "WirelessRedstoneDatabase";
    private static final int DB_VERSION = 1;
    private static volatile DatabaseClient instance;
    private final SQLiteDatabase db;


    private DatabaseClient(String channelFolder) {
        super(channelFolder + File.separator + DB_NAME, DB_VERSION);
        this.db = getWritableInstance();
    }

    protected static synchronized DatabaseClient getInstance() {
        return getInstance(WirelessRedstone.getInstance().getDataFolder() + File.separator + WirelessRedstone.CHANNEL_FOLDER);
    }

    protected static synchronized DatabaseClient getInstance(String channelFolder) {
        if (instance == null) {
            if (channelFolder == null) {
                throw new IllegalArgumentException("Channel folder can't be null");
            }

            instance = new DatabaseClient(channelFolder);
        }
        return instance;
    }

    @Override
    protected void onCreate(SQLiteDatabase db) {
        try {
            String sql = IOUtils.toString(WirelessRedstone.getInstance().getResource("Database_1.sql"), StandardCharsets.UTF_8);
            db.execSql(sql);
        } catch (IOException e) {
            WirelessRedstone.getWRLogger().info("There was an error while initializing the database.");

            e.printStackTrace();
        }
    }

    @Override
    protected void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (WirelessRedstone.getStorage().backupData()) {
            if (oldVersion == 0) {
                try {
                    WirelessRedstone.getWRLogger().info("Updating SQLite database. This could take a while. As a precaution, a backup will be created.");
                    performUpdate1(db);
                    WirelessRedstone.getWRLogger().info("Updating SQLite database done.");
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("There was an error while performing update 1.");
                }
            }
        } else {
            throw new RuntimeException("There was an error while performing a database update. The channels folder couldn't be accessed.");
        }
    }

    /**
     * Expose the SQLiteDatabase to anything that wants to use it.
     */
    protected SQLiteDatabase getDatabase() {
        return db;
    }

    protected Collection<WirelessChannel> getAllChannels() {
        Collection<WirelessChannel> channels = new ArrayList<>();

        try {
            ResultSet resultSet = getDatabase().query("channel",null, null, null);
            while (resultSet.next()) {
                channels.add(new WirelessChannel(resultSet.getString("name"), resultSet.getBoolean("locked")));
            }

            resultSet.close();

            Iterator<WirelessChannel> iterator = channels.iterator();
            while (iterator.hasNext()) {
                WirelessChannel channel = iterator.next();

                resultSet = getDatabase().query("owner", new String[]{"user"}, "[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    channel.addOwner(resultSet.getString("user"));
                }

                resultSet.close();

                resultSet = getDatabase().query("transmitter","[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    WirelessPoint point = new WirelessTransmitter(
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("world"),
                            resultSet.getInt("is_wallsign") != 0,
                            BlockFace.valueOf(resultSet.getString("direction")),
                            resultSet.getString("owner")
                    );
                    channel.addWirelessPoint(point);
                    WirelessRedstone.getWRLogger().debug("Transmitter found: " + point);
                }

                resultSet.close();

                resultSet = getDatabase().query("receiver","[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    WirelessPoint point = new WirelessReceiver(
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("world"),
                            resultSet.getInt("is_wallsign") != 0,
                            BlockFace.valueOf(resultSet.getString("direction")),
                            resultSet.getString("owner")
                    );
                    channel.addWirelessPoint(point);
                    WirelessRedstone.getWRLogger().debug("Receiver found: " + point);
                }

                resultSet.close();

                resultSet = getDatabase().query("screen","[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    WirelessPoint point = new WirelessScreen(
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("world"),
                            resultSet.getInt("is_wallsign") != 0,
                            BlockFace.valueOf(resultSet.getString("direction")),
                            resultSet.getString("owner")
                    );
                    channel.addWirelessPoint(point);
                    WirelessRedstone.getWRLogger().debug("Screen found: " + point);
                }

                resultSet.close();

                resultSet = getDatabase().query("inverter","[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    WirelessPoint point = new WirelessReceiverInverter(
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("world"),
                            resultSet.getInt("is_wallsign") != 0,
                            BlockFace.valueOf(resultSet.getString("direction")),
                            resultSet.getString("owner")
                    );
                    channel.addWirelessPoint(point);
                    WirelessRedstone.getWRLogger().debug("Inverter found: " + point);
                }

                resultSet.close();

                resultSet = getDatabase().query("delayer","[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    WirelessPoint point = new WirelessReceiverDelayer(
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("world"),
                            resultSet.getInt("is_wallsign") != 0,
                            BlockFace.valueOf(resultSet.getString("direction")),
                            resultSet.getString("owner"),
                            resultSet.getInt("delay")
                    );
                    channel.addWirelessPoint(point);
                    WirelessRedstone.getWRLogger().debug("Delayer found: " + point);
                }

                resultSet.close();

                resultSet = getDatabase().query("switch","[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    WirelessPoint point = new WirelessReceiverSwitch(
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("world"),
                            resultSet.getInt("is_wallsign") != 0,
                            BlockFace.valueOf(resultSet.getString("direction")),
                            resultSet.getString("owner"),
                            resultSet.getBoolean("state")
                    );
                    channel.addWirelessPoint(point);
                    WirelessRedstone.getWRLogger().debug("Switch found: " + point);
                }

                resultSet.close();

                resultSet = getDatabase().query("clock","[channel_name]='" + channel.getName() + "'", null, null);
                while (resultSet.next()) {
                    WirelessPoint point = new WirelessReceiverClock(
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("world"),
                            resultSet.getInt("is_wallsign") != 0,
                            BlockFace.valueOf(resultSet.getString("direction")),
                            resultSet.getString("owner"),
                            resultSet.getInt("delay")
                    );
                    channel.addWirelessPoint(point);
                    WirelessRedstone.getWRLogger().debug("Clock found: " + point);
                }

                resultSet.close();
            }
        } catch (SQLException e) {
            WirelessRedstone.getWRLogger().severe("Couldn't retrieve channels from the database!");

            e.printStackTrace();
        }
        return channels;
    }

    protected void recreateDatabase() {
        onCreate(getDatabase());
    }

    private void performUpdate1(SQLiteDatabase db) throws SQLException, IOException {
        Collection<WirelessChannel> channels = new ArrayList<>();
        Collection<String> channelNames = new ArrayList<>();
        int channelIteration = 0;
        int progress = 0;

        ResultSet resultSet = db.rawQuery("SELECT [name] FROM [sqlite_master] WHERE [type] = 'table'");

        while (resultSet.next()) {
            WirelessRedstone.getWRLogger().debug("Found channel: " + resultSet.getString(1));
            channelNames.add(resultSet.getString(1));
        }

        resultSet.close();

        for (String channelName : channelNames) {
            if ((int)Math.floor((float)channelIteration / (float)channelNames.size() * 100) % 5 == 0
                    && (int)Math.floor((float)channelIteration / (float)channelNames.size() * 100) != progress) {
                progress = (int)Math.floor((float)channelIteration / (float)channelNames.size() * 100);
                WirelessRedstone.getWRLogger().info("Stage 1/2; Progress: " + progress + "%");
            }

            WirelessChannel channel = null;
            int channelInfoIteration = 0;
            resultSet = db.query(channelName, null, null, null);

            while (resultSet.next()) {
                if (channelInfoIteration == 0) {
                    if (resultSet.getString("name") != null) {
                        WirelessRedstone.getWRLogger().debug("---------------");
                        WirelessRedstone.getWRLogger().debug("Created channel: " + resultSet.getString("name") + " | " +
                                Collections.singletonList(resultSet.getString("owners")) + " | " +
                                resultSet.getBoolean("locked")
                        );

                        channel = new WirelessChannel(
                                resultSet.getString("name"),
                                Collections.singletonList(resultSet.getString("owners")),
                                resultSet.getBoolean("locked")
                        );
                    }
                }

                if (channelInfoIteration > 0 && channel == null) {
                    continue;
                }

                WirelessRedstone.getWRLogger().debug("---------------");

                if (channelInfoIteration > 0) {
                    if (resultSet.getString("signType") != null) {
                        String signTypeSerialized = resultSet.getString("signType");
                        SignType signType = getSignType(signTypeSerialized);
                        WirelessRedstone.getWRLogger().debug("SignType " + signType);

                        if (signType == null) {
                            continue;
                        }

                        switch (signType) {
                            case TRANSMITTER:
                                WirelessPoint point = new WirelessTransmitter(
                                        resultSet.getInt("x"),
                                        resultSet.getInt("y"),
                                        resultSet.getInt("z"),
                                        resultSet.getString("world"),
                                        resultSet.getInt("isWallSign") != 0,
                                        BlockFace.valueOf(resultSet.getString("direction")),
                                        resultSet.getString("signOwner")
                                );
                                channel.addWirelessPoint(point);

                                WirelessRedstone.getWRLogger().debug(point.toString());
                                break;
                            case RECEIVER:
                                point = new WirelessReceiver(
                                        resultSet.getInt("x"),
                                        resultSet.getInt("y"),
                                        resultSet.getInt("z"),
                                        resultSet.getString("world"),
                                        resultSet.getInt("isWallSign") != 0,
                                        BlockFace.valueOf(resultSet.getString("direction")),
                                        resultSet.getString("signOwner")
                                );
                                channel.addWirelessPoint(point);

                                WirelessRedstone.getWRLogger().debug(point.toString());
                                break;
                            case SCREEN:
                                point = new WirelessScreen(
                                        resultSet.getInt("x"),
                                        resultSet.getInt("y"),
                                        resultSet.getInt("z"),
                                        resultSet.getString("world"),
                                        resultSet.getInt("isWallSign") != 0,
                                        BlockFace.valueOf(resultSet.getString("direction")),
                                        resultSet.getString("signOwner")
                                );
                                channel.addWirelessPoint(point);

                                WirelessRedstone.getWRLogger().debug(point.toString());
                                break;
                            case RECEIVER_INVERTER:
                                point = new WirelessReceiverInverter(
                                        resultSet.getInt("x"),
                                        resultSet.getInt("y"),
                                        resultSet.getInt("z"),
                                        resultSet.getString("world"),
                                        resultSet.getInt("isWallSign") != 0,
                                        BlockFace.valueOf(resultSet.getString("direction")),
                                        resultSet.getString("signOwner")
                                );
                                channel.addWirelessPoint(point);

                                WirelessRedstone.getWRLogger().debug(point.toString());
                                break;
                            case RECEIVER_DELAYER:
                                int delay;

                                try {
                                    delay = Integer.parseInt(signTypeSerialized.split("_")[2]);
                                } catch (NumberFormatException e) {
                                    continue;
                                }

                                point = new WirelessReceiverDelayer(
                                        resultSet.getInt("x"),
                                        resultSet.getInt("y"),
                                        resultSet.getInt("z"),
                                        resultSet.getString("world"),
                                        resultSet.getInt("isWallSign") != 0,
                                        BlockFace.valueOf(resultSet.getString("direction")),
                                        resultSet.getString("signOwner"),
                                        delay
                                );
                                channel.addWirelessPoint(point);

                                WirelessRedstone.getWRLogger().debug(point.toString());
                                break;
                            case RECEIVER_SWITCH:
                                boolean state;

                                state = Boolean.parseBoolean(signTypeSerialized.split("_")[2]);

                                point = new WirelessReceiverSwitch(
                                        resultSet.getInt("x"),
                                        resultSet.getInt("y"),
                                        resultSet.getInt("z"),
                                        resultSet.getString("world"),
                                        resultSet.getInt("isWallSign") != 0,
                                        BlockFace.valueOf(resultSet.getString("direction")),
                                        resultSet.getString("signOwner"),
                                        state
                                );
                                channel.addWirelessPoint(point);

                                WirelessRedstone.getWRLogger().debug(point.toString());
                                break;
                            case RECEIVER_CLOCK:
                                try {
                                    delay = Integer.parseInt(signTypeSerialized.split("_")[2]);
                                } catch (NumberFormatException e) {
                                    continue;
                                }

                                point = new WirelessReceiverDelayer(
                                        resultSet.getInt("x"),
                                        resultSet.getInt("y"),
                                        resultSet.getInt("z"),
                                        resultSet.getString("world"),
                                        resultSet.getInt("isWallSign") != 0,
                                        BlockFace.valueOf(resultSet.getString("direction")),
                                        resultSet.getString("signOwner"),
                                        delay
                                );
                                channel.addWirelessPoint(point);

                                WirelessRedstone.getWRLogger().debug(point.toString());
                                break;
                        }
                    }
                }
                channelInfoIteration++;
            }
            resultSet.close();
            channels.add(channel);
            channelIteration++;

            db.execSql("DROP TABLE IF EXISTS [" + channelName + "];");
        }

        WirelessRedstone.getWRLogger().debug("---------------");

        String sql = IOUtils.toString(WirelessRedstone.getInstance().getResource("Database_1.sql"), StandardCharsets.UTF_8);
        db.execSql(sql);

        progress = 0;
        channelIteration = 0;
        for (WirelessChannel channel : channels) {
            if ((int)Math.floor((float)channelIteration / (float)channels.size() * 100) % 5 == 0
                    && (int)Math.floor((float)channelIteration / (float)channels.size() * 100) != progress) {
                progress = (int)Math.floor((float)channelIteration / (float)channels.size() * 100);
                WirelessRedstone.getWRLogger().info("Stage 2/2; Progress: " + progress + "%");
            }

            ContentValues values = new ContentValues();
            values.put("name", channel.getName());
            values.put("locked", channel.isLocked());
            db.insert("channel", values);
            WirelessRedstone.getWRLogger().debug("Inserted channel " + channel.getName());

            for (String owner : channel.getOwners()) {
                values = new ContentValues();
                values.put("channel_name", channel.getName());
                values.put("user", owner);
                db.insert("owner", values);
                WirelessRedstone.getWRLogger().debug("Inserted owner " + owner + "|" + channel.getName());
            }

            for (WirelessTransmitter point : channel.getTransmitters()) {
                values = new ContentValues();
                values.put("x", point.getX());
                values.put("y", point.getY());
                values.put("z", point.getZ());
                values.put("world", point.getWorld());
                values.put("channel_name", channel.getName());
                values.put("direction", point.getDirection().toString());
                values.put("owner", point.getOwner());
                values.put("is_wallsign", point.isWallSign());
                db.insert("transmitter", values);
                WirelessRedstone.getWRLogger().debug("Inserted transmitter " + point.toString() + "|" + channel.getName());
            }

            for (WirelessScreen point : channel.getScreens()) {
                values = new ContentValues();
                values.put("x", point.getX());
                values.put("y", point.getY());
                values.put("z", point.getZ());
                values.put("world", point.getWorld());
                values.put("channel_name", channel.getName());
                values.put("direction", point.getDirection().toString());
                values.put("owner", point.getOwner());
                values.put("is_wallsign", point.isWallSign());
                db.insert("screen", values);
                WirelessRedstone.getWRLogger().debug("Inserted screen " + point.toString() + "|" + channel.getName());
            }

            for (WirelessReceiver point : channel.getReceivers()) {
                if (point instanceof WirelessReceiverInverter) {
                    values = new ContentValues();
                    values.put("x", point.getX());
                    values.put("y", point.getY());
                    values.put("z", point.getZ());
                    values.put("world", point.getWorld());
                    values.put("channel_name", channel.getName());
                    values.put("direction", point.getDirection().toString());
                    values.put("owner", point.getOwner());
                    values.put("is_wallsign", point.isWallSign());
                    db.insert("inverter", values);
                    WirelessRedstone.getWRLogger().debug("Inserted inverter " + point.toString() + "|" + channel.getName());
                } else if (point instanceof WirelessReceiverDelayer) {
                    values = new ContentValues();
                    values.put("x", point.getX());
                    values.put("y", point.getY());
                    values.put("z", point.getZ());
                    values.put("world", point.getWorld());
                    values.put("channel_name", channel.getName());
                    values.put("direction", point.getDirection().toString());
                    values.put("owner", point.getOwner());
                    values.put("is_wallsign", point.isWallSign());
                    values.put("delay", ((WirelessReceiverDelayer) point).getDelay());
                    db.insert("delayer", values);
                    WirelessRedstone.getWRLogger().debug("Inserted delayer " + point.toString() + "|" + channel.getName());
                } else if (point instanceof WirelessReceiverSwitch) {
                    values = new ContentValues();
                    values.put("x", point.getX());
                    values.put("y", point.getY());
                    values.put("z", point.getZ());
                    values.put("world", point.getWorld());
                    values.put("channel_name", channel.getName());
                    values.put("direction", point.getDirection().toString());
                    values.put("owner", point.getOwner());
                    values.put("is_wallsign", point.isWallSign());
                    values.put("powered", ((WirelessReceiverSwitch) point).isActive());
                    db.insert("switch", values);
                    WirelessRedstone.getWRLogger().debug("Inserted switch " + point.toString() + "|" + channel.getName());
                } else if (point instanceof WirelessReceiverClock) {
                    values = new ContentValues();
                    values.put("x", point.getX());
                    values.put("y", point.getY());
                    values.put("z", point.getZ());
                    values.put("world", point.getWorld());
                    values.put("channel_name", channel.getName());
                    values.put("direction", point.getDirection().toString());
                    values.put("owner", point.getOwner());
                    values.put("is_wallsign", point.isWallSign());
                    values.put("delay", ((WirelessReceiverClock) point).getDelay());
                    WirelessRedstone.getWRLogger().debug("Inserted clock " + point.toString() + "|" + channel.getName());
                    db.insert("clock", values);
                } else {
                    values = new ContentValues();
                    values.put("x", point.getX());
                    values.put("y", point.getY());
                    values.put("z", point.getZ());
                    values.put("world", point.getWorld());
                    values.put("channel_name", channel.getName());
                    values.put("direction", point.getDirection().toString());
                    values.put("owner", point.getOwner());
                    values.put("is_wallsign", point.isWallSign());
                    db.insert("receiver", values);
                    WirelessRedstone.getWRLogger().debug("Inserted receiver " + point.toString() + "|" + channel.getName());
                }
            }
            channelIteration++;
        }
    }

    private SignType getSignType(String signTypeSerialized) {
        if (signTypeSerialized.equalsIgnoreCase("transmitter")) {
            return SignType.TRANSMITTER;
        } else if (signTypeSerialized.equalsIgnoreCase("receiver")) {
            return SignType.RECEIVER;
        } else if (signTypeSerialized.equalsIgnoreCase("screen")) {
            return SignType.SCREEN;
        } else if (signTypeSerialized.contains("receiver")) {
            String[] receiver = signTypeSerialized.split("_");

            if (receiver[1].equalsIgnoreCase("inverter")) {
                return SignType.RECEIVER_INVERTER;
            } else if (receiver[1].equalsIgnoreCase("delayer")) {
                return SignType.RECEIVER_DELAYER;
            } else if (receiver[1].equalsIgnoreCase("switch")) {
                return SignType.RECEIVER_SWITCH;
            } else if (receiver[1].equalsIgnoreCase("clock")) {
                return SignType.RECEIVER_CLOCK;
            }
        }

        return null;
    }
}