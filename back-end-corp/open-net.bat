echo off
set curDir=%~dp0
net share open-assets="%curDir%assets" /cache:no
net share open-templates="%curDir%templates" /cache:no