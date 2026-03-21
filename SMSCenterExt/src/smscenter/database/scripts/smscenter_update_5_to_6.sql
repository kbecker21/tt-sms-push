DROP TRIGGER [dbo].[grSmsUpdateTrigger]

GO

CREATE TRIGGER [dbo].[grSmsUpdateTrigger] ON [dbo].[GrRec] FOR UPDATE AS
    UPDATE [dbo].[smscenter_groups] 
       SET state_enabled = inserted.grPublished
      FROM inserted 
            INNER JOIN deleted ON inserted.grID = deleted.grID 
    WHERE [dbo].[smscenter_groups].state_manual = 0 
            AND inserted.grPublished <> deleted.grPublished

GO

