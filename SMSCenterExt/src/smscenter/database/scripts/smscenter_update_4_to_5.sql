ALTER TABLE [dbo].[smscenter_groups] ADD
        [state_sent] [smallint] NOT NULL DEFAULT 0,
        [state_enabled] [smallint] NOT NULL DEFAULT 0,
        [state_manual] [smallint] NOT NULL DEFAULT 0
GO

CREATE TRIGGER [dbo].[grSmsInsertTrigger] ON [dbo].[GrRec] FOR INSERT AS
    INSERT INTO [dbo].[smscenter_groups] (grID) SELECT grID FROM inserted

GO

CREATE TRIGGER [dbo].[grSmsUpdateTrigger] ON [dbo].[GrRec] FOR UPDATE AS
    UPDATE [dbo].[smscenter_groups] 
       SET state_enabled = inserted.grPublish
      FROM inserted 
            INNER JOIN deleted ON inserted.grID = deleted.grID 
    WHERE [dbo].[smscenter_groups].state_manual = 0 
            AND inserted.grPublish <> deleted.grPublish

GO

INSERT INTO [dbo].[smscenter_groups] (grID, state_enabled)
    SELECT GrRec.grID, GrRec.grPublish 
      FROM GrRec LEFT OUTER JOIN smscenter_groups ON GrRec.grID = smscenter_groups.grID
     WHERE smscenter_groups.grID IS NULL
GO

