CREATE TABLE [dbo].[smsserver_out](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[type] [char](1) NOT NULL DEFAULT ('O'),
	[recipient] [nvarchar](32) NOT NULL,
	[text] [nvarchar](1000) NOT NULL,
	[wap_url] [nvarchar](100) NULL,
	[wap_expiry_date] [datetime] NULL,
	[wap_signal] [char](1) NULL,
	[create_date] [datetime] NOT NULL DEFAULT (getdate()),
	[originator] [nvarchar](16) NOT NULL DEFAULT (''),
	[encoding] [char](1) NOT NULL DEFAULT ('7'),
	[status_report] [int] NOT NULL DEFAULT (0),
	[flash_sms] [int] NOT NULL DEFAULT (0),
	[src_port] [int] NOT NULL DEFAULT (-1),
	[dst_port] [int] NOT NULL DEFAULT (-1),
	[sent_date] [datetime] NULL DEFAULT (NULL),
	[ref_no] [nvarchar](64) NULL DEFAULT (NULL),
	[priority] [int] NOT NULL DEFAULT (0),
	[status] [char](1) NOT NULL DEFAULT ('U'),
	[errors] [int] NOT NULL DEFAULT (0),
	[gateway_id] [nvarchar](64) NOT NULL DEFAULT ('*'),
	[plNr] [int] NULL DEFAULT (NULL),
        CONSTRAINT [PK_smssvr_out] PRIMARY KEY ([id])
)
GO

-- Migration fuer bestehende Datenbanken:
-- ALTER TABLE smsserver_out ADD plNr int NULL DEFAULT(NULL);
-- GO



