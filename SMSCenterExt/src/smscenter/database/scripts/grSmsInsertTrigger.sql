CREATE TRIGGER [dbo].[grSmsInsertTrigger] ON [dbo].[GrRec] FOR INSERT AS
    INSERT INTO [dbo].[smscenter_groups] (grID) SELECT grID FROM inserted

GO
