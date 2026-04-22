@echo off
cd /d "%~dp0"
echo VeriCertChain GitHub Publisher
echo.
echo First create a PUBLIC empty GitHub repository named VeriCertChain.
echo Do not add README, gitignore, or license on GitHub.
echo.
set /p REPO_URL=Paste GitHub repository URL here: 
git remote remove origin 2>nul
git remote add origin %REPO_URL%
git push -u origin main
pause
