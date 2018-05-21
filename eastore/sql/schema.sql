drop sequence EAS_NODE_ID_SEQUENCE;
drop sequence EAS_LINK_ID_SEQUENCE;
drop sequence EAS_PRUNE_ID_SEQUENCE;
drop sequence EAS_STORE_ID_SEQUENCE;
drop sequence EAS_DOWNLOAD_ID_SEQUENCE;

drop table EAS_BINARY_RESOURCE;
drop table EAS_FILE_META_RESOURCE;
drop table EAS_DIRECTORY_RESOURCE;
drop table EAS_PATH_RESOURCE;
drop table EAS_STORE;
drop table EAS_NODE;
drop table EAS_CLOSURE;
drop table EAS_PRUNE;
drop table EAS_DOWNLOAD;

/**
 * Master list of all nodes for all trees.
 */
create table EAS_NODE ( 
	NODE_ID NUMBER(15,0) NOT NULL, 
	PARENT_NODE_ID NUMBER(15,0) NOT NULL,
	NODE_NAME VARCHAR2(250) NOT NULL,
	CREATION_DATE date NOT NULL, 
	UPDATED_DATE date NOT NULL, 
	PRIMARY KEY (NODE_ID) 
);

/**
 * Master table which stores data that describes the hierarchical structure for out trees.
 * There should be an entry from every parent node to every child node with it,
 * cooresponding depth.
 */
create table EAS_CLOSURE ( 
	LINK_ID NUMBER(15,0) NOT NULL, 
	PARENT_NODE_ID NUMBER(15,0) NOT NULL, 
	CHILD_NODE_ID NUMBER(15,0) NOT NULL, 
	DEPTH NUMBER(5,0) NOT NULL, 
	PRIMARY KEY (LINK_ID) 
);

/**
 * Used when pruning (deleting) branches from a tree.
 */
create table EAS_PRUNE ( 
	PRUNE_ID NUMBER(15,0) NOT NULL, 
	NODE_ID NUMBER(15,0) NOT NULL, 
	PRIMARY KEY (PRUNE_ID,NODE_ID) 
);

/**
 * We use our tree structure to model a files sytem. This table contains
 * the common data elements for any type of resource in a file system. There
 * is a one-to-one relationship between this table and EAS_NODE.
 * 
 * NODE_ID - unique ID for the node
 * STORE_ID - ID of the store that this node resides under
 * PATH_NAME - The name of the path resource, i.e., file name or directory name
 * PATH_TYPE - Specify the type of path resource, i.e. file or directory
 * RELATIVE_PATH - Path of the resource relative to the store path
 * PATH_DESC - optional description for the resource
 * READ_GROUP_1 - optional read access group for controlling read access of the resource
 * WRITE_GROUP_1 - optional write access group for controlling write (update) access to the resource
 */
create table EAS_PATH_RESOURCE ( 
	NODE_ID NUMBER(15,0) NOT NULL,
	STORE_ID NUMBER(15,0) NOT NULL,
	PATH_NAME VARCHAR2(250) NOT NULL,
	PATH_TYPE VARCHAR2(250) NOT NULL,
	RELATIVE_PATH VARCHAR2(4000) NOT NULL,
	PATH_DESC VARCHAR2(4000),
	READ_GROUP_1 VARCHAR2(100),
	WRITE_GROUP_1 VARCHAR2(100),
	EXECUTE_GROUP_1 VARCHAR2(100),
	PRIMARY KEY (NODE_ID) 
);

/**
 * Master list of directory resources. Table is empty for now, but it may contain stuff later.
 */
create table EAS_DIRECTORY_RESOURCE ( 
	NODE_ID NUMBER(15,0) NOT NULL,
	PRIMARY KEY (NODE_ID)
);

/**
 * This table contains all data for a file resource, except for the actual
 * binary data.
 */
create table EAS_FILE_META_RESOURCE ( 
	NODE_ID NUMBER(15,0) NOT NULL, 
	FILE_SIZE NUMBER(15,0) NOT NULL,
	MIME_TYPE VARCHAR2(100),
	IS_FILE_DATA_IN_DB CHAR(1) default 'N',
	PRIMARY KEY (NODE_ID) 
);

/**
 * This table contains the actual binary data for all files. Not every file
 * needs to be stored in the database. It's optional. The application will
 * attempt to read the file from the local file system, and if it's not
 * found then it will be pulled from the database.
 */
create table EAS_BINARY_RESOURCE ( 
	NODE_ID NUMBER(15,0) NOT NULL,
	FILE_DATA BLOB NOT NULL,
	PRIMARY KEY (NODE_ID) 
);

/**
 * Master list of stores.
 * 
 * NODE_ID - A root node of a tree in EAS_NODE. Should be a directory node.
 */
create table EAS_STORE ( 
	STORE_ID NUMBER(15,0) NOT NULL,
	STORE_NAME VARCHAR2(250) NOT NULL,
	STORE_DESCRIPTION VARCHAR2(4000) NOT NULL,
	STORE_PATH VARCHAR2(2000) NOT NULL,
	NODE_ID NUMBER(15,0) NOT NULL,
	MAX_FILE_SIZE_IN_DB NUMBER(15,0) DEFAULT 26214400 NOT NULL,
	ACCESS_RULE VARCHAR2(25) DEFAULT 'DENY' NOT NULL,
	CREATION_DATE date NOT NULL, 
	UPDATED_DATE date NOT NULL,	
	PRIMARY KEY (STORE_ID) 
);

/**
 * List of all downloads
 */
create table EAS_DOWNLOAD ( 
	DOWN_ID NUMBER(15,0) NOT NULL,
	FILE_PATH VARCHAR2(4000) NOT NULL,
	USER_ID VARCHAR2(100) NOT NULL,
	DOWN_DATE date NOT NULL,	
	PRIMARY KEY (DOWN_ID)
);

CREATE SEQUENCE EAS_NODE_ID_SEQUENCE  
MINVALUE 1 
MAXVALUE 999999999999999999999999999  
INCREMENT BY 1 
START WITH 100 
CACHE 10  
ORDER  
NOCYCLE;

CREATE SEQUENCE EAS_LINK_ID_SEQUENCE  
MINVALUE 1 
MAXVALUE 999999999999999999999999999  
INCREMENT BY 1 
START WITH 100 
CACHE 10  
ORDER  
NOCYCLE;

CREATE SEQUENCE EAS_PRUNE_ID_SEQUENCE  
MINVALUE 1 
MAXVALUE 999999999999999999999999999  
INCREMENT BY 1 
START WITH 100 
CACHE 10  
ORDER  
NOCYCLE;

CREATE SEQUENCE EAS_STORE_ID_SEQUENCE  
MINVALUE 1 
MAXVALUE 999999999999999999999999999  
INCREMENT BY 1 
START WITH 100 
CACHE 10  
ORDER  
NOCYCLE;

CREATE SEQUENCE EAS_DOWNLOAD_ID_SEQUENCE  
MINVALUE 1 
MAXVALUE 999999999999999999999999999  
INCREMENT BY 1 
START WITH 100 
CACHE 10  
ORDER  
NOCYCLE;