

alter table EAS_STORE add ACCESS_RULE VARCHAR2(25) DEFAULT 'DENY' NOT NULL;

alter table EAS_PATH_RESOURCE add READ_GROUP_1 VARCHAR2(100);
alter table EAS_PATH_RESOURCE add WRITE_GROUP_1 VARCHAR2(100);
alter table EAS_PATH_RESOURCE add EXECUTE_GROUP_1 VARCHAR2(100);