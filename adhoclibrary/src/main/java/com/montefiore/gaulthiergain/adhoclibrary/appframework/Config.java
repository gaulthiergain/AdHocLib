package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions.BadServerPortException;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions.MaxThreadReachedException;

import java.util.UUID;

/**
 * <p>This class defines several different parameters to setup the library's behaviour. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class Config {

    private static final short MAX_THREAD = 8;
    private static final int MIN_PORT = 1023;
    private static final int MAX_PORT = 65535;

    private String label;
    private int timeout;
    private int serverPort;
    private short nbThreadBt;
    private short nbThreadWifi;
    private boolean json;
    private boolean secure;
    private boolean connectionFlooding;
    private boolean reliableTransportWifi;

    /**
     * Default constructor
     */
    public Config() {
        this.json = true;
        this.secure = true;
        this.nbThreadBt = 7;
        this.nbThreadWifi = 10;
        this.timeout = 5000;
        this.serverPort = 52000;
        this.connectionFlooding = false;
        this.reliableTransportWifi = true;
        this.label = String.valueOf(UUID.randomUUID());
    }

    /**
     * Constructor
     *
     * @param label                 a String value which represents the unique identifier of a
     *                              mobile node.
     * @param json                  a boolean value to use json or bytes in network transfer.
     *                              It is true if json is used. Otherwise, byte streams are used.
     * @param secure                a boolean value to determine if the Bluetooth connection is
     *                              secure. It is true if the connection is secured (ciphered).
     *                              Otherwise, the connection is not secured.
     * @param serverPort            an integer value to set the listening port number (for Wi-Fi).
     * @param nbThreadBt            a short value to determine the number of threads managed by the
     *                              Bluetooth server.
     * @param nbThreadWifi          a short value to determine the number of threads managed by the
     *                              Wi-Fi server.
     * @param reliableTransportWifi a boolean value to use TCP or UDP in network transfer for Wi-Fi.
     *                              It is true if TCP is used. Otherwise, UDP is used.
     * @param connectionFlooding    a boolean value to use network discovery. It allows to discover
     *                              the neighbours of the direct neighbours. It is true if discovery
     *                              is used. Otherwise, discovery is not used.
     */
    public Config(String label, boolean json, boolean secure, int serverPort,
                  short nbThreadBt, short nbThreadWifi, boolean reliableTransportWifi,
                  boolean connectionFlooding) {
        this.label = label;
        this.json = json;
        this.secure = secure;
        this.serverPort = serverPort;
        this.nbThreadBt = nbThreadBt;
        this.nbThreadWifi = nbThreadWifi;
        this.connectionFlooding = connectionFlooding;
        this.reliableTransportWifi = reliableTransportWifi;
    }

    /**
     * Constructor
     *
     * @param secure     a boolean value to determine if the Bluetooth connection is
     *                   secure. It is true if the connection is secured (ciphered).
     * @param nbThreadBt a short value to determine the number of threads managed by the
     *                   Bluetooth server.
     * @throws MaxThreadReachedException signals that the maximum number of threads is reached (7
     *                                   for Bluetooth and not defined for Wi-Fi).
     */
    public Config(boolean secure, int nbThreadBt) throws MaxThreadReachedException {
        this();
        this.secure = secure;
        this.setNbThreadBt(nbThreadBt);
    }

    /**
     * Constructor
     *
     * @param serverPort   an integer value to set the listening port number (for Wi-Fi).
     * @param nbThreadWifi a short value to determine the number of threads managed by the
     *                     Wi-Fi server.
     * @throws BadServerPortException signals that the listening port for Wi-Fi server is incorrect.
     *                                It must be between 1024 and 65534.
     */
    public Config(int serverPort, int nbThreadWifi) throws BadServerPortException {
        this();
        this.setServerPort(serverPort);
        this.nbThreadWifi = (short) nbThreadWifi;
    }

    /**
     * Constructor
     *
     * @param serverPort            an integer value to set the listening port number (for Wi-Fi).
     * @param nbThreadWifi          a short value to determine the number of threads managed by the
     *                              Wi-Fi server.
     * @param reliableTransportWifi a boolean value to use TCP or UDP in network transfer for Wi-Fi.
     *                              It is true if TCP is used. Otherwise, UDP is used.
     * @throws BadServerPortException signals that the listening port for Wi-Fi server is incorrect.
     *                                It must be between 1024 and 65534.
     */
    public Config(int serverPort, int nbThreadWifi, boolean reliableTransportWifi)
            throws BadServerPortException {
        this();
        this.setServerPort(serverPort);
        this.nbThreadWifi = (short) nbThreadWifi;
        this.reliableTransportWifi = reliableTransportWifi;
    }

    /**
     * Method allowing to get the listening port number (for Wi-Fi).
     *
     * @return an integer value to set the listening port number (for Wi-Fi).
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Method allowing to get the connection timeout (for Wi-Fi/Bluetooth).
     *
     * @return an integer value to set connection timeout (for Wi-Fi/Bluetooth).
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Method allowing to get the number of threads managed by the Bluetooth server.
     *
     * @return a short value to determine the number of threads managed by the Bluetooth server.
     */
    public short getNbThreadBt() {
        return nbThreadBt;
    }

    /**
     * Method allowing to get the number of threads managed by the Wi-Fi server.
     *
     * @return a short value to determine the number of threads managed by the Wi-Fi server.
     */
    public short getNbThreadWifi() {
        return nbThreadWifi;
    }

    /**
     * Method allowing to get the unique identifier of a mobile node.
     *
     * @return a String value which represents the unique identifier of a mobile node.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Method allowing to know if the connectionFlooding flag is set.
     *
     * @return a boolean value to use network discovery. It allows to discover the neighbours of the
     * direct neighbours. It is true if discovery is used. Otherwise, discovery is not used.
     */
    public boolean isConnectionFlooding() {
        return connectionFlooding;
    }

    /**
     * Method allowing to know if the reliableTransportWifi flag is set.
     *
     * @return a boolean value to use TCP or UDP in network transfer for Wi-Fi. It is true if TCP
     * is used. Otherwise, UDP is used.
     */
    public boolean isReliableTransportWifi() {
        return reliableTransportWifi;
    }

    /**
     * Method allowing to know if the secure flag is set.
     *
     * @return a boolean value to determine if the Bluetooth connection is secure. It is true if the
     * connection is secured (ciphered). Otherwise, the connection is not secured.
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Method allowing to know if the json flag is set.
     *
     * @return a boolean value to use json or bytes in network transfer. It is true if json is used.
     * Otherwise, byte streams are used.
     */
    public boolean isJson() {
        return json;
    }

    /**
     * Method allowing to set the secure flag.
     *
     * @param secure a boolean value to determine if the Bluetooth connection is secure. It is true
     *               if the connection is secured (ciphered). Otherwise, the connection is not secured.
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * Method allowing to set the timeout (in seconds) for Bluetooth and Wi-Fi connection.
     *
     * @param timeout an integer value which represents the connection timeout (for Wi-Fi/Bluetooth).
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Method allowing to set the listening port number (for Wi-Fi).
     *
     * @param serverPort an integer value which represents the listening port number (for Wi-Fi).
     * @throws BadServerPortException signals that the listening port for Wi-Fi server is incorrect.
     *                                It must be between 1024 and 65534.
     */
    public void setServerPort(int serverPort) throws BadServerPortException {
        if (serverPort <= MIN_PORT || serverPort >= MAX_PORT) {
            throw new BadServerPortException("The server port must be in range ["
                    + (MIN_PORT + 1) + " , " + (MAX_PORT - 1) + "]");
        } else {
            this.serverPort = serverPort;
        }
    }

    /**
     * Method allowing to set the number of threads managed by the Bluetooth server.
     *
     * @param nbThreadBt a short value which represents the number of threads managed by the
     *                   Bluetooth server.
     * @throws MaxThreadReachedException signals that the maximum number of threads is reached (7
     *                                   for Bluetooth and not defined for Wi-Fi).
     */
    public void setNbThreadBt(int nbThreadBt) throws MaxThreadReachedException {

        if (nbThreadBt >= MAX_THREAD) {
            throw new MaxThreadReachedException("Number of threads must be smaller than " + MAX_THREAD);
        } else {
            this.nbThreadBt = (short) nbThreadBt;
        }
    }

    /**
     * Method allowing to set the number of threads managed by the Wi-Fi server.
     *
     * @param nbThreadWifi a short value which represents the number of threads managed by the
     *                     Wi-Fi server.
     */
    public void setNbThreadWifi(int nbThreadWifi) {
        this.nbThreadWifi = (short) nbThreadWifi;
    }

    /**
     * Method allowing to set the label of the current device.
     *
     * @param label a String object which represents the unique identifier of a
     *              mobile node.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Method allowing to set the connectionFlooding flag.
     *
     * @param reliableTransportWifi a boolean value to use TCP or UDP in network transfer for Wi-Fi.
     *                              It is true if TCP is used. Otherwise, UDP is used.
     */
    public void setReliableTransportWifi(boolean reliableTransportWifi) {
        this.reliableTransportWifi = reliableTransportWifi;
    }

    /**
     * Method allowing to set the connectionFlooding flag.
     *
     * @param json a boolean value to use json or bytes in network transfer.
     *             It is true if json is used. Otherwise, byte streams are used.
     */
    public void setJson(boolean json) {
        this.json = json;
    }

    /**
     * Method allowing to set the connectionFlooding flag.
     *
     * @param connectionFlooding a boolean value to use network discovery. It allows to discover
     *                           the neighbours of the direct neighbours. It is true if discovery
     *                           is used. Otherwise, discovery is not used.
     */
    public void setConnectionFlooding(boolean connectionFlooding) {
        this.connectionFlooding = connectionFlooding;
    }
}
