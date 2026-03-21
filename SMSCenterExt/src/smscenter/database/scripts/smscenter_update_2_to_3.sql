EXEC sp_rename 'dbo.smscenter_phones.start_nr ', 'plNr', 'COLUMN'
GO

EXEC sp_rename N'dbo.smscenter_phones.IX_smsserver_phone_start_nr', N'IX_smsserver_phone_plNr', N'INDEX'
GO

