-- 由 psql 执行；生产环境数据库和权限通常应由运维/IaC 管理。
SELECT 'CREATE DATABASE rehealth_hardware WITH ENCODING = ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'rehealth_hardware')\gexec
\connect rehealth_hardware
SET TIME ZONE 'UTC';
CREATE EXTENSION IF NOT EXISTS timescaledb;
