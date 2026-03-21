CREATE TRIGGER [dbo].[mtSmsUpdateTrigger] ON [dbo].[MtRec] FOR UPDATE AS
	DECLARE @updateCursor CURSOR 
	DECLARE @mtNr INT
	DECLARE @oldResA SMALLINT, @oldResX SMALLINT, @newResA SMALLINT, @newResX SMALLINT
	DECLARE @oldTable SMALLINT, @oldDateTime DATETIME, @newTable SMALLINT, @newDateTime DATETIME
	DECLARE @oldStA INT, @oldStX INT, @newStA INT, @newStX INT
	DECLARE @oldTmA INT, @oldTmX INT, @newTmA INT, @newTmX INT
	SET @updateCursor = CURSOR FOR
		SELECT inserted.mtNr, 
                       inserted.mtResA, inserted.mtResX, deleted.mtResA, deleted.mtResX,
                       inserted.stA, inserted.stX, deleted.stA, deleted.stX,
                       newsta.tmID, newstx.tmID, oldsta.tmID, oldstx.tmID,
                       inserted.mtTable, inserted.mtDateTime, deleted.mtTable, deleted.mtDateTime
		  FROM inserted INNER JOIN deleted ON inserted.mtID = deleted.mtID 
                                INNER JOIN smscenter_groups ON inserted.grID = smscenter_groups.grID 
                                LEFT OUTER JOIN StList newsta ON inserted.stA = newsta.stID
                                LEFT OUTER JOIN StList newstx ON inserted.stX = newstx.stID
                                LEFT OUTER JOIN StList oldsta ON deleted.stA = oldsta.stID
                                LEFT OUTER JOIN StList oldstx ON deleted.stX = oldstx.stID

	OPEN @updateCursor
	FETCH NEXT FROM @updateCursor 
		INTO @mtNr, 
		     @newResA, @newResX, @oldResA, @oldResX,
		     @newStA, @newStX, @oldStA, @oldStX,
                     @newTmA, @newTmX, @oldTmA, @oldTmX,
		     @newTable, @newDateTime, @oldTable, @oldDateTime
	WHILE (@@FETCH_STATUS = 0)
	BEGIN	
		IF (@oldResA <> @newResA OR @oldResX <> @newResX) 
		BEGIN
			IF ( (@oldResA > 0 OR @oldResX > 0) AND (@newResA = 0 AND @newResX = 0) )
			BEGIN
				DELETE FROM smscenter_schedules WHERE mtNr = @mtNr
				INSERT INTO smscenter_schedules (mtNr) VALUES (@mtNr)
			END
			DELETE FROM smscenter_results WHERE mtNr = @mtNr
			INSERT INTO smscenter_results (mtNr) VALUES (@mtNr)
		END
		IF ( ISNULL(@oldStA, 0) <> ISNULL(@newStA, 0) OR 
                     ISNULL(@oldStX, 0) <> ISNULL(@newStX, 0) OR
                     ISNULL(@oldTmA, 0) <> ISNULL(@newTmA, 0) OR 
                     ISNULL(@oldTmX, 0) <> ISNULL(@newTmX, 0) ) 
                BEGIN
                    DELETE FROM smscenter_schedules WHERE mtNr = @mtNr
                    INSERT INTO smscenter_schedules (mtNr, rescheduled) VALUES (@mtNr, 0)
                END
                ELSE
                BEGIN
                    IF ( ISNULL(@oldTable, 0) <> ISNULL(@newTable, 0) OR 
                         ISNULL(@oldDateTime, CAST(0 AS DATETIME)) <> ISNULL(@newDateTime, CAST(0 AS DATETIME)) )
                    BEGIN
                        DELETE FROM smscenter_schedules WHERE mtNr = @mtNr
                        INSERT INTO smscenter_schedules (mtNr, rescheduled) VALUES (@mtNr, 1)
                    END                
                END
		FETCH NEXT FROM @updateCursor 
			INTO @mtNr, 
                             @newResA, @newResX, @oldResA, @oldResX,
                             @newStA, @newStX, @oldStA, @oldStX,
                             @newTmA, @newTmX, @oldTmA, @oldTmX,
                             @newTable, @newDateTime, @oldTable, @oldDateTime
	END
	CLOSE @updateCursor
	DEALLOCATE @updateCursor
 
GO

