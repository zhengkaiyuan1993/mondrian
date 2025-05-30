/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package mondrian.server;

import mondrian.olap.MondrianServer;
import mondrian.olap.Util;
import mondrian.spi.CatalogLocator;
import mondrian.spi.impl.IdentityCatalogLocator;
import mondrian.util.LockBox;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Registry of all servers within this JVM, and also serves as a factory for
 * servers.
 *
 * <p>This class is not a public API. User applications should use the
 * methods in {@link mondrian.olap.MondrianServer}.
 *
 * @author jhyde
 */
public class MondrianServerRegistry {
    public static final Logger logger =
        LogManager.getLogger(MondrianServerRegistry.class);
    public static final MondrianServerRegistry INSTANCE =
        new MondrianServerRegistry();

    public MondrianServerRegistry() {
        super();
    }

    /**
     * Registry of all servers.
     */
    final LockBox lockBox = new LockBox();

    /**
     * The one and only one server that does not have a repository.
     */
    final MondrianServer staticServer =
        createWithRepository(null, null);

    /**
     * Looks up a server with a given id. If the id is null, returns the
     * static server.
     *
     * @param instanceId Unique identifier of server instance
     * @return Server
     * @throws RuntimeException if no server instance exists
     */
    public MondrianServer serverForId(String instanceId) {
        if (instanceId != null) {
            final LockBox.Entry entry = lockBox.get(instanceId);
            if (entry == null) {
                throw Util.newError(
                    "No server instance has id '" + instanceId + "'");
            }
            return (MondrianServer) entry.getValue();
        } else {
            return staticServer;
        }
    }

    public String getCopyrightYear() {
        return MondrianServerVersion.COPYRIGHT_YEAR;
    }

    public String getProductVersion() {
        return MondrianServerVersion.VERSION;
    }

    public int getSchemaVersion() { return MondrianServerVersion.SCHEMA_VERSION; }

    public MondrianServer.MondrianVersion getVersion() {
        if (logger.isDebugEnabled()){
            logger.debug(" Vendor: " + MondrianServerVersion.VENDOR);
            final String title = MondrianServerVersion.NAME;
            logger.debug("  Title: " + title);
            final String versionString = MondrianServerVersion.VERSION;
            logger.debug("Version: " + versionString);
            final int majorVersion = MondrianServerVersion.MAJOR_VERSION;
            logger.debug(String.format("Major Version: %d", majorVersion));
            final int minorVersion = MondrianServerVersion.MINOR_VERSION;
            logger.debug(String.format("Minor Version: %d", minorVersion));
        }
        final StringBuilder sb = new StringBuilder();
        try {
            Integer.parseInt(MondrianServerVersion.VERSION);
            sb.append(MondrianServerVersion.VERSION);
        } catch (NumberFormatException e) {
            // Version is not a number (e.g. "TRUNK-SNAPSHOT").
            // Fall back on VersionMajor, VersionMinor, if present.
            final String versionMajor =
                String.valueOf(MondrianServerVersion.MAJOR_VERSION);
            final String versionMinor =
                String.valueOf(MondrianServerVersion.MINOR_VERSION);
            if (versionMajor != null) {
                sb.append(versionMajor);
            }
            if (versionMinor != null) {
                sb.append(".").append(versionMinor);
            }
        }
        return new MondrianServer.MondrianVersion() {
            public String getVersionString() {
                return sb.toString();
            }
            public String getProductName() {
                return MondrianServerVersion.NAME;
            }
            public int getMinorVersion() {
                return MondrianServerVersion.MINOR_VERSION;
            }
            public int getMajorVersion() {
                return MondrianServerVersion.MAJOR_VERSION;
            }
            public int getSchemaVersion() {
                return MondrianServerVersion.SCHEMA_VERSION;
            }
        };
    }

    public MondrianServer createWithRepository(
        RepositoryContentFinder contentFinder,
        CatalogLocator catalogLocator)
    {
        if (catalogLocator == null) {
            catalogLocator = new IdentityCatalogLocator();
        }
        final Repository repository;
        if (contentFinder == null) {
            // NOTE: registry.staticServer is initialized by calling this
            // method; this is the only time that it is null.
            if (staticServer != null) {
                return staticServer;
            }
            repository = new ImplicitRepository();
        } else {
            repository = new FileRepository(contentFinder, catalogLocator);
        }
        return new MondrianServerImpl(this, repository, catalogLocator);
    }
}

// End MondrianServerRegistry.java
