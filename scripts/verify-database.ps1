param(
    [string]$DatabaseHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = $env:DB_PASSWORD
)

$ErrorActionPreference = "Stop"
if (-not (Get-Command mysql.exe -ErrorAction SilentlyContinue)) { throw "mysql.exe was not found." }
if (-not $Password) {
    $securePassword = Read-Host "Enter the MySQL password for user $User" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

$query = @"
SELECT 'schema_version_count', COUNT(*) FROM sys_schema_version;
SELECT 'latest_schema_version', version FROM sys_schema_version ORDER BY installed_at DESC, version DESC LIMIT 1;
SELECT 'schema_integrity_issues', COUNT(*) FROM sys_schema_issue;
SELECT 'orphan_user_roles', COUNT(*) FROM sys_user_role ur LEFT JOIN sys_user u ON u.id=ur.user_id LEFT JOIN sys_role r ON r.role_id=ur.role_id WHERE u.id IS NULL OR r.role_id IS NULL;
SELECT 'duplicate_user_roles', COUNT(*) FROM (SELECT user_id, role_id FROM sys_user_role GROUP BY user_id, role_id HAVING COUNT(*) > 1) d;
SELECT 'users_without_role', COUNT(*) FROM sys_user u LEFT JOIN sys_user_role ur ON ur.user_id=u.id WHERE ur.id IS NULL;
SELECT 'negative_product_stock', COUNT(*) FROM sys_product WHERE stock < 0;
SELECT 'duplicate_pending_payments', COUNT(*) FROM (SELECT order_no FROM sys_payment WHERE status=0 GROUP BY order_no HAVING COUNT(*) > 1) d;
SELECT 'orphan_order_items', COUNT(*) FROM sys_order_item oi LEFT JOIN sys_order o ON o.order_no=oi.order_no WHERE o.order_no IS NULL;
"@

$env:MYSQL_PWD = $Password
try {
    & mysql.exe --host=$DatabaseHost --port=$Port --user=$User --database=fruit_mall `
        --table --default-character-set=utf8mb4 --execute=$query
    if ($LASTEXITCODE -ne 0) { throw "Database verification failed." }
    Write-Host "All issue counters should be 0 and the latest version should be v7." -ForegroundColor Cyan
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
