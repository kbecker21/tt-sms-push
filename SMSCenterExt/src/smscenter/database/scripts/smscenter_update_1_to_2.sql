ALTER TABLE [dbo].[smscenter_settings] ADD
	[ts] [datetime] NOT NULL DEFAULT (getdate())
GO

CREATE TRIGGER [dbo].[smsSettingsTrigger] ON [dbo].[smscenter_settings] FOR UPDATE AS
        UPDATE [dbo].[smscenter_settings] SET ts = getdate();
GO

