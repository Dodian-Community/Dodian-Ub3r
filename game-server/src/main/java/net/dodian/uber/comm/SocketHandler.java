package net.dodian.uber.comm;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketHandler implements Runnable {

    private Client player;
    private Socket socket;

    private boolean processRunning;

    private PacketQueue packets = new PacketQueue();
    private Queue<PacketData> myPackets = new LinkedList<PacketData>();
    private Queue<byte[]> outData = new ConcurrentLinkedQueue<byte[]>();

    public SocketHandler(Client player, Socket socket) {
        this.player = player;
        this.socket = socket;
        this.processRunning = true;
    }

    public void run() {
        long lastProcess = System.currentTimeMillis();
        while (processRunning) {
            try {
                if (!isConnected()) {
                    myPackets.clear();
                    break;
                }
                while (writeOutput()) ;
                flush();
                if (lastProcess + 50 <= System.currentTimeMillis()) {
                    while (parsePackets()) ;
                    LinkedList<PacketData> temp = packets.getPackets();
                    if (myPackets == null)
                        myPackets.clear();
                    if (temp != null)
                        myPackets.addAll(temp);
                    lastProcess = System.currentTimeMillis();
                }
                    Thread.sleep(50);
            } catch (java.lang.Exception _ex) {
                YellSystem.alertStaff("Something is up with socket handler!");
                System.out.println("SocketHandling is throwing errors: " + _ex.getMessage());
            }
        }
    }

    private boolean isConnected() {
        return getInput() != null && getOutput() != null;
    }

    public Client getPlayer() {
        return player;
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getInput() {
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            logout();
        }
        return null;
    }

    public OutputStream getOutput() {
        try {
            return socket.getOutputStream();
        } catch (IOException e) {
            logout();
        }
        return null;
    }

    public void write(byte[] array) {
        try {
            getOutput().write(array);
        } catch (IOException e) {
            logout();
        }
    }

    public void write(byte[] data, int i, int length) {
        try {
            getOutput().write(data, i, length);
        } catch (IOException e) {
            logout();
        }
    }

    public void flush() {
        try {
            getOutput().flush();
        } catch (IOException e) {
            logout();
        }
    }

    public void logout() {
        this.processRunning = false;
        if (player == null)
            return;
        if (player.disconnected) // Already disconnected.
            return;
        if (player.loggingOut) {
            player.disconnected = true;
        } else {
            player.logout();
        }
    }

    public boolean parsePackets() {
        try {
            if (player.disconnected) {
                this.processRunning = false;
                return false;
            }
            if (getInput() == null) {
                this.processRunning = false;
                return false;
            }

            int avail = getInput().available();
            if (avail == 0) {
                return false;
            }
            if (player.packetType == -1) {
                player.packetType = getInput().read() & 0xff;
                if (player.inStreamDecryption != null) {
                    player.packetType = player.packetType - player.inStreamDecryption.getNextKey() & 0xff;
                }
                player.packetSize = Constants.PACKET_SIZES[player.packetType];
                avail--;
            }
            if (player.packetSize == -1) {
                if (avail > 0) {
                    player.packetSize = getInput().read() & 0xff;
                    avail--;
                } else {
                    return false;
                }
            }
            if (avail < player.packetSize) {
                return false;
            }
            fillInStream(player.packetType, player.packetSize);
            player.timeOutCounter = 0;
            player.packetType = -1;
        } catch (java.lang.Exception __ex) {
            player.saveStats(true);
            player.disconnected = true;
            this.processRunning = false;
            return false;
        }
        return true;
    }

    private void fillInStream(int id, int forceRead) throws java.io.IOException {
        byte[] data = new byte[forceRead];
        getInput().read(data, 0, forceRead);
        PacketData pData = new PacketData(id, data, forceRead);
        packets.add(pData);
    }

    public LinkedList<PacketData> getPackets() {
        return (LinkedList<PacketData>) myPackets;
    }

    /**
     * @return if the thread is still running
     */
    public boolean isRunning() {
        return processRunning;
    }

    public void setRunning(boolean running) {
        this.processRunning = running;
    }

    public void queueOutput(byte[] copy) {
        outData.add(copy);
    }

    public boolean writeOutput() {
        for (int i = 0; i < 20; i++) {
            if (outData.isEmpty())
                return false;
            byte[] data = outData.poll();
            if (data == null)
                continue;
            write(data);
        }
        return false;
    }

}
