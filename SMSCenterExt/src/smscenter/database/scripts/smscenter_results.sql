CREATE TABLE [dbo].[smscenter_results](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[mtNr] [int] NOT NULL,
	[ts] [datetime] NOT NULL DEFAULT (getdate()),
        CONSTRAINT [PK_smscenter_results] PRIMARY KEY ([id])
)
GO

CREATE UNIQUE INDEX [IX_smscenter_results_mtnr] ON [dbo].[smscenter_results] ([mtNr])
GO

ALTER TABLE [dbo].[smscenter_results]  
    ADD CONSTRAINT [FK_smscenter_results_mtrec] FOREIGN KEY([mtNr]) REFERENCES [dbo].[MtRec] ([mtNr]) ON DELETE CASCADE
GO



