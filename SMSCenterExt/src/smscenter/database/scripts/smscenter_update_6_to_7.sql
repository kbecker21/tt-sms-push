DROP TRIGGER [dbo].[stSmsUpdateTrigger]

GO

CREATE TRIGGER [dbo].[stSmsUpdateTrigger] ON [dbo].[StRec] FOR UPDATE AS
	DECLARE @updateCursor CURSOR 
	DECLARE @stID INT
	DECLARE @oldTmID SMALLINT, @newTmID SMALLINT
	DECLARE @oldStPos SMALLINT, @newStPos SMALLINT
	
	SET @updateCursor = CURSOR FOR
		SELECT inserted.stID, inserted.tmID, deleted.tmID, inserted.stPos, deleted.stPos
		  FROM inserted INNER JOIN deleted ON inserted.stID = deleted.stID
                                INNER JOIN smscenter_groups ON inserted.grID = smscenter_groups.grID
		  
	OPEN @updateCursor
			  
	FETCH NEXT FROM @updateCursor 
		INTO @stID, @oldTmID, @newTmID, @oldStPos, @newStPos
		     
	WHILE (@@FETCH_STATUS = 0)
	BEGIN	
            --- Team has changed, update schedules
            IF ( ISNULL(@oldTmID, 0) <> ISNULL(@newTmID, 0) )
            BEGIN
                DELETE FROM smscenter_schedules WHERE mtNr IN (SELECT mtNr FROM MtRec WHERE (stA = @stID OR stX = @stID))
                INSERT INTO smscenter_schedules (mtNr, rescheduled) SELECT mtNr, 0 FROM MtRec WHERE (stA = @stID OR stX = @stID)
            END

            --- Update positions
            IF (@oldStPos <> @newStPos)
            BEGIN
                DELETE FROM smscenter_positions WHERE stID = @stID
                INSERT INTO smscenter_positions (stID) VALUES (@stID)
            END

            FETCH NEXT FROM @updateCursor 
                    INTO @stID, @oldStPos, @newStPos
	END
	
	CLOSE @updateCursor
	DEALLOCATE @updateCursor


GO
