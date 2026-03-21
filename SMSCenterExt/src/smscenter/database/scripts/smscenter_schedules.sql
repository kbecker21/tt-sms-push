CREATE TABLE [dbo].[smscenter_schedules](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[mtNr] [int] NOT NULL,
        [rescheduled] [int] NOT NULL DEFAULT 0,
        [reminder] [int] NOT NULL DEFAULT 0,
	[ts] [datetime] NOT NULL DEFAULT (getdate()),
        CONSTRAINT [PK_smscenter_schedules] PRIMARY KEY ([id])
)
GO

CREATE UNIQUE INDEX [IX_smscenter_schedules_mtid] ON [dbo].[smscenter_schedules] ([mtNr])
GO

ALTER TABLE [dbo].[smscenter_schedules]  
    ADD CONSTRAINT [FK_smscenter_schedules_mtrec] FOREIGN KEY([mtNr]) REFERENCES [dbo].[MtRec] ([mtNr]) ON DELETE CASCADE
GO

