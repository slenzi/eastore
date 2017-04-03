/* sample store */
Insert into EAS_STORE (STORE_ID, STORE_NAME, STORE_DESCRIPTION, STORE_PATH, NODE_ID, MAX_FILE_SIZE_IN_DB, CREATION_DATE, UPDATED_DATE) values (1, 'Sample Store 1', 'Sample store for testing', 'C:/temp/eastore/sample1', 1, 52428800, to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'), to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );

/* sample path resources */
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (1,  1, 'test', 'Directory', '/test');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (2,  1, 'more', 'Directory', '/test/more');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (3,  1, 'other', 'Directory', '/test/other');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (4,  1, 'foo', 'Directory', '/test/other/foo');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (5,  1, 'things', 'Directory', '/test/other/things');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (6,  1, 'cats', 'Directory', '/test/other/foo/cats');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (7,  1, 'dogs', 'Directory', '/test/other/foo/dogs');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (8,  1, 'big', 'Directory', '/test/other/foo/dogs/big');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (9,  1, 'small', 'Directory', '/test/other/foo/dogs/small');
Insert into EAS_PATH_RESOURCE (NODE_ID, STORE_ID, PATH_NAME, PATH_TYPE, RELATIVE_PATH) values (10, 1, 'pics', 'Directory', '/test/other/foo/dogs/small/pics');

/* sample directory resources */
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 1  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 2  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 3  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 4  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 5  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 6  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 7  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 8  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 9  );
Insert into EAS_DIRECTORY_RESOURCE (NODE_ID) values ( 10 );

/* sample nodes */
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 1,  0, 'test', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 2,  1, 'more', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 3,  1, 'other', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 4,  3, 'foo', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 5,  3, 'things', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 6,  4, 'cats', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 7,  4, 'dogs', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 8,  7, 'big', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 9,  7, 'small', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );
Insert into EAS_NODE (NODE_ID, PARENT_NODE_ID, NODE_NAME, CREATION_DATE, UPDATED_DATE) values ( 10, 9, 'pics', to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS'),  to_date('20-JUN-14 18:55:09','DD-MON-RR HH24:MI:SS') );

/* every node is parent/child of itself at depth-0 */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  1,  0,  1  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (2,  2,  0,  2  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  3,  0,  3  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (4,  4,  0,  4  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (5,  5,  0,  5  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (6,  6,  0,  6  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (7,  7,  0,  7  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (8,  8,  0,  8  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (9,  9,  0,  9  );
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (10, 10, 0,  10 );
/* all depth-1 entries */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  2,  1,  11 ); /* A->B */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  3,  1,  12 ); /* A->C */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  4,  1,  13 ); /* C->D */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  5,  1,  14 ); /* C->E */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (4,  6,  1,  15 ); /* D->F */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (4,  7,  1,  16 ); /* D->G */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (7,  8,  1,  17 ); /* G->H */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (7,  9,  1,  18 ); /* G->I */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (9,  10, 1,  19 ); /* I->J */
/* all depth-2 entries */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  5,  2,  20 ); /* A->E */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  4,  2,  21 ); /* A->D */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  6,  2,  22 ); /* C->F */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  7,  2,  23 ); /* C->G */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (4,  8,  2,  24 ); /* D->H */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (4,  9,  2,  25 ); /* D->I */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (7,  10, 2,  26 ); /* G->J */
/* all depth-3 entries */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  6,  3,  27 ); /* A->F */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  7,  3,  28 ); /* A->G */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  8,  3,  29 ); /* C->H */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  9,  3,  30 ); /* C->I */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (4,  10, 3,  31 ); /* D->J */
/* all depth-4 entries */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  8,  4,  32 ); /* A->H */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  9,  4,  33 ); /* A->I */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (3,  10, 4,  34 ); /* C->J */
/* all depth-5 entries */
Insert into EAS_CLOSURE (PARENT_NODE_ID, CHILD_NODE_ID, DEPTH, LINK_ID) values (1,  10, 5,  35 ); /* A->J */