CREATE TABLE [dbo].[smsserver_in](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[process] [int] NOT NULL,
	[originator] [nvarchar](32) NOT NULL,
	[type] [char](1) NOT NULL,
	[encoding] [char](1) NOT NULL,
	[message_date] [datetime] NOT NULL,
	[receive_date] [datetime] NOT NULL,
	[text] [nvarchar](1000) NOT NULL,
	[original_ref_no] [nvarchar](64) NULL,
	[original_receive_date] [datetime] NULL,
	[gateway_id] [nvarchar](64) NOT NULL,
	[status] [char](1) NOT NULL DEFAULT ('U'),
        CONSTRAINT [PK_smssvr_in] PRIMARY KEY ([id])
)
GO


