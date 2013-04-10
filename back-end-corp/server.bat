@echo off
net use H: \\Bside-PC\open-assets "123456" /User:"notifyer"
net use I: \\Bside-PC\open-templates "123456" /User:"notifyer"

set curDir=%~dp0

rd "%curDir%"assets
rd "%curDir%"templates

rd "%curDir%"assets
rd "%curDir%"templates

mklink /d "%curDir%"assets H:
mklink /d "%curDir%"templates I: 

pushd "%curDir%/../../bin"
startup.bat
