' ============================================================
'  Create a desktop shortcut for Java10143 Shop
'  Double click this file (or right click -> Open with cscript)
' ============================================================
Option Explicit

Dim fso, shell, here, target, name, link, jing, dong
Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

here = fso.GetParentFolderName(WScript.ScriptFullName)
target = here & "\start-java10143.bat"

If Not fso.FileExists(target) Then
  MsgBox "start-java10143.bat not found next to this script." & vbCrLf & target, 16, "Java10143"
  WScript.Quit
End If

' shortcut name = Chinese chars built with ChrW() so the file stays pure ASCII
jing = ChrW(20140)   ' jing
dong = ChrW(19996)   ' dong
name = jing & dong & "10143" & " Shop"

link = shell.SpecialFolders("Desktop") & "\" & name & ".lnk"

Dim sc
Set sc = shell.CreateShortcut(link)
sc.TargetPath = target
sc.WorkingDirectory = here
sc.Description = "Start Java10143 backend and open the web shop"
sc.WindowStyle = 7
sc.IconLocation = "shell32.dll,13"
sc.Save

MsgBox "Desktop shortcut created:" & vbCrLf & link, 64, "Java10143"
