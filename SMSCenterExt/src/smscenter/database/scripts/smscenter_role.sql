CREATE ROLE smscenter_role AUTHORIZATION db_datareader
GO

GRANT INSERT, UPDATE, DELETE ON smscenter_groups TO smscenter_role
GO

GRANT INSERT, UPDATE, DELETE ON smscenter_phones TO smscenter_role
GO

GRANT INSERT, UPDATE, DELETE ON smscenter_positions TO smscenter_role
GO

GRANT INSERT, UPDATE, DELETE ON smscenter_results TO smscenter_role
GO

GRANT INSERT, UPDATE, DELETE ON smscenter_schedules TO smscenter_role
GO

GRANT INSERT, UPDATE, DELETE ON smsserver_calls TO smscenter_role
GO

GRANT INSERT, UPDATE, DELETE ON smsserver_in TO smscenter_role
GO

GRANT INSERT, UPDATE, DELETE ON smsserver_out TO smscenter_role
GO
