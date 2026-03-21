#define Version '25.06.04'

[Setup]
AppName=SMSCenter
AppVersion={#Version}
VersionInfoVersion={#Version}
AppPublisher=Christoph Theis
DefaultDirName={autopf}\TTM\SMSCenter
DefaultGroupName=TTM
OutputDir=.\Output
OutputBaseFilename=install
ArchitecturesInstallIn64BitMode=x64
MinVersion= 0,6.1

; Sign installer
; SignTool=MS /d $qTTM Installer$q $f


[Types]
Name: "client"; Description: "SMS Center for TTM"

[Components]
Name: "Client"; Description: "SMS Center for TTM"; Types: client; Flags: fixed


[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:";

[Files]
Source: ".\dist\SMSCenter.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\dist\*.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\dist\lib\*.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: ".\lib\rxtxSerial.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\dist\SMSCenter.ico"; DestDir: "{app}"; Flags: ignoreversion 
Source: "..\TTM Manuals\src\SMSCenter\SMSCenter.pdf"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\SMSServer.template.conf"; DestDir: "{commonappdata}\TTM"; DestName: "SMSServer.conf"; Flags: onlyifdoesntexist uninsneveruninstall
Source: ".\3rdparty.html"; DestDir: "{app}"; Flags: ignoreversion

[Registry]
; HKLM\Software\JavaSoft\Prefs should be created by JRE installer, but that does not always happen
Root: HKLM; Subkey: "Software\JavaSoft\Prefs"; Flags: noerror

[Icons]
Name: "{group}\SMSCenter"; Filename: "{app}\SMSCenter.jar"; WorkingDir: "{app}";
Name: "{userdesktop}\SMSCenter"; Filename: "{app}\SMSCenter.jar"; WorkingDir: "{app}"; Tasks: desktopicon; IconFilename: "{app}\SMSCenter.ico";

[Code]
(* looks for JDK or JRE version in Registry *)
function getJREVersion(): String;
var
	jreVersion: String;
begin
	jreVersion := '';
  if IsWin64 then begin
	  RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JRE', 'CurrentVersion', jreVersion);
  end
  else begin
  	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JRE', 'CurrentVersion', jreVersion);
  end;
	Result := jreVersion;
end;

(* looks for JDK version, in Registry *)
function getJDKVersion(): String;
var
	jdkVersion: String;
begin
	jdkVersion := '';
  if IsWin64 then begin
	  RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', jdkVersion);
  end
  else begin
  	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', jdkVersion);
  end;
	Result := jdkVersion;
end;

(* Called on setup startup *)
function InitializeSetup(): Boolean;
var
	javaVersion: String;
begin
	javaVersion := GetJDKVersion();
  if Length(javaVersion) = 0 then begin
    javaVersion := GetJREVersion()
  end;

	if javaVersion >= '11' then begin
		(* MsgBox('Found java version' + javaVersion, mbInformation, MB_OK); *)
		Result := true;
	end
	else begin
		MsgBox('Setup is unable to find a Java Development Kit or Java Runtime 11, or higher, installed.' + #13 +
			     'You must have installed at least JDK or JRE, 11 or higher to continue setup.' + #13 +
			     'Please install one from https://AdoptOpenJDK.com and then run this setup again.', mbInformation, MB_OK);
		Result := true;
	end;
end;





