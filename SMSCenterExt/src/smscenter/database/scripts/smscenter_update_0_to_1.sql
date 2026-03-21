CREATE TABLE [dbo].[smscenter_settings] (
        [version] [int] NOT NULL,
        [welcome] [nvarchar](256) NULL DEFAULT NULL,
        [sponsor] [nvarchar](160) NULL DEFAULT NULL
)
GO

INSERT INTO [dbo].[smscenter_settings] (version) VALUES (0)
GO
