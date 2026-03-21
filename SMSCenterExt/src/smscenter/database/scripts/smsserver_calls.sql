CREATE TABLE [dbo].[smsserver_calls](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[call_date] [datetime] NOT NULL,
	[gateway_id] [nvarchar](64) NOT NULL,
	[caller_id] [nvarchar](64) NOT NULL,
        CONSTRAINT [PK_smssvr_calls] PRIMARY KEY ([id])
)
GO


