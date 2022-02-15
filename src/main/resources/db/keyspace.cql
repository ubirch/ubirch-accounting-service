-- local
CREATE KEYSPACE acct_system WITH replication = {'class': 'SimpleStrategy','replication_factor': '1'};

-- dev/prod
CREATE KEYSPACE acct_system WITH replication = {'class': 'NetworkTopologyStrategy','AZURE_WEST_EUROPE_DC1': '3'};

-- helper to modify replication
ALTER KEYSPACE acct_system WITH REPLICATION = {'class': 'NetworkTopologyStrategy','AZURE_WEST_EUROPE_DC1': '3'};
