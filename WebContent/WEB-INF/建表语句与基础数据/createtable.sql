create table dbo.LOG_XSYJ_ERR (UUID varchar(32) not null, FYDM varchar(6) not null, LX varchar(1) null, ERRMSG varchar(255) null, LASTUPDATE datetime not null) ; 
alter table dbo.LOG_XSYJ_ERR add constraint PK_LOG_XSYJ_ERR primary key nonclustered (UUID) ;


create table dbo.LOG_XSYJ (FYDM varchar(6) not null, DT varchar(14) not null, LAS int not null, JAS int not null) ;
alter table dbo.LOG_XSYJ add constraint PK_LOG_XSYJ primary key nonclustered (FYDM, DT) ;

create table dbo.TS_DB (ID1 varchar(6) not null, ID2 varchar(6) not null, MS varchar(50) null, CONN_DRIVER varchar(100) null, CONN_URL varchar(100) null, CONN_USER varchar(20) null, CONN_PASSWORD varchar(20) null, SFJY varchar(1) null) ;
alter table dbo.TS_DB add constraint PK_TS_DB primary key nonclustered (ID1, ID2) ;

create table dbo.TS_FYMC (FYDM varchar(6) not null, DM smallint default 0 null, FJM varchar(3) null, FDM smallint default 0 null, FYMC varchar(50) null, FYJC varchar(6) null, FYJB varchar(1) null, TJBM varchar(3) null, SFJY varchar(1) null, FYDC varchar(20) null, JCYMC varchar(50) null, JCYJC varchar(10) null, FYJC09 varchar(20) null) ;
alter table dbo.TS_FYMC add constraint pk_ts_fymc primary key nonclustered (FYDM) ;

create table dbo.TS_BZDM (KIND varchar(6) not null, BT varchar(160) null, CODE varchar(15) not null, MC varchar(160) null, FDM varchar(15) null, SFJY varchar(1) null, PXH int null, CODENOW varchar(15) null, VER varchar(3) null, KIN09 varchar(5) null, V2014CODE varchar(15) null, SFKZ varchar(1) null) ;
alter table dbo.TS_BZDM add constraint TS_BZDM_x primary key nonclustered (KIND, CODE) ;


create table dbo.T_AJLX_DMZH (DM varchar(2) null, MC varchar(40) null, AJLX varchar(2) null, AJLXDM int not null, XTAJLX varchar(4) null, SFJY varchar(1) null) ;
alter table dbo.T_AJLX_DMZH add constraint PK_T_AJLX_DMZH primary key nonclustered (AJLXDM) ;
