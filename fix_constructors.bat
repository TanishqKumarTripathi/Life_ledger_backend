@echo off
echo Fixing remaining Lombok constructor conflicts...

REM Fix GoalController
powershell -Command "(Get-Content 'src\main\java\com\Life_ledger\controller\GoalController.java') -replace '@Autowired\s*\r?\n', '' -replace 'public GoalController\([^}]+}\s*', '' | Set-Content 'src\main\java\com\Life_ledger\controller\GoalController.java'"

REM Fix InsightController  
powershell -Command "(Get-Content 'src\main\java\com\Life_ledger\controller\InsightController.java') -replace '@Autowired\s*\r?\n', '' -replace 'public InsightController\([^}]+}\s*', '' | Set-Content 'src\main\java\com\Life_ledger\controller\InsightController.java'"

REM Fix AccountServiceImpl
powershell -Command "(Get-Content 'src\main\java\com\Life_ledger\service\AccountServiceImpl.java') -replace '@Autowired\s*\r?\n', '' -replace 'public AccountServiceImpl\([^}]+}\s*', '' | Set-Content 'src\main\java\com\Life_ledger\service\AccountServiceImpl.java'"

echo Done!