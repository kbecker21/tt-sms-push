CREATE TABLE [dbo].[smscenter_phones](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[plNr] [int] NOT NULL,
	[phone] [nvarchar](1024) NOT NULL,
	[status] [char](1) NOT NULL DEFAULT ('U'),
	[created] [datetime] NOT NULL DEFAULT (getdate()),
        CONSTRAINT [PK_smsserver_numbers] PRIMARY KEY ([id])
)
GO

CREATE UNIQUE INDEX [IX_smsserver_phone_start_nr] ON [dbo].[smscenter_phones] ([plNr])
GO

ALTER TABLE [dbo].[smscenter_phones]  
    ADD CONSTRAINT [FK_smscenter_phones_start_nr] FOREIGN KEY([plNr]) REFERENCES [dbo].[PlRec] ([plNr]) ON DELETE CASCADE
GO



