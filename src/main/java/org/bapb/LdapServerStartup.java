package org.bapb;


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import java.io.InputStream;

@ApplicationScoped
public class LdapServerStartup {

    public void onStart(@Observes StartupEvent ev) throws Exception {
        DirectoryService service = new DefaultDirectoryService();
        service.setShutdownHookEnabled(true);

        // Create partition
        JdbmPartition partition = new JdbmPartition();
        partition.setId("example");
        partition.setSuffix("dc=example,dc=com");
        service.addPartition(partition);

        // Start the service
        service.startup();

        // Load LDIF file
        try (InputStream is = getClass().getResourceAsStream("/data.ldif")) {
            for (LdifEntry ldifEntry : new LdifReader(is)) {
                Entry entry = new DefaultEntry(service.getSchemaManager(), ldifEntry.getEntry());
                service.getAdminSession().add(entry);
            }
        }

        // Create and start the LDAP server
        LdapServer ldapServer = new LdapServer();
        ldapServer.setDirectoryService(service);
        TcpTransport ldapTransport = new TcpTransport(10389);
        ldapServer.setTransports(ldapTransport);
        ldapServer.start();
    }
}
