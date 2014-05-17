package com.toastcoders.jschssh

import com.jcraft.jsch.ConfigRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import grails.util.Holders
import org.apache.log4j.Logger

/**
 * Created with IntelliJ IDEA.
 * User: Michael Rice
 * Twitter: @errr_
 * Website: http://www.errr-online.com/
 * Github: https://github.com/michaelrice
 * Date: 5/17/2014
 * Time: 6:50 PM
 * Licenses: MIT http://opensource.org/licenses/MIT
 */
class ConnectionInfo {

    /**
     * Logger
     */
    private Logger log = Logger.getLogger(ConnectionInfo)

    /**
     * Username from the config file.
     *
     */
    public String username = config.user

    /**
     * Password from the config file.
     *
     */
    public String password = config.password

    /**
     * Full path to the key file.
     * Tomcat user must have read access to this file.
     *
     */
    public String keyFile = config.keyFile

    /**
     * Password for the key file.
     *
     */
    public String keyFilePassword = config.keyFilePassword

    /**
     * This setting allows you to ignore hosts you have not accepted a key
     * for. This is handy in development mode, but should not be used in
     * in production. The default is yes. In development mode set to "no".
     *
     */
    public String strictHostKeyChecking = config.StrictHostKeyChecking

    /**
     * The port the remote ssh server is listening on. Default is 22.
     */
    public int port = config.port

    /**
     * Full path to the ssh known hosts file. The tomcat user will need
     * read access to this file. The default is ~/.ssh/known_hosts
     *
     */
    public String knownHostsFile = config.knownHostsFile

    /**
     * Hostname of the remote host you wish to connect to.
     * This can be a hostname or an IP address.
     *
     */
    public String host

    /**
     * The command you wish to run on the remote server.
     *
     */
    public String command

    /**
     * The full path to the ssh config file you wish to load.
     * The file is loaded first, so options in the class
     * and in the application config will over ride what is in
     * the file.
     *
     * For example you set sshCondifgFile = "~/.ssh/config"
     * Next in this file you have the following option set:
     *
     * StrictHostKeyChecking yes
     *
     * You can over ride that in the class by setting:
     * this.strictHostKeyChecking = "no"
     *
     * Or by using the Config.groovy and setting:
     * jschSsh2 {
     *     StrictHostKeyChecking = "no"
     * }
     *
     * This is to allow you to over ride settings while in
     * development mode.
     */
    public String sshConfigFile = config.sshConfigFile

    /**
     * Connection timeout for connecting to a remote host.
     */
    public int connectionTimeout = config.connectionTimeout

    /**
     * Retrieve Jsch Ssh portion of the Configuration.
     */
    public ConfigObject getConfig() {
        return Holders.config.jschSsh2
    }

    public Session getSession() throws JSchException {
        try {
            log.debug("Executing command on remote server.")
            JSch jSch = new JSch()
            // session object used once connected
            Session session

            // if the hosts file variable has been set then attempt
            // to load into JSch object.
            if (!knownHostsFile?.empty) {
                jSch.setKnownHosts(knownHostsFile)
            }

            // If the config file is set attempt to load it
            if (!sshConfigFile?.empty) {
                ConfigRepository configRepository = com.jcraft.jsch.OpenSSHConfig.parse(sshConfigFile)
                jSch.setConfigRepository(configRepository)
            }

            // If keyFile is set and password is not attempt to use the key to auth
            if (!keyFile?.empty && password?.empty) {
                if (!keyFilePassword?.empty) {
                    jSch.addIdentity(keyFile, keyFilePassword)
                }
                else {
                    jSch.addIdentity(keyFile)
                }
            }

            session = jSch.getSession(username, host, port)
            // If the connectionTimeout is set use it instead of jsch default.
            if (connectionTimeout) {
                session.timeout = connectionTimeout
            }

            if (!password?.empty) {
                // If this is not set maybe its a key auth?
                session.setPassword(password)
            }

            session.setConfig("StrictHostKeyChecking", strictHostKeyChecking)

            // Connect to the server to run the command.
            session.connect()
            log.trace("connected to server.")
            return session
        }
        catch (JSchException e) {
            log.debug("Failed to create session to host.")
            throw e
        }
    }

    /**
     * Runs a passed closure to implement builder-style operation.
     *
     * @param closure
     */
    public void run(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.OWNER_FIRST
        closure.call()
    }

}