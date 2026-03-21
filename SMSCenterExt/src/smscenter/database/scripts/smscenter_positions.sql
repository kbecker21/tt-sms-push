CREATE TABLE [dbo].[smscenter_positions](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[stID] [int] NOT NULL,
	[ts] [datetime] NOT NULL DEFAULT (getdate()),
        CONSTRAINT [PK_smscenter_positions] PRIMARY KEY ([id])
)
GO

CREATE UNIQUE INDEX [IX_smscenter_positions_stid] ON [dbo].[smscenter_positions] ([stID])
GO

ALTER TABLE [dbo].[smscenter_positions]  
    ADD CONSTRAINT [FK_smscenter_positions_strec] FOREIGN KEY([stID]) REFERENCES [dbo].[StRec] ([stID]) ON DELETE CASCADE
GO



