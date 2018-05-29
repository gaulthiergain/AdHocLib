package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;

/**
 * <p>This interface allows to define callback functions which must be directly used within the
 * code through {@link TransferManager} and {@link AutoTransferManager} classes.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ListenerApp {
    /**
     * Method allowing to notify when a Data message is received from a remote peer.
     *
     * @param adHocDevice an AdHocDevice object which represents the remote peer that has sent
     *                    the message.
     * @param pdu         a generic object which represents the pdu of the message.
     */
    void onReceivedData(AdHocDevice adHocDevice, Object pdu);

    /**
     * Method allowing to notify when a Data message from remote peer is forwarded.
     *
     * @param adHocDevice an AdHocDevice object which represents the remote peer that has sent
     *                    the message.
     * @param pdu         a generic object which represents the pdu of the message.
     */
    void onForwardData(AdHocDevice adHocDevice, Object pdu);

    /**
     * Method allowing to notify when a connection is performed with a remote peer.
     *
     * @param adHocDevice an AdHocDevice object which represents the remote peer with which
     *                    the connection is established.
     */
    void onConnection(AdHocDevice adHocDevice);

    /**
     * Method allowing to notify when a exception occurs during connection process.
     * A {@link java.io.IOException},
     * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException}
     * are exception that are thrown by the underlying layer.
     *
     * @param e an Exception object ({@link java.io.IOException} and
     *          {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException}
     *          caused by the underlying layer.
     */
    void onConnectionFailed(Exception e);

    /**
     * Method allowing to notify when a connection is closed with a remote peer.
     *
     * @param adHocDevice an AdHocDevice object which represents the remote peer with which
     *                    the connection is broken.
     */
    void onConnectionClosed(AdHocDevice adHocDevice);

    /**
     * Method allowing to notify when a exception occurs during closing a connection.
     * A {@link java.io.IOException},
     * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException}
     * are exception that are thrown by the underlying layer.
     *
     * @param e an Exception object ({@link java.io.IOException} and
     *          {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException}
     *          caused by the underlying layer.
     */
    void onConnectionClosedFailed(Exception e);

    /**
     * Method allowing to notify when a exception occurs during processing messages.
     * A {@link java.io.IOException}, {@link java.io.NotSerializableException} are exception that
     * are thrown by the underlying layer.
     *
     * @param e an Exception object ({@link java.io.IOException} and
     *          {@link java.io.NotSerializableException}) caused by the underlying layer.
     */
    void processMsgException(Exception e);
}
