CREATE TABLE [dbo].[smscenter_groups] (
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[grID] [int] NOT NULL,
        [state_sent] [smallint] NOT NULL DEFAULT 0,
        [state_enabled] [smallint] NOT NULL DEFAULT 0,
        [state_manual] [smallint] NOT NULL DEFAULT 0,
        CONSTRAINT [PK_smscenter_groups] PRIMARY KEY ([id])
)
GO

CREATE UNIQUE INDEX [IX_smscenter_groups_grid] ON [dbo].[smscenter_groups] ([grID])
GO

ALTER TABLE [dbo].[smscenter_groups] 
        ADD CONSTRAINT [FK_smscenter_groups_GrRec] FOREIGN KEY([grID]) REFERENCES [dbo].[GrRec] ([grID]) ON DELETE CASCADE
GO

INSERT INTO [dbo].[smscenter_groups] (grID, state_enabled)
    SELECT grID, grPublished FROM [dbo].[GrRec]
GO



